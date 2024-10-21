package com.vk.education_bot.controller;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.education_bot.configuration.BotProperties;
import com.vk.education_bot.dto.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConfigurationController {

    private final BotProperties botProperties;
    private static final String TYPE_CONFIRMATION = "confirmation";
    private static final String DEFAULT = "";

    /**
     * may be not needed, @see CommonCallbackHandler.CommonCallbackHandler(BotProperties botProperties, VkClient vkClient)
      */
    @PostMapping("/")
    public String confirmServer(@RequestBody Request request) {
        if (TYPE_CONFIRMATION.equals(request.type()) && request.groupId() == botProperties.groupId()) {
            return botProperties.confirmationCode();
        }
        return DEFAULT;
    }
}
