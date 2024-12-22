package com.vk.education_bot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("properties.yandex-gpt")
public record YandexGptProperties(
    String folderId,
    String host
) {
}
