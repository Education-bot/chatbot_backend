package com.vk.education_bot.dto.question;

import java.util.List;

public record QuestionPrediction(
        List<Prediction> predictions
) {
    public record Prediction(
            String label,
            double confidence
    ) {

    }
}
