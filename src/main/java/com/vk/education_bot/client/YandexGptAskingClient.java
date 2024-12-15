package com.vk.education_bot.client;

import com.vk.education_bot.configuration.YandexGptProperties;
import com.vk.education_bot.dto.question.ProjectQuestionGptRequest;
import com.vk.education_bot.dto.question.ProjectQuestionGptResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
public class YandexGptAskingClient {

    private final static String BEARER = "Bearer ";
    private final static String MODEL_URI_TEMPLATE = "gpt://%s/yandexgpt-lite";


    private final WebClient webClient;
    private final String token;
    private final String modelUri;

    public YandexGptAskingClient(YandexGptProperties yandexGptProperties) {
        this.token = BEARER + yandexGptProperties.token();
        this.modelUri = MODEL_URI_TEMPLATE.formatted(yandexGptProperties.folderId());
        this.webClient = WebClient.create(yandexGptProperties.host());
    }

    public String sendQuestion(String prompt) {
        log.info("Sending question to YandexGPT: {}", prompt);
        ProjectQuestionGptRequest.CompletionOptions completionOptions = new ProjectQuestionGptRequest.CompletionOptions(
                false, // stream
                0.1,   // temperature
                "1000" // maxTokens
        );

        List<ProjectQuestionGptRequest.Message> messages = List.of(
                new ProjectQuestionGptRequest.Message("system", "Тебе дана информация о проекте и в конце вопрос к нему. Ответь на вопрос"),
                new ProjectQuestionGptRequest.Message("user", prompt)
        );

        ProjectQuestionGptRequest request = new ProjectQuestionGptRequest(
                modelUri,
                completionOptions,
                messages
        );

        ProjectQuestionGptResponse response = webClient.post()
                .uri("/foundationModels/v1/completion")
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
}
