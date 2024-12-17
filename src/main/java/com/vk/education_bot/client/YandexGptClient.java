package com.vk.education_bot.client;

import com.vk.education_bot.configuration.YandexGptProperties;
import com.vk.education_bot.dto.question.ProjectQuestionGptRequest;
import com.vk.education_bot.dto.question.ProjectQuestionGptResponse;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.dto.question.QuestionPredictionRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class YandexGptClient {

    private final static String BEARER = "Bearer ";
    private final static String CLS_URI_TEMPLATE = "cls://%s/yandexgpt/latest";
    private final static String GPT_URI_TEMPLATE = "gpt://%s/yandexgpt/latest";
    private final static String CLS_TASK_URI = "/foundationModels/v1/fewShotTextClassification";
    private final static String GPT_TASK_URI = "/foundationModels/v1/completion";
    private final static String CLS_TASK_DESCRIPTION = "Question classification";

    @Getter
    @AllArgsConstructor
    public enum GptTaskDescription {
        ANSWER_ABOUT_PROJECT("Тебе дана информация о проекте и в конце вопрос к нему. Ответь на вопрос"),
        COMMON_ANSWER("Ты бот, который помогает студентам работающим над проектами VK Education Projects. Ответь на заданный студентом вопрос.");

        private final String text;
    }

    private final WebClient webClient;
    private final String token;
    private final YandexGptProperties yandexGptProperties;

    public YandexGptClient(YandexGptProperties yandexGptProperties) {
        this.token = BEARER + yandexGptProperties.token();
        this.webClient = WebClient.create(yandexGptProperties.host());
        this.yandexGptProperties = yandexGptProperties;
    }

    public QuestionPrediction classifyQuestion(List<String> labels, String question) {
        log.info("Testing request: {}", question);
        return wrapCall(() -> webClient.post()
                .uri(CLS_TASK_URI)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(buildQuestionClassificationRequest(labels, question))
                .retrieve()
                .bodyToMono(QuestionPrediction.class)
                .block());
    }

    public String generateAnswer(String prompt, GptTaskDescription taskDescription) {
        log.info("Sending question to YandexGPT: {}", prompt);
        ProjectQuestionGptRequest request = buildAnswerCompletionRequest(prompt, taskDescription);

        ProjectQuestionGptResponse response = webClient.post()
                .uri(GPT_TASK_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProjectQuestionGptResponse.class)
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

    private <T> T wrapCall(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException("Api error", e);
        }
    }
}
