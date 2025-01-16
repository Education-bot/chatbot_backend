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
import com.vk.education_bot.service.AdminService;
import com.vk.education_bot.service.ProjectService;
import com.vk.education_bot.service.QuestionService;
import com.vk.education_bot.service.SectionService;
import com.vk.education_bot.service.UnknownQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class CommonCallbackHandler extends CallbackApi {

    private final static String EMPTY = "";

    private static final Pattern adminPattern = Pattern.compile("^\\s*([\\d,\\s]+)\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"$");
    private static final Pattern curseWordsPattern = Pattern.compile("(?iu)(?:\\b[а-яё]?)" +
        "(?:у|[нз]а|(?:хитро|не)?вз?[ыьъ]|с[ьъ]|(?:и|ра)[зс]ъ?|(?:о[тб]|п[оа]д)[ьъ]?|[а-яё]*?)?" +
        "(?:[её]б(?!о[рй]|рач)|п[уа][цтс]|и[пб][ае][тцд][ьъ]|ху(?:[яйиеёюл]+)|бля(?:[дтц])?|заебал\\b)|" +
        "(?:[нз]а|по|про|на|вы)?м[ао]нд[ауеиы]*|елд[ауые]|ля[тд]ь|(?:п[иеё]зд|ид[аое]?р|охую|бля[дтц]).*?" +
        "|(?:хуй\\w*|залуп.*|\\bбля.*|\\bпизд.*|\\bчлен.*|\\bсуч+к.*|еба.*|\\bёб.*)");
    private static final Pattern numberPattern = Pattern.compile("^\\s*([\\d,\\s]+)$");

    private final Map<Long, UserContext> userContexts = new ConcurrentHashMap<>();
    private final Map<Long, String> userQuestions = new ConcurrentHashMap<>();

    private final BotProperties botProperties;
    private final AdminService adminService;
    private final ProjectService projectService;
    private final QuestionService questionService;
    private final SectionService sectionService;
    private final UnknownQuestionService unknownQuestionService;

    private final VkClient vkClient;
    private final YandexGptClient yandexGptClient;
    private final QuestionClassifier questionClassifier;

    public CommonCallbackHandler(BotProperties botProperties, AdminService adminService, ProjectService projectService, VkClient vkClient, QuestionService questionService, SectionService sectionService, QuestionClassifier questionClassifier, UnknownQuestionService unknownQuestionService, YandexGptClient yandexGptClient) {
        super(vkClient.getConfirmationCode());
        this.adminService = adminService;
        this.projectService = projectService;
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
        long userId = ofNullable(message
            .getObject()
            .getMessage()
            .getFromId())
            .orElseThrow(() -> new RuntimeException("UserId not presented"));
        // Сообщение от пользователя
        var userInput = ofNullable(message.getObject()
            .getMessage()
            .getText())
            .orElse(EMPTY);
        log.info("ПОЛУЧЕНО: {}", userInput);

        var matcher = curseWordsPattern.matcher(userInput);

        if (matcher.find()) {
            vkClient.sendMessage(userId, "Так говорить некрасиво(");
            return;
        }

        var context = userContexts.computeIfAbsent(userId, UserContext::new);

        if ("Назад".equalsIgnoreCase(userInput) || "Начать".equalsIgnoreCase(userInput)) {
            handleBackAction(context);
            return;
        }

        if (userInput.startsWith("/admin") && context.getState() != UserState.ADMIN) {
            if (!adminService.checkAdmin(userId)) {
                vkClient.sendMessage(userId, "У тебя нет прав администратора.");
            } else {
                context.setState(UserState.ADMIN);
                vkClient.sendMessageWithKeyboard(userId, "Ты вошел в режим администратора.",
                    KeyboardFactory.createAdminKeyboard());
                KeyboardFactory.clearKeyboard();
            }
            return;
        }

        switch (context.getState()) {
            case MAIN_MENU -> handleMainMenu(userInput, context);
            case SECTION_SELECTION -> handleSectionSelection(userInput, context);
            case PROJECT_SELECTION -> handleProjectSelection(userInput, context);
            case ASKING_PROJECT_QUESTION -> handleAskingQuestion(userInput, context);
            case ASKING_GENERAL_QUESTION -> handleUserQuestion(userInput, context);
            case QUES_YES_NO -> handleQuesYesNo(userInput, context);
            case PROJ_YES_NO -> handleProjYesNo(userInput, context);
            case ADMIN -> handleAdminCommand(userInput, context);
            case ADMIN_ANSWER -> handleAdminAnswer(userInput, context);
            case ADMIN_DELETE_Q -> handleAdminDelete(userInput, context);
            case ADMIN_ADD_ADMIN -> handleAddAdmin(userInput, context);
            case ADMIN_DELETE_ADMIN -> handleDeleteAdmin(userInput, context);
            case ADMIN_SECTION_SELECTION -> handleAdminSectionSelection(userInput, context);
            case ADMIN_NEW_PROJECT -> handleAdminNewProject(userInput, context);
            case ADMIN_SHOW_PROJECTS -> handleAdminShowProject(userInput, context);
            case ADMIN_EDIT_PROJECT -> handleAdminEditProject(userInput, context);
            case ADMIN_ACCEPT_EDITED_PROJECT -> handleAdminAcceptEditedProject(userInput, context);
            default ->
                vkClient.sendMessageWithKeyboard(userId, "Выбери опцию", KeyboardFactory.createMainMenuKeyboard());
        }
    }

    private void handleMainMenu(String userInput, UserContext context) {
        long userId = context.getUserId();
        if ("Задать общий вопрос".equalsIgnoreCase(userInput)) {
            vkClient.sendMessageWithKeyboard(userId, "Введи свой вопрос", KeyboardFactory.createBackButtonKeyboard());
            context.setState(UserState.ASKING_GENERAL_QUESTION);
        } else if ("Вопрос по проекту".equalsIgnoreCase(userInput)) {
            var sections = sectionService.getAllSections();
            vkClient.sendMessageWithKeyboard(userId, "Выбери раздел", KeyboardFactory.createSectionsKeyboard(sections));
            context.setState(UserState.SECTION_SELECTION);
        } else {
            vkClient.sendMessageWithKeyboard(userId, "Выбери опцию", KeyboardFactory.createMainMenuKeyboard());
        }
    }

    private void handleSectionSelection(String userInput, UserContext context) {
        long userId = context.getUserId();
        try {
            var section = sectionService.getAllSections().stream()
                .filter(s -> s.getName().equalsIgnoreCase(userInput))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

            context.setCurrentSection(section);

            var projects = section.getProjects();
            var projectsMessageBuilder = new StringBuilder("Выберите проект, введя его номер:\n");
            for (var project : projects) {
                projectsMessageBuilder.append(project.getId()).append(". ").append(project.getName()).append("\n");
            }

            vkClient.sendMessageWithKeyboard(userId, projectsMessageBuilder.toString(), KeyboardFactory.createBackButtonKeyboard());
            context.setState(UserState.PROJECT_SELECTION);

        } catch (Exception e) {
            vkClient.sendMessageWithKeyboard(userId, "Раздел не найден. Попробуйте снова.", KeyboardFactory.createSectionsKeyboard(sectionService.getAllSections()));
        }
    }

    private void handleProjectSelection(String userInput, UserContext context) {
        long userId = context.getUserId();
        try {
            int projectId = Integer.parseInt(userInput);

            var project = context.getCurrentSection().getProjects().stream()
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

    private void handleAskingQuestion(String userInput, UserContext context) {
        long userId = context.getUserId();
        var project = context.getCurrentProject();

        var prompt = String.format(
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

        var response = yandexGptClient.generateAnswer(prompt, YandexGptClient.GptTaskDescription.ANSWER_ABOUT_PROJECT);
        vkClient.sendMessage(userId, response);
        vkClient.sendMessageWithKeyboard(userId, "Ты получил ответ на свой вопрос?", KeyboardFactory.createYesNoKeyboard());
        userQuestions.put(userId, project.getName() + ": " + userInput);
        context.setState(UserState.PROJ_YES_NO);
    }

    private void handleBackAction(UserContext context) {
        long userId = context.getUserId();
        context.reset();
        userQuestions.remove(userId);
        vkClient.sendMessageWithKeyboard(userId, "Выбери опцию", KeyboardFactory.createMainMenuKeyboard());
    }

    private void handleUserQuestion(String userInput, UserContext context) {
        long userId = context.getUserId();
        var response = questionClassifier.classifyQuestion(userInput)
            .map(Question::getAnswer)
            .orElseGet(() -> yandexGptClient.generateAnswer(userInput, YandexGptClient.GptTaskDescription.COMMON_ANSWER));

        vkClient.sendMessage(userId, response);
        vkClient.sendMessageWithKeyboard(userId, "Ты получил ответ на свой вопрос?", KeyboardFactory.createYesNoKeyboard());
        userQuestions.put(userId, userInput);
        context.setState(UserState.QUES_YES_NO);
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

    private void handleUnknownQuestion(String userInput, UserContext context) {
        long userId = context.getUserId();
        // Обработка неизвестного вопроса
        var response = "Извини, я не понял твой вопрос. Я сообщу в поддержку о твоей проблеме.";
        vkClient.sendMessage(userId, response);

        unknownQuestionService.saveUnknownQuestion(userId, userInput);

        // Уведомить поддержку
        for (var id : adminService.getAllAdminIds()) {
            try {
                vkClient.sendMessage(id, "Новый неизвестный вопрос: \"%s\". Нужно добавить ответ.".formatted(userInput));
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private void handleAdminCommand(String commandBody, UserContext context) {
        long userId = context.getUserId();
        if (!adminService.checkAdmin(userId)) {
            vkClient.sendMessage(userId, "У тебя нет прав администратора.");
            return;
        }

        if (commandBody.startsWith("Список вопросов")) {
            // Показать список неизвестных вопросов
            handleAdminShowAdminList(context);
        } else if (commandBody.startsWith("Ответить на вопрос")) {
            context.setState(UserState.ADMIN_ANSWER);
            vkClient.sendMessageWithKeyboard(userId, "Напиши на какой вопрос (или вопросы) ты хочешь ответить, " +
                    "формулировку вопроса и ответ в формате: \n1, 3, 10, 56, 101 \"Текст вопроса\" \"Текст ответа\"",
                KeyboardFactory.createAdminBackButtonKeyboard());
        } else if (commandBody.startsWith("Удалить вопрос")) {
            context.setState(UserState.ADMIN_DELETE_Q);
            vkClient.sendMessageWithKeyboard(userId, "Перечисли номера вопросов, которые ты хочешь удалить, " +
                    "в формате: \n1, 3, 10, 56, 101",
                KeyboardFactory.createAdminBackButtonKeyboard());
        } else if (commandBody.startsWith("Добавить админа")) {
            context.setState(UserState.ADMIN_ADD_ADMIN);
            vkClient.sendMessageWithKeyboard(userId, "Введи VK ID пользователя, которого ты хочешь сделать " +
                "администратором бота", KeyboardFactory.createAdminBackButtonKeyboard());
        } else if (commandBody.startsWith("Список админов")) {
            var sb = new StringBuilder();
            var list = adminService.getAllAdminIds();
            for (var l : list) {
                sb.append("VK ID: ").append(l).append(". Профиль: https://vk.com/id").append(l.toString()).append("\n");
            }
            vkClient.sendMessage(userId, sb.toString());
        } else if (commandBody.startsWith("Удалить админа")) {
            context.setState(UserState.ADMIN_DELETE_ADMIN);
            vkClient.sendMessageWithKeyboard(userId, "Введи VK ID пользователя, у которого ты хочешь забрать админку",
                KeyboardFactory.createAdminBackButtonKeyboard());
        } else if (commandBody.startsWith("Добавить проект")) {
            context.setState(UserState.ADMIN_SECTION_SELECTION);
            var sections = sectionService.getAllSections();
            vkClient.sendMessageWithKeyboard(userId, "Выбери раздел, к которому будет относиться проект",
                KeyboardFactory.createAdminSectionsKeyboard(sections));
        } else if (commandBody.startsWith("Изменить проект")) {
            context.setState(UserState.ADMIN_EDIT_PROJECT);
            vkClient.sendMessageWithKeyboard(context.getUserId(), "Выберите проект из списка и введите его номер\n" + projectService.listAllProjectsAsString(), KeyboardFactory.createAdminBackButtonKeyboard());
        } else if (commandBody.startsWith("Описания проектов")) {
            context.setState(UserState.ADMIN_SHOW_PROJECTS);
            vkClient.sendMessageWithKeyboard(context.getUserId(), "Выберите проект из списка и введите его номер\n" + projectService.listAllProjectsAsString(), KeyboardFactory.createAdminBackButtonKeyboard());
        } else {
            vkClient.sendMessage(userId, "Неизвестная команда администратора.");
        }
    }

    private void handleAdminSectionSelection(String userInput, UserContext context) {
        if (userInput.startsWith("В меню")) {
            handleAdminBackAction(context);
            return;
        }
        long userId = context.getUserId();
        try {
            var section = sectionService.getAllSections().stream()
                .filter(s -> s.getName().equalsIgnoreCase(userInput))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

            context.setCurrentSection(section);

            var message = """
                Введи описание проекта в формате (скопируй и заполни шаблон):
                # Название
                Текст
                # Направление
                Текст
                # Тип (для чего подходит)
                Текст
                # Минимальное число участников
                1
                # Максимальное число участников
                3
                # Цель
                Текст
                # Описание
                Текст
                # Примеры и материалы
                Текст
                # Продающее описание
                Текст
                # Алгоритм
                Текст
                # Компетенции
                Текст
                # Рекомендации
                Текст
                # Сложность
                Текст
                # Формат обучения
                Текст
                # Трудоемкость (в часах)
                Текст
                # Условия получения сертификата
                Текст
                # Ожидаемый результат
                Текст
                # Критерии оценивания
                Текст
                # Награды
                Текст
                """;


            vkClient.sendMessageWithKeyboard(userId, message, KeyboardFactory.createAdminBackButtonKeyboard());
            context.setState(UserState.ADMIN_NEW_PROJECT);

        } catch (Exception e) {
            vkClient.sendMessageWithKeyboard(userId, "Раздел не найден. Попробуйте снова.", KeyboardFactory.createAdminSectionsKeyboard(sectionService.getAllSections()));
        }
    }

    private void handleAdminNewProject(String userInput, UserContext context) {
        if (userInput.startsWith("В меню")) {
            handleAdminBackAction(context);
            return;
        }
        projectService.parseAndAddProject(userInput);
        vkClient.sendMessage(context.getUserId(), "Проект добавлен");
        context.setCurrentSection(null);
        handleAdminBackAction(context);
    }

    private void handleAdminShowProject(String userInput, UserContext context) {
        if (userInput.startsWith("В меню")) {
            handleAdminBackAction(context);
            return;
        }
        long userId = context.getUserId();
        try {
            int projectId = Integer.parseInt(userInput);

            var project = projectService.getProjects().stream()
                .filter(p -> p.getId() == projectId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Project not found"));
            vkClient.sendMessageWithKeyboard(userId, "Вот текущее описание проекта:\n" + projectService.buildProjectDescription(project), KeyboardFactory.createAdminBackButtonKeyboard());
            handleAdminBackAction(context);

        } catch (NumberFormatException e) {
            vkClient.sendMessageWithKeyboard(userId, "Некорректный ввод. Введите номер нужного проекта.", KeyboardFactory.createAdminBackButtonKeyboard());
        } catch (RuntimeException e) {
            vkClient.sendMessageWithKeyboard(userId, "Такой проект не найден. Выберите проект из списка.", KeyboardFactory.createAdminBackButtonKeyboard());
        }
    }

    private void handleAdminEditProject(String userInput, UserContext context) {
        if (userInput.startsWith("В меню")) {
            handleAdminBackAction(context);
            return;
        }
        long userId = context.getUserId();
        try {
            int projectId = Integer.parseInt(userInput);

            var project = projectService.getProjects().stream()
                .filter(p -> p.getId() == projectId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Project not found"));
            context.setAdminCurrentProject(project);
            vkClient.sendMessageWithKeyboard(userId, "Вот текущее описание проекта. Скопируй его и отредактируй нужные пункты\n" + projectService.buildProjectDescription(project), KeyboardFactory.createAdminBackButtonKeyboard());
            context.setState(UserState.ADMIN_ACCEPT_EDITED_PROJECT);

        } catch (NumberFormatException e) {
            vkClient.sendMessageWithKeyboard(userId, "Некорректный ввод. Введите номер нужного проекта.", KeyboardFactory.createAdminBackButtonKeyboard());
        } catch (RuntimeException e) {
            vkClient.sendMessageWithKeyboard(userId, "Такой проект не найден. Выберите проект из списка.", KeyboardFactory.createAdminBackButtonKeyboard());
        }
    }

    private void handleAdminAcceptEditedProject(String userInput, UserContext context) {
        projectService.parseAndUpdateProject(context.getAdminCurrentProject().getId(), userInput);
        vkClient.sendMessage(context.getUserId(), "Описание проекта обновлено");
        context.setAdminCurrentProject(null);
        handleAdminBackAction(context);
    }

    private void handleAddAdmin(String userInput, UserContext context) {
        if (userInput.startsWith("В меню")) {
            handleAdminBackAction(context);
            return;
        }
        try {
            var id = Long.parseLong(userInput);
            if (adminService.checkAdmin(id)) {
                vkClient.sendMessage(context.getUserId(), "Пользователь уже есть в списке администраторов");
                return;
            }
            adminService.addAdmin(id);
            vkClient.sendMessage(context.getUserId(), "Администратор " + userInput + " добавлен");
        } catch (Exception e) {
            vkClient.sendMessage(context.getUserId(), "Неверный формат VK ID.");
        }
    }

    private void handleDeleteAdmin(String userInput, UserContext context) {
        if (userInput.startsWith("В меню")) {
            handleAdminBackAction(context);
            return;
        }
        try {
            var adminId = Long.parseLong(userInput);
            if (!adminService.checkAdmin(adminId)) {
                vkClient.sendMessage(context.getUserId(), "Администратор с таким ID не найден");
                return;
            }
            adminService.deleteAdmin(adminId);
            vkClient.sendMessage(context.getUserId(), "Администратор " + userInput + " удален");
        } catch (Exception e) {
            vkClient.sendMessage(context.getUserId(), "Неверный формат VK ID.");
        }
    }

    private void handleAdminShowAdminList(UserContext context) {
        long userId = context.getUserId();
        var unknownQuestions = unknownQuestionService.getAllUnknownQuestions();
        if (unknownQuestions.isEmpty()) {
            vkClient.sendMessage(userId, "Нет неизвестных вопросов.");
        } else {
            var sb = new StringBuilder("Список неизвестных вопросов:\n");
            for (var unknownQuestion : unknownQuestions) {
                sb.append("ID: ").append(unknownQuestion.getId())
                    .append(", Вопрос: ").append(unknownQuestion.getQuestionText())
                    .append("\n");
            }
            vkClient.sendMessage(userId, sb.toString());
        }
    }

    private void handleAdminDelete(String commandBody, UserContext context) {
        long userId = context.getUserId();
        var matcher = numberPattern.matcher(commandBody);
        if (matcher.find()) {
            var idsString = matcher.group(1).trim();
            // Парсим IDs
            var ids = Arrays.stream(idsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
            if (ids.isEmpty()) {
                vkClient.sendMessage(userId, "Не указаны корректные ID вопросов.");
                return;
            }
            // Получаем список неизвестных вопросов по указанным ID
            var unknownQuestions = unknownQuestionService.findAllByIds(ids);
            if (unknownQuestions.isEmpty()) {
                vkClient.sendMessage(userId, "Не найдены неизвестные вопросы по указанным ID.");
                return;
            }
            // Для каждого удаляемого вопроса отправляем уведомление пользователю
            for (var unknownQuestion : unknownQuestions) {
                vkClient.sendMessage(unknownQuestion.getUserId(), "Твой вопрос снят с рассмотрения");
            }
            // Удаляем выбранные вопросы из списка
            unknownQuestionService.deleteAll(unknownQuestions);

            vkClient.sendMessage(userId, "Вопрос удален");
            handleAdminBackAction(context);
        } else if (commandBody.startsWith("В меню")) {
            handleAdminBackAction(context);
        } else {
            vkClient.sendMessage(userId, "Неверный формат команды.");
        }
    }

    private void handleAdminAnswer(String commandBody, UserContext context) {
        long userId = context.getUserId();
        Matcher matcher = adminPattern.matcher(commandBody);
        if (matcher.find()) {
            var idsString = matcher.group(1).trim();
            var newQuestionText = matcher.group(2);
            var answerText = matcher.group(3);
            // Парсим IDs
            var ids = Arrays.stream(idsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
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
            var unknownQuestions = unknownQuestionService.findAllByIds(ids);
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
            handleAdminBackAction(context);
        } else if (commandBody.startsWith("В меню")) {
            handleAdminBackAction(context);
        } else {
            vkClient.sendMessage(userId, "Неверный формат команды.");
        }
    }

    private void handleAdminBackAction(UserContext context) {
        long userId = context.getUserId();
        context.setState(UserState.ADMIN);
        vkClient.sendMessageWithKeyboard(userId, "Ты в режиме администратора", KeyboardFactory.createAdminKeyboard());
    }

    @Override
    public String parse(String json) {
        // Разбор входящего JSON и обработка событий
        var callbackMessage = new GsonHolder()
            .getGson()
            .fromJson(json, CallbackMessage.class);

        if (callbackMessage.getType() == null) {
            return EMPTY;
        }

        if (!Events.CONFIRMATION.equals(callbackMessage.getType())) {
            return super.parse(callbackMessage);
        }

        if (botProperties.groupId() == callbackMessage.getGroupId()) {
            return vkClient.getConfirmationCode();
        }
        return EMPTY;
    }

    @Override
    public void messageReply(Integer groupId, MessageReply message) {
        // ignore?
    }

}
