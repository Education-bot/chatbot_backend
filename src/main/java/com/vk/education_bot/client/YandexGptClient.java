package com.vk.education_bot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.education_bot.configuration.YandexGptProperties;
import com.vk.education_bot.dto.question.ProjectQuestionGptRequest;
import com.vk.education_bot.dto.question.ProjectQuestionGptResponse;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.dto.question.QuestionPredictionRequest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * todo rewrite with self implementation.
 * Problem and solution: https://medium.com/@m-elbably/rate-limiting-the-sliding-window-algorithm-daa1d91e6196
 * YGPT allows 1 rps, current rateLimiter allows only fixed window, cause 1s period can fails
 */
@Slf4j
@Component
public class YandexGptClient {

    private final static String BEARER = "Bearer ";
    private final static String CLS_URI_TEMPLATE = "cls://%s/yandexgpt/latest";
    private final static String GPT_URI_TEMPLATE = "gpt://%s/yandexgpt/latest";
    private final static String CLS_TASK_URI = "/foundationModels/v1/fewShotTextClassification";
    private final static String GPT_TASK_URI = "/foundationModels/v1/completion";
    private final static String CLS_TASK_DESCRIPTION = "Question classification";

    private final RateLimiter rateLimiter;
    private final WebClient webClient;
    private final String token;
    private final YandexGptProperties yandexGptProperties;

    @Getter
    @AllArgsConstructor
    public enum GptTaskDescription {
        ANSWER_ABOUT_PROJECT("Тебе дана информация о проекте и в конце вопрос к нему. Ответь на вопрос"),
        COMMON_ANSWER("Ты бот, который помогает студентам работающим над проектами VK Education Projects. Ответь на заданный студентом вопрос.");

        private final String text;
    }

    public YandexGptClient(YandexGptProperties yandexGptProperties, ObjectMapper objectMapper) {
        this.token = BEARER + yandexGptProperties.token();
        this.webClient = WebClient.create(yandexGptProperties.host());
        this.yandexGptProperties = yandexGptProperties;
        this.rateLimiter = RateLimiter.of("y-gpt-rate-limiter",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofSeconds(30))
                        .build());
    }

    public QuestionPrediction classifyQuestion(List<String> labels, String question) {
        log.info("Classify question: {}", question);
        return webClient.post()
                .uri(CLS_TASK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(buildQuestionClassificationRequest(labels, question))
                .retrieve()
                .bodyToMono(QuestionPrediction.class)
                .delayElement(Duration.ofSeconds(1))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .block();
    }

    public String generateAnswer(String prompt, GptTaskDescription taskDescription) {
        log.info("Generate answer for promt: {}", prompt);
        ProjectQuestionGptRequest request = buildAnswerCompletionRequest(prompt, taskDescription);
        ProjectQuestionGptResponse response = webClient.post()
                .uri(GPT_TASK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProjectQuestionGptResponse.class)
                .delayElement(Duration.ofSeconds(1))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .block();

        if (response == null || response.result().alternatives().isEmpty()) {
            log.error("No response received from YandexGPT");
            throw new RuntimeException("Empty response from YandexGPT");
        }

        // Возвращаем текст из первого варианта ответа
        return response.result().alternatives().getFirst().message().text();
    }

    private ProjectQuestionGptRequest buildAnswerCompletionRequest(String prompt, GptTaskDescription taskDescription) {
        ProjectQuestionGptRequest.CompletionOptions completionOptions = new ProjectQuestionGptRequest.CompletionOptions(
                false, // stream
                0.1,   // temperature
                "1000" // maxTokens
        );

        List<ProjectQuestionGptRequest.Message> messages = List.of(
                new ProjectQuestionGptRequest.Message("system", taskDescription.getText()),
                new ProjectQuestionGptRequest.Message("user", prompt)
        );

        return new ProjectQuestionGptRequest(
                GPT_URI_TEMPLATE.formatted(yandexGptProperties.folderId()),
                completionOptions,
                messages
        );
    }

    private QuestionPredictionRequest buildQuestionClassificationRequest(List<String> labels, String question) {
        return new QuestionPredictionRequest(
                CLS_URI_TEMPLATE.formatted(yandexGptProperties.folderId()),
                CLS_TASK_DESCRIPTION,
                labels,
                question
        );
    }
}
