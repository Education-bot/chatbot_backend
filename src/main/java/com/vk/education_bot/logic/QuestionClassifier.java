package com.vk.education_bot.logic;

import com.vk.education_bot.client.YandexGptClient;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.entity.Question;
import com.vk.education_bot.repository.QuestionRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionClassifier {

    private final YandexGptClient yandexGptClient;
    private final QuestionRepository questionRepository;
    private static final int BATCH_SIZE = 20;
    private static final String UNKNOWN_QUESTION = "Другое";

    // todo optimize and add cache
    public Optional<Question> classifyQuestion(@Nullable String questionText) {
        if (questionText == null) {
            return Optional.empty();
        }
        List<String> questions = questionRepository.findAll().stream()
                .map(Question::getText)
                .toList();
        return questionRepository.findByText(iterateQuestionsAndGetMatches(questions, questionText));
    }

    public String iterateQuestionsAndGetMatches(List<String> questions, String questionText) {
        List<String> intermediateResult = new ArrayList<>();
        for (int i = 0; i < questions.size(); i += BATCH_SIZE) {
            intermediateResult.add(getMaxFitPrediction(questions.subList(i, Math.min(questions.size(), i + BATCH_SIZE)), questionText));
        }
        if (intermediateResult.size() < BATCH_SIZE - 1) {
            intermediateResult.add(UNKNOWN_QUESTION);
            return getMaxFitPrediction(intermediateResult, questionText);
        }
        return iterateQuestionsAndGetMatches(intermediateResult, questionText);
    }

    public String getMaxFitPrediction(List<String> allQuestions, String questionText) {
        return yandexGptClient.classifyQuestion(allQuestions, questionText)
                .predictions()
                .stream()
                .max(Comparator.comparingDouble(QuestionPrediction.Prediction::confidence))
                .map(QuestionPrediction.Prediction::label)
                .orElseThrow(() -> new RuntimeException("Label not presented in response"));
    }
}
