package com.vk.education_bot.controller;

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

    @PostMapping("/")
    public String confirmServer(@RequestBody Request request) {
        System.out.println(botProperties);
        if (TYPE_CONFIRMATION.equals(request.type()) && request.groupId() == botProperties.groupId()) {
            return botProperties.confirmationCode();
        }
        return DEFAULT;
    }
}
