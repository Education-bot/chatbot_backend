package com.vk.education_bot.client;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.education_bot.configuration.BotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class VkClient {

    private final VkApiClient vkApiClient;
    private final GroupActor groupActor;
    private final BotProperties botProperties;

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

    public void sendMessageWithKeyboard(long userId, String message, Keyboard keyboard) {
        try {
            vkApiClient.messages()
                    .sendDeprecated(groupActor)
                    .userId(userId)
                    .message(message)
                    .keyboard(keyboard)
                    .randomId(ThreadLocalRandom.current().nextInt())
                    .execute();
        } catch (ApiException | ClientException e) {
            log.error("Error sending message with keyboard to user {}", userId, e);
        }
    }

    public String getConfirmationCode() {
        var code = botProperties.confirmationCode();
        try {
            code = vkApiClient.groups()
                .getCallbackConfirmationCode(groupActor, botProperties.groupId())
                .execute()
                .getCode();
        } catch (ApiException | ClientException e) {
            log.error("Error getting group confirmation code", e);
        }
        return code;
    }

}
