package com.vk.education_bot.configuration;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class VkApiConfiguration {

    @Bean
    public VkApiClient vkApiClient() {
        TransportClient transportClient = HttpTransportClient.getInstance();
        return new VkApiClient(transportClient);
    }

    @Bean
    public GroupActor groupActor(BotProperties botProperties) {
        return new GroupActor(botProperties.groupId(), botProperties.accessCode());
    }
}
