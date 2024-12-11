package com.vk.education_bot.handler;

import com.vk.api.sdk.client.GsonHolder;
import com.vk.api.sdk.events.Events;
import com.vk.api.sdk.events.callback.CallbackApi;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.api.sdk.objects.callback.messages.CallbackMessage;
import com.vk.education_bot.client.VkClient;
import com.vk.education_bot.client.YandexGptClient;
import com.vk.education_bot.configuration.BotProperties;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.entity.Question;
import com.vk.education_bot.logic.QuestionClassifier;
import com.vk.education_bot.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CommonCallbackHandler extends CallbackApi {

    private final static String UNKNOWN_QUESTION = "Извините, я не понимаю ваш вопрос. Вот список доступных вопросов:\n";
    private final static String EMPTY = "";

    private final VkClient vkClient;
    private final BotProperties botProperties;
    private final QuestionService questionService;
    private final QuestionClassifier questionClassifier;

    public CommonCallbackHandler(BotProperties botProperties, VkClient vkClient, QuestionService questionService, QuestionClassifier questionClassifier) {
        super(botProperties.confirmationCode());
        this.vkClient = vkClient;
        this.botProperties = botProperties;
        this.questionService = questionService;
        this.questionClassifier = questionClassifier;
    }

    @Override
    public void messageNew(Integer groupId, MessageNew message) {
        long userId = Optional.ofNullable(message
                        .getObject()
                        .getMessage()
                        .getFromId())
                .orElseThrow(() -> new RuntimeException("UserId not presented"));

        String answer = questionClassifier.classifyQuestion(message
                .getObject().getMessage().getText())
                .map(Question::getAnswer)
                .orElseGet(() -> UNKNOWN_QUESTION + String.join("\n", questionService.getAllQuestionsText()));
        vkClient.sendMessage(userId, answer);
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
}
