package com.vk.education_bot.logic;

import com.vk.education_bot.client.YandexGptClient;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.entity.Question;
import com.vk.education_bot.repository.QuestionRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionClassifier {

    private final YandexGptClient yandexGptClient;
    private final QuestionRepository questionRepository;
    private static final int MAX_LABELS = 20;

    // todo optimize and add cache
    public Optional<Question> classifyQuestion(@Nullable String questionText) {
        if (questionText == null) {
            return Optional.empty();
        }
        List<String> allQuestionsText = questionRepository.findAll().stream()
                .map(Question::getText)
                .limit(MAX_LABELS)
                .toList();
        return yandexGptClient.classifyQuestion(allQuestionsText, questionText)
                .predictions()
                .stream()
                .max(Comparator.comparingDouble(QuestionPrediction.Prediction::confidence))
                .map(QuestionPrediction.Prediction::label)
                .flatMap(questionRepository::findByText);
    }
}
