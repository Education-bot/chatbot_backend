package com.vk.education_bot.client;

import com.vk.education_bot.configuration.YandexGptProperties;
import com.vk.education_bot.dto.question.ProjectQuestionGptRequest;
import com.vk.education_bot.dto.question.ProjectQuestionGptResponse;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.dto.question.QuestionPredictionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class YandexGptClient {

    private final static String BEARER = "Bearer ";
    private final static String CLS_URI_TEMPLATE = "cls://%s/yandexgpt/latest";
    private final static String GPT_URI_TEMPLATE = "gpt://%s/yandexgpt/latest";
    private final static String CLS_TASK_URI = "/foundationModels/v1/fewShotTextClassification";
    private final static String GPT_TASK_URI = "/foundationModels/v1/completion";
    private final static String CLS_TASK_DESCRIPTION = "Question classification";
    private final static String GPT_TASK_DESCRIPTION = "Тебе дана информация о проекте и в конце вопрос к нему. Ответь на вопрос";

    private final WebClient webClient;
    private final MetadataClient metadataClient;
    private final YandexGptProperties yandexGptProperties;

    public YandexGptClient(YandexGptProperties yandexGptProperties) {
        this.webClient = WebClient.create(yandexGptProperties.host());
        this.metadataClient = new MetadataClient();
        this.yandexGptProperties = yandexGptProperties;
    }

    public QuestionPrediction classifyQuestion(List<String> labels, String question) throws Exception {
        log.info("Testing request: {}", question);
        return webClient.post()
                .uri(CLS_TASK_URI)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER + metadataClient.getToken())
                .bodyValue(buildRequest(labels, question))
                .retrieve()
                .bodyToMono(QuestionPrediction.class)
                .block();
    }

    private QuestionPredictionRequest buildRequest(List<String> labels, String question) {
        return new QuestionPredictionRequest(
                CLS_URI_TEMPLATE.formatted(yandexGptProperties.folderId()),
                CLS_TASK_DESCRIPTION,
                labels,
                question
        );
    }

    public String sendQuestion(String prompt) {
        log.info("Sending question to YandexGPT: {}", prompt);
        ProjectQuestionGptRequest.CompletionOptions completionOptions = new ProjectQuestionGptRequest.CompletionOptions(
                false, // stream
                0.1,   // temperature
                "1000" // maxTokens
        );

        List<ProjectQuestionGptRequest.Message> messages = List.of(
                new ProjectQuestionGptRequest.Message("system", GPT_TASK_DESCRIPTION),
                new ProjectQuestionGptRequest.Message("user", prompt)
        );

        ProjectQuestionGptRequest request = new ProjectQuestionGptRequest(
                GPT_URI_TEMPLATE.formatted(yandexGptProperties.folderId()),
                completionOptions,
                messages
        );

        ProjectQuestionGptResponse response = null;
        try {
            response = webClient.post()
                    .uri(GPT_TASK_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", BEARER + metadataClient.getToken())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ProjectQuestionGptResponse.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (response == null || response.result().alternatives().isEmpty()) {
            log.error("No response received from YandexGPT");
            throw new RuntimeException("Empty response from YandexGPT");
        }

        // Возвращаем текст из первого варианта ответа
        return response.result().alternatives().getFirst().message().text();
    }

}
