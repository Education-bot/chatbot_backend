package com.vk.education_bot.controller;

import com.vk.education_bot.handler.CommonCallbackHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VkCallbackController {

    private final CommonCallbackHandler commonCallbackHandler;

    @PostMapping("/")
    public String handlerCallbackRequest(@RequestBody String request) {
        return commonCallbackHandler.parse(request);
    }
}
