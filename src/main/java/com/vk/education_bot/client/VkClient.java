package com.vk.education_bot.client;

import com.vk.api.sdk.actions.Messages;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
                    .sendUserIds(groupActor)
                    .userId(userId)
                    .message(message)
                    .execute();
        } catch (ApiException | ClientException e) {
            log.error("Got error when send message to user {}", userId);
            throw new RuntimeException(e);
        }
    }
}
