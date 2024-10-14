package com.vk.education_bot.configuration;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.exceptions.OAuthException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.GroupAuthResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class VkApiConfiguration {

    @Bean
    public VkApiClient vkApiClient() {
        TransportClient transportClient = new HttpTransportClient();
        return new VkApiClient(transportClient);
    }

    @Bean
    public GroupActor groupActor(VkApiClient vk, BotProperties botProperties) {
        GroupAuthResponse authResponse = null;
        try {
            authResponse = vk.oAuth()
                    .groupAuthorizationCodeFlow(0, "CLIENT_SECRET", "REDIRECT_URI", "code") // todo complete with app auth
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
//            e.getRedirectUri();
        }

        long groupId = botProperties.groupId();
        return new GroupActor(groupId, authResponse.getAccessTokens().get(groupId));
    }
}
