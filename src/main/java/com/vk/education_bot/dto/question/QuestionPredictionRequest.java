package com.vk.education_bot.dto.question;

import java.util.List;

public record QuestionPredictionRequest(
        String modelUri,
        String taskDescription,
        List<String> labels,
        String text
) {
}
