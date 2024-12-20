package com.vk.education_bot.client;

import com.vk.education_bot.configuration.YandexGptProperties;
import com.vk.education_bot.dto.question.QuestionPrediction;
import com.vk.education_bot.dto.question.QuestionPredictionRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class YandexGptClient {
    private final static String BEARER = "Bearer ";
    private final static String MODEL_URI_TEMPLATE = "cls://%s/yandexgpt/latest";
    private final static String TASK_DESCRIPTION = "Question classification";

    private final WebClient webClient;
    private final String modelUri;
    private static final Logger log = LoggerFactory.getLogger(YandexGptClient.class);

    private MetadataClient metadataClient = new MetadataClient();

    public YandexGptClient(YandexGptProperties yandexGptProperties) {
        this.modelUri = MODEL_URI_TEMPLATE.formatted(yandexGptProperties.folderId());
        this.webClient = WebClient.create(yandexGptProperties.host());
    }

    public QuestionPrediction classifyQuestion(List<String> labels, String question) {
        log.info("Testing request: {}", question);
        try {
            return webClient.post()
                    .uri("/foundationModels/v1/fewShotTextClassification")
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(StandardCharsets.UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", BEARER + metadataClient.getToken())
                    .bodyValue(buildRequest(labels, question))
                    .retrieve()
                    .bodyToMono(QuestionPrediction.class)
                    .block();
        } catch (Exception exc) {
            log.error("Can't perform request to YandexGPT: ", exc);
            throw new RuntimeException(exc);
        }
    }

    private QuestionPredictionRequest buildRequest(List<String> labels, String question) {
        return new QuestionPredictionRequest(
                modelUri,
                TASK_DESCRIPTION,
                labels,
                question
        );
    }
}
