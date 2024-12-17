package com.vk.education_bot.dto.question;

import java.util.List;

public record ProjectQuestionGptRequest(
        String modelUri,
        CompletionOptions completionOptions,
        List<Message> messages
) {
    public record CompletionOptions(
            boolean stream,
            double temperature,
            String maxTokens
    ) {
    }

    public record Message(
            String role,
            String text
    ) {
    }
}