package com.vk.education_bot.client;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionCallback;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionCallbackType;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import com.vk.api.sdk.objects.messages.KeyboardButtonColor;
import com.vk.api.sdk.objects.messages.TemplateActionTypeNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class VkClient {

    private final VkApiClient vkApiClient;
    private final GroupActor groupActor;

    // example
    public void sendMessage(long userId, String message) {
        // todo create common solve for exception handling
        try {
            vkApiClient
                    .messages()
                    .sendDeprecated(groupActor)
                    .userId(userId)
                    .message(message)
                    .randomId(ThreadLocalRandom.current().nextInt())
                    .execute();
        } catch (ApiException | ClientException e) {
            log.error("Got error when send message to user {}", userId);
            throw new RuntimeException(e);
        }
    }

    public void sendFollowUpQuestion(long userId) {
        try {
            vkApiClient
                    .messages()
                    .sendDeprecated(groupActor)
                    .userId(userId)
                    .message("Вы получили ответ на ваш вопрос?")
                    .randomId(ThreadLocalRandom.current().nextInt())
                    .keyboard(buildKeyboard())
                    .execute();
        } catch (ApiException | ClientException e) {
            log.error("Got error when sending message with keyboard to user {}", userId);
            throw new RuntimeException(e);
        }
    }

    private Keyboard buildKeyboard() {
        KeyboardButton yesButton = new KeyboardButton()
                .setAction(new KeyboardButtonActionCallback()
                        .setType(KeyboardButtonActionCallbackType.CALLBACK)
                        .setLabel("Yes")
                        .setPayload("{\"button\": \"yes\"}"))
                .setColor(KeyboardButtonColor.POSITIVE);

        KeyboardButton noButton = new KeyboardButton()
                .setAction(new KeyboardButtonActionCallback()
                        .setType(KeyboardButtonActionCallbackType.CALLBACK)
                        .setLabel("No")
                        .setPayload("{\"button\": \"no\"}"))
                .setColor(KeyboardButtonColor.NEGATIVE);

        return new Keyboard()
                .setOneTime(true)
                .setButtons(List.of(List.of(yesButton, noButton)));
    }
}
