package com.vk.education_bot.handler;

import com.vk.api.sdk.client.GsonHolder;
import com.vk.api.sdk.events.Events;
import com.vk.api.sdk.events.callback.CallbackApi;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.api.sdk.objects.callback.MessageReply;
import com.vk.api.sdk.objects.callback.messages.CallbackMessage;
import com.vk.education_bot.client.KeyboardFactory;
import com.vk.education_bot.client.VkClient;
import com.vk.education_bot.client.YandexGptClient;
import com.vk.education_bot.configuration.BotProperties;
import com.vk.education_bot.entity.Project;
import com.vk.education_bot.entity.Question;
import com.vk.education_bot.entity.Section;
import com.vk.education_bot.entity.UnknownQuestion;
import com.vk.education_bot.logic.QuestionClassifier;
import com.vk.education_bot.service.QuestionService;
import com.vk.education_bot.service.SectionService;
import com.vk.education_bot.service.UnknownQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CommonCallbackHandler extends CallbackApi {

    private final static String EMPTY = "";

    private final Map<Long, UserContext> userContexts = new ConcurrentHashMap<>();
    private final Map<Long, String> userQuestions = new ConcurrentHashMap<>();

    private final BotProperties botProperties;
    private final QuestionService questionService;
    private final SectionService sectionService;
    private final UnknownQuestionService unknownQuestionService;

    private final VkClient vkClient;
    private final YandexGptClient yandexGptClient;
    private final QuestionClassifier questionClassifier;

    public CommonCallbackHandler(BotProperties botProperties, VkClient vkClient, QuestionService questionService, SectionService sectionService, QuestionClassifier questionClassifier, UnknownQuestionService unknownQuestionService, YandexGptClient yandexGptClient) {
        super(botProperties.confirmationCode());
        this.vkClient = vkClient;
        this.botProperties = botProperties;
        this.questionService = questionService;
        this.sectionService = sectionService;
        this.unknownQuestionService = unknownQuestionService;
        this.questionClassifier = questionClassifier;
        this.yandexGptClient = yandexGptClient;
    }

    @Override
    public void messageNew(Integer groupId, MessageNew message) {
        long userId = Optional.ofNullable(message
                        .getObject()
                        .getMessage()
                        .getFromId())
                .orElseThrow(() -> new RuntimeException("UserId not presented"));
        // Сообщение от пользователя
        String userInput = Optional.ofNullable(message.getObject().getMessage().getText()).orElse("");
        System.out.println("ПОЛУЧЕНО: " + userInput);
        UserContext context = userContexts.computeIfAbsent(userId, UserContext::new);

        if ("Назад".equalsIgnoreCase(userInput) || "Начать".equalsIgnoreCase(userInput)) {
            handleBackAction(context);
            return;
        }

        if (userInput.startsWith("/admin") && context.getState() != UserState.ADMIN) {
            if (userId != botProperties.supportUserId()) {
                vkClient.sendMessage(userId, "У Вас нет прав администратора.");
            } else {
                context.setState(UserState.ADMIN);
                vkClient.sendMessageWithKeyboard(userId, """
                                Вы вошли в режим администратора. Используйте следующие команды для работы:\s
                                /admin show - показать список неизвестных вопросов\s
                                /admin delete 1, 3, 10, 56 - для удаления вопроса\s
                                /admin answer 1, 3, 10, 56, 101 "Текст вопроса" "Текст ответа" - для ответа на вопросы\s
                                """,
                        KeyboardFactory.createBackButtonKeyboard());
                KeyboardFactory.clearKeyboard();
            }
            return;
        }

        switch (context.getState()) {
            case MAIN_MENU -> handleMainMenu(userInput, context);
            case SECTION_SELECTION -> handleSectionSelection(userInput, context);
            case PROJECT_SELECTION -> handleProjectSelection(userInput, context);
            case ASKING_PROJECT_QUESTION -> handleProjectQuestion(userInput, context);
            case ASKING_GENERAL_QUESTION -> handleGeneralQuestion(userInput, context);
            case ADMIN -> handleAdminCommand(userInput, context);
            case QUES_YES_NO -> handleQuesYesNo(userInput, context);
            case PROJ_YES_NO -> handleProjYesNo(userInput, context);
            default -> vkClient.sendMessageWithKeyboard(userId, "Выбери опцию", KeyboardFactory.createMainMenuKeyboard());
        }
    }

    private void handleMainMenu(String userInput, UserContext context) {
        long userId = context.getUserId();
        if ("Задать общий вопрос".equalsIgnoreCase(userInput)) {
            vkClient.sendMessageWithKeyboard(userId, "Введи свой вопрос", KeyboardFactory.createBackButtonKeyboard());
            context.setState(UserState.ASKING_GENERAL_QUESTION);
        } else if ("Вопрос по проекту".equalsIgnoreCase(userInput)) {
            List<Section> sections = sectionService.getAllSections();
            vkClient.sendMessageWithKeyboard(userId, "Выбери раздел", KeyboardFactory.createSectionsKeyboard(sections));
            context.setState(UserState.SECTION_SELECTION);
        } else {
            vkClient.sendMessageWithKeyboard(userId, "Выбери опцию", KeyboardFactory.createMainMenuKeyboard());
        }
    }

    private void handleSectionSelection(String userInput, UserContext context) {
        long userId = context.getUserId();
        try {
            Section section = sectionService.getAllSections().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(userInput))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Section not found"));

            context.setCurrentSection(section);

            List<Project> projects = section.getProjects();
            StringBuilder projectListMessage = new StringBuilder("Выберите проект, введя его номер:\n");
            for (Project project : projects) {
                projectListMessage.append(project.getId()).append(". ").append(project.getName()).append("\n");
            }

            vkClient.sendMessageWithKeyboard(userId, projectListMessage.toString(), KeyboardFactory.createBackButtonKeyboard());
            context.setState(UserState.PROJECT_SELECTION);

        } catch (Exception e) {
            vkClient.sendMessageWithKeyboard(userId, "Раздел не найден. Попробуйте снова.", KeyboardFactory.createSectionsKeyboard(sectionService.getAllSections()));
        }
    }

    private void handleProjectSelection(String userInput, UserContext context) {
        long userId = context.getUserId();
        try {
            int projectId = Integer.parseInt(userInput);

            Project project = context.getCurrentSection().getProjects().stream()
                    .filter(p -> p.getId() == projectId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            context.setCurrentProject(project);

            vkClient.sendMessageWithKeyboard(userId, "Введи свой вопрос по проекту", KeyboardFactory.createBackButtonKeyboard());
            context.setState(UserState.ASKING_PROJECT_QUESTION);

        } catch (NumberFormatException e) {
            vkClient.sendMessageWithKeyboard(userId, "Некорректный ввод. Введите номер нужного проекта.", KeyboardFactory.createBackButtonKeyboard());
        } catch (RuntimeException e) {
            vkClient.sendMessageWithKeyboard(userId, "Такой проект не найден. Выберите проект из списка.", KeyboardFactory.createBackButtonKeyboard());
        }
    }

    private void handleProjectQuestion(String userInput, UserContext context) {
        long userId = context.getUserId();
        Project project = context.getCurrentProject();

        String prompt = String.format(
                """
                        Ответь на вопрос, используя информацию о проекте:
                        Название: %s
                        Направление: %s
                        Для чего подойдет: %s
                        Минимальное количество участников: %d
                        Максимальное количество участников: %d
                        Цель проекта: %s
                        Описание: %s
                        Материалы: %s
                        Продающее описание: %s
                        Алгоритм: %s
                        Необходимые компетенции: %s
                        Рекомендации: %s
                        Сложность: %s
                        Формат обучения: %s
                        Трудозатратность: %s
                        Условия получения сертификата: %s
                        Ожидаемый результат: %s
                        Критерии оценки: %s
                        Преимущества: %s
                        Вопрос по проекту: %s""",
                project.getName(),
                project.getDirection(),
                project.getType(),
                project.getMinMembers() != null ? project.getMinMembers() : 0,
                project.getMaxMembers() != null ? project.getMaxMembers() : 0,
                project.getGoal() != null ? project.getGoal() : "Не указано",
                project.getDescription() != null ? project.getDescription() : "Не указано",
                project.getMaterials() != null ? project.getMaterials() : "Не указаны",
                project.getSellingDescription() != null ? project.getSellingDescription() : "Не указано",
                project.getAlgorithm() != null ? project.getAlgorithm() : "Не указан",
                project.getCompetencies() != null ? project.getCompetencies() : "Не указаны",
                project.getRecommendations() != null ? project.getRecommendations() : "Не указаны",
                project.getComplexity() != null ? project.getComplexity() : "Не указана",
                project.getStudyFormat() != null ? project.getStudyFormat() : "Не указан",
                project.getIntense() != null ? project.getIntense() : "Не указана",
                project.getCertificateConditions() != null ? project.getCertificateConditions() : "Не указаны",
                project.getExpectedResult() != null ? project.getExpectedResult() : "Не указан",
                project.getGradingCriteria() != null ? project.getGradingCriteria() : "Не указаны",
                project.getBenefits() != null ? project.getBenefits() : "Не указаны",
                userInput
        );

        String response = yandexGptClient.generateAnswer(prompt, YandexGptClient.GptTaskDescription.ANSWER_ABOUT_PROJECT);
        vkClient.sendMessage(userId, response);
        vkClient.sendMessageWithKeyboard(userId, "Ты получил ответ на свой вопрос?", KeyboardFactory.createYesNoKeyboard());
        userQuestions.put(userId, project.getName() + ": " + userInput);
        context.setState(UserState.PROJ_YES_NO);
    }

    private void handleQuesYesNo(String userInput, UserContext context) {
        long userId = context.getUserId();
        if ("Да".equalsIgnoreCase(userInput)) {
            context.setState(UserState.ASKING_GENERAL_QUESTION);
            userQuestions.remove(userId);
            vkClient.sendMessageWithKeyboard(userId, "Если хочешь узнать что-то еще - спрашивай", KeyboardFactory.createBackButtonKeyboard());
        } else if ("Нет".equalsIgnoreCase(userInput)) {
            handleUnknownQuestion(userQuestions.get(userId), context);
            context.setState(UserState.ASKING_GENERAL_QUESTION);
            userQuestions.remove(userId);
            vkClient.sendMessageWithKeyboard(userId, "Если хочешь узнать что-то еще - спрашивай", KeyboardFactory.createBackButtonKeyboard());
        } else if ("Назад".equalsIgnoreCase(userInput)) {
            handleBackAction(context);
        } else {
            vkClient.sendMessageWithKeyboard(userId, "Выберите да или нет", KeyboardFactory.createYesNoKeyboard());
        }
    }

    private void handleProjYesNo(String userInput, UserContext context) {
        long userId = context.getUserId();
        if ("Да".equalsIgnoreCase(userInput)) {
            context.setState(UserState.ASKING_PROJECT_QUESTION);
            userQuestions.remove(userId);
            vkClient.sendMessageWithKeyboard(userId, "Если хочешь узнать что-то еще по проекту - спрашивай", KeyboardFactory.createBackButtonKeyboard());
        } else if ("Нет".equalsIgnoreCase(userInput)) {
            handleUnknownQuestion(userQuestions.get(userId), context);
            context.setState(UserState.ASKING_PROJECT_QUESTION);
            userQuestions.remove(userId);
            vkClient.sendMessageWithKeyboard(userId, "Если хочешь узнать что-то еще по проекту - спрашивай", KeyboardFactory.createBackButtonKeyboard());
        } else if ("Назад".equalsIgnoreCase(userInput)) {
            handleBackAction(context);
        } else {
            vkClient.sendMessageWithKeyboard(userId, "Выберите да или нет", KeyboardFactory.createMainMenuKeyboard());
        }
    }

    private void handleBackAction(UserContext context) {
        long userId = context.getUserId();
        context.reset();
        userQuestions.remove(userId);
        vkClient.sendMessageWithKeyboard(userId, "Выбери опцию", KeyboardFactory.createMainMenuKeyboard());
    }

    private void handleGeneralQuestion(String userInput, UserContext context) {
        long userId = context.getUserId();
        String response = questionClassifier.classifyQuestion(userInput)
                .map(Question::getAnswer)
                .orElseGet(() -> yandexGptClient.generateAnswer(userInput, YandexGptClient.GptTaskDescription.COMMON_ANSWER));

        vkClient.sendMessage(userId, response);
        vkClient.sendMessageWithKeyboard(userId, "Ты получил ответ на свой вопрос?", KeyboardFactory.createYesNoKeyboard());
        userQuestions.put(userId,  userInput);
        context.setState(UserState.QUES_YES_NO);
    }

    private void handleUnknownQuestion(String userInput, UserContext context) {
        long userId = context.getUserId();
        // Обработка неизвестного вопроса
        String response = "Извини, я не понял твой вопрос. Я сообщу в поддержку о твоей проблеме.";
        vkClient.sendMessage(userId, response);

        unknownQuestionService.saveUnknownQuestion(userId, userInput);

        // Уведомить поддержку
        vkClient.sendMessage(botProperties.supportUserId(),
                "Новый неизвестный вопрос: " + "\"" + userInput + "\". Нужно добавить ответ.");
    }

    private void handleAdminCommand(String userInput, UserContext context) {
        long userId = context.getUserId();
        if (userId != botProperties.supportUserId()) {
            vkClient.sendMessage(userId, "У тебя нет прав администратора.");
            return;
        }

        String commandBody = userInput.substring(6).trim();

        if (commandBody.startsWith("show")) {
            // Показать список неизвестных вопросов
            handleAdminShow(context);
        } else if (commandBody.startsWith("answer")) {
            // Формат: /admin answer 1, 3, 10, 56, 101 "Текст вопроса" "Текст ответа"
            Pattern pattern = Pattern.compile(
                    "^answer\\s+([\\d,\\s]+)\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"$"
            );
            Matcher matcher = pattern.matcher(commandBody);
            if (matcher.find()) {
                String idsString = matcher.group(1).trim();
                String newQuestionText = matcher.group(2);
                String answerText = matcher.group(3);
                // Парсим IDs
                List<Long> ids = Arrays.stream(idsString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();
                handleAdminAnswer(ids, context, newQuestionText, answerText);
            } else {
                vkClient.sendMessage(userId, "Неверный формат команды.");
            }
        } else if (commandBody.startsWith("delete")) {
            // Формат: /admin delete 1, 3, 10, 56
            Pattern pattern = Pattern.compile(
                    "^delete\\s+([\\d,\\s]+)$"
            );
            Matcher matcher = pattern.matcher(commandBody);
            if (matcher.find()) {
                String idsString = matcher.group(1).trim();
                // Парсим IDs
                List<Long> ids = Arrays.stream(idsString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();
                handleAdminDelete(ids, context);
            } else {
                vkClient.sendMessage(userId, "Неверный формат команды.");
            }
        } else {
            vkClient.sendMessage(userId, "Неизвестная команда администратора.");
        }
    }

    private void handleAdminShow(UserContext context) {
        long userId = context.getUserId();
        List<UnknownQuestion> unknownQuestions = unknownQuestionService.getAllUnknownQuestions();
        if (unknownQuestions.isEmpty()) {
            vkClient.sendMessage(userId, "Нет неизвестных вопросов.");
        } else {
            StringBuilder sb = new StringBuilder("Список неизвестных вопросов:\n");
            for (UnknownQuestion uq : unknownQuestions) {
                sb.append("ID: ").append(uq.getId())
                        .append(", Вопрос: ").append(uq.getQuestionText())
                        .append("\n");
            }
            vkClient.sendMessage(userId, sb.toString());
        }
    }


    private void handleAdminDelete(List<Long> ids, UserContext context) {
        long userId = context.getUserId();
        if (ids.isEmpty()) {
            vkClient.sendMessage(userId, "Не указаны корректные ID вопросов.");
            return;
        }
        // Получаем список неизвестных вопросов по указанным ID
        List<UnknownQuestion> unknownQuestions = unknownQuestionService.findAllByIds(ids);
        if (unknownQuestions.isEmpty()) {
            vkClient.sendMessage(userId, "Не найдены неизвестные вопросы по указанным ID.");
            return;
        }
        // Для каждого удаляемого вопроса отправляем уведомление пользователю
        for (UnknownQuestion uq : unknownQuestions) {
            vkClient.sendMessage(uq.getUserId(), "Твой вопрос снят с рассмотрения");
        }
        // Удаляем выбранные вопросы из списка
        unknownQuestionService.deleteAll(unknownQuestions);

        vkClient.sendMessage(userId, "Вопрос удален");
    }

    private void handleAdminAnswer(List<Long> ids, UserContext context, String newQuestionText, String answerText) {
        long userId = context.getUserId();
        if (ids.isEmpty()) {
            vkClient.sendMessage(userId, "Не указаны корректные ID вопросов.");
            return;
        }
        // Проверяем, не существует ли уже вопрос с таким текстом
        if (questionService.checkQuestionExists(newQuestionText)) {
            vkClient.sendMessage(userId, "На вопрос с данной формулировкой уже есть ответ в списке.");
            return;
        }
        // Получаем список неизвестных вопросов по указанным ID
        List<UnknownQuestion> unknownQuestions = unknownQuestionService.findAllByIds(ids);
        if (unknownQuestions.isEmpty()) {
            vkClient.sendMessage(userId, "Не найдены неизвестные вопросы по указанным ID.");
            return;
        }
        // Сохраняем новый вопрос с ответом
        questionService.saveQuestion(newQuestionText, answerText);

        // Для каждого неизвестного вопроса отправляем уведомление пользователю и удаляем его
        for (UnknownQuestion uq : unknownQuestions) {
            vkClient.sendMessage(uq.getUserId(), "Ответ на твой вопрос добавлен: " + answerText);
        }
        // Удаляем выбранные вопросы из списка
        unknownQuestionService.deleteAll(unknownQuestions);
        vkClient.sendMessage(userId, "Вопрос и ответ успешно добавлены, пользователи уведомлены.");
    }


    @Override
    public String parse(String json) {
        // Разбор входящего JSON и обработка событий
        CallbackMessage callbackMessage = new GsonHolder().getGson().fromJson(json, CallbackMessage.class);

        if (callbackMessage.getType() == null) {
            return "";
        }

        if (!Events.CONFIRMATION.equals(callbackMessage.getType())) {
            return super.parse(callbackMessage);
        }

        if (botProperties.groupId() == callbackMessage.getGroupId()) {
            return botProperties.confirmationCode();
        }
        return EMPTY;
    }

    @Override
    public void messageReply(Integer groupId, MessageReply message) {
        // ignore?
    }

}
