package com.vk.education_bot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("properties.vk")
public record BotProperties(
        long groupId,
        String confirmationCode,
        String accessCode,
        long supportUserId
) {

}
