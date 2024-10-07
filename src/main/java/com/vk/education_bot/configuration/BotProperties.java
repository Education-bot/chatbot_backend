package com.vk.education_bot.configuration;

import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("properties")
public record BotProperties(
        long groupId,
        String confirmationCode,
        String accessCode
) {

}
