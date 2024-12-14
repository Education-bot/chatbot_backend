package com.vk.education_bot.handler;

import com.vk.api.sdk.client.GsonHolder;
import com.vk.api.sdk.events.Events;
import com.vk.api.sdk.events.callback.CallbackApi;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.api.sdk.objects.callback.MessageReply;
import com.vk.api.sdk.objects.callback.messages.CallbackMessage;
import com.vk.education_bot.client.VkClient;
import com.vk.education_bot.client.YandexGptClient;
import com.vk.education_bot.configuration.BotProperties;

import com.vk.education_bot.entity.UnknownQuestion;
import com.vk.education_bot.service.UnknownQuestionService;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.entity.Question;
import com.vk.education_bot.logic.QuestionClassifier;
import com.vk.education_bot.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CommonCallbackHandler extends CallbackApi {

    private final static String UNKNOWN_QUESTION = "Извините, я не понимаю ваш вопрос. Вот список доступных вопросов:\n";
    private final static String EMPTY = "";

    private final VkClient vkClient;
    private final BotProperties botProperties;
    private final QuestionService questionService;

    private final UnknownQuestionService unknownQuestionService;
    private final QuestionClassifier questionClassifier;

    public CommonCallbackHandler(BotProperties botProperties, VkClient vkClient, QuestionService questionService, QuestionClassifier questionClassifier, UnknownQuestionService unknownQuestionService) {
        super(botProperties.confirmationCode());
        this.vkClient = vkClient;
        this.botProperties = botProperties;
        this.questionService = questionService;
        this.unknownQuestionService = unknownQuestionService;
        this.questionClassifier = questionClassifier;
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

        if (userInput.startsWith("/admin")) {
            handleAdminCommand(userId, userInput);
            return;
        }

        String response = questionClassifier.classifyQuestion(message
                .getObject().getMessage().getText())
                .map(Question::getAnswer)
                .orElseGet(() -> UNKNOWN_QUESTION + String.join("\n", questionService.getAllQuestionsText()));

        if (response == null) {
            // Обработка неизвестного вопроса
            response = "Извините, я не понимаю ваш вопрос. Ваш вопрос отправлен в поддержку на доработку.";
            vkClient.sendMessage(userId, response);

            unknownQuestionService.saveUnknownQuestion(userId, userInput);

            // Уведомить поддержку
            vkClient.sendMessage(botProperties.supportUserId(),
                    "Новый неизвестный вопрос: " + "\"" + userInput + "\". Нужно добавить ответ.");

        } else {
            vkClient.sendMessage(userId, response);
        }
    }

    private void handleAdminCommand(long userId, String userInput) {
        if (userId != botProperties.supportUserId()) {
            vkClient.sendMessage(userId, "У Вас нет прав администратора.");
            return;
        }

        String commandBody = userInput.substring(6).trim();

        if (commandBody.startsWith("show")) {
            // Показать список неизвестных вопросов
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
                    vkClient.sendMessage(uq.getUserId(), "Ответ на ваш вопрос добавлен: " + answerText);
                }

                // Удаляем выбранные вопросы из списка
                unknownQuestionService.deleteAll(unknownQuestions);

                vkClient.sendMessage(userId, "Вопрос и ответ успешно добавлены, пользователи уведомлены.");
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
                    vkClient.sendMessage(uq.getUserId(), "Ваш вопрос снят с рассмотрения");
                }
                // Удаляем выбранные вопросы из списка
                unknownQuestionService.deleteAll(unknownQuestions);

                vkClient.sendMessage(userId, "Вопрос удален");
            } else {
                vkClient.sendMessage(userId, "Неверный формат команды.");
            }
        } else {
            vkClient.sendMessage(userId, "Неизвестная команда администратора.");
        }
    }

    @Override
    public String parse(String json) {
        // Разбор входящего JSON и обработка событий
        CallbackMessage callbackMessage = new GsonHolder().getGson().fromJson(json, CallbackMessage.class);

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
