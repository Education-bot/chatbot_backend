package com.vk.education_bot.handler;

import com.vk.api.sdk.client.GsonHolder;
import com.vk.api.sdk.events.Events;
import com.vk.api.sdk.events.callback.CallbackApi;
import com.vk.api.sdk.objects.callback.MessageNew;
import com.vk.api.sdk.objects.callback.messages.CallbackMessage;
import com.vk.education_bot.client.VkClient;
import com.vk.education_bot.configuration.BotProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CommonCallbackHandler extends CallbackApi {

    private final static String DEFAULT_MESSAGE = "Message accepted!";
    private final static String EMPTY = "";
    private final VkClient vkClient;
    private final BotProperties botProperties;

    public CommonCallbackHandler(BotProperties botProperties, VkClient vkClient) {
        super(botProperties.confirmationCode());
        this.vkClient = vkClient;
        this.botProperties = botProperties;
    }

    @Override
    public void messageNew(Integer groupId, MessageNew message) {
        long userId = Optional.ofNullable(message
                .getObject()
                .getMessage()
                .getFromId())
                .orElseThrow(() -> new RuntimeException("UserId not presented"));
        vkClient.sendMessage(userId, DEFAULT_MESSAGE);
    }

    @Override
    public String parse(String json) {
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
