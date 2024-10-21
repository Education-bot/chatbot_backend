package com.vk.education_bot.handler;

import com.vk.api.sdk.events.callback.CallbackApi;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.education_bot.client.VkClient;
import com.vk.education_bot.configuration.BotProperties;
import org.springframework.stereotype.Component;

@Component
public class CommonCallbackHandler extends CallbackApi {

    private final static String DEFAULT_MESSAGE = "Message accepted!";
    private final VkClient vkClient;

    protected CommonCallbackHandler(BotProperties botProperties, VkClient vkClient) {
        super(botProperties.confirmationCode());
        this.vkClient = vkClient;
    }

    @Override
    public void messageNew(Integer groupId, MessageNew message) {
        long userId = message
                .getObject()
                .getMessage()
                .getFromId();
        vkClient.sendMessage(userId, DEFAULT_MESSAGE);
    }
}
