package com.vk.education_bot.dto.question;

import java.util.List;

public record ProjectQuestionGptResponse(
        Result result
) {
    public record Result(
            List<Alternative> alternatives,
            Usage usage,
            String modelVersion
    ) {
        public record Alternative(
                Message message,
                String status
        ) {
            public record Message(
                    String role,
                    String text
            ) {
            }
        }

        public record Usage(
                int inputTextTokens,
                int completionTokens,
                int totalTokens
        ) {
        }
    }
}
