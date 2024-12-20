package com.vk.education_bot.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.education_bot.GetTokenException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetadataClient {
    private static final String METADATA_URL = "http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/token";
    private static final String METADATA_FLAVOR_HEADER = "Metadata-Flavor";
    private static final String METADATA_FLAVOR_VALUE = "Google";
    private static final Logger log = LoggerFactory.getLogger(MetadataClient.class);

    private String token;
    private Instant tokenExpirationTime;

    public String getToken() throws Exception {
        if (token == null || Instant.now().isAfter(tokenExpirationTime)) {
            log.info("IAM-token is expired or empty, try to fetch new one from metadata");
            fetchNewToken();
        }
        return token;
    }

    private void fetchNewToken() throws GetTokenException {
        HttpURLConnection connection;
        try {
            URL url = new URL(METADATA_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE);
        } catch (IOException exc) {
            log.error("Can't create request to metadata: ", exc);
            throw new GetTokenException();
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            parseTokenResponse(response.toString());
        } catch (JsonProcessingException exc) {
            log.error("Can't parse json form metadata response: ", exc);
            throw new GetTokenException();
        } catch (IOException exc) {
            log.error("Can't send request to metadata: ", exc);
            throw new GetTokenException();

        }
    }

    private void parseTokenResponse(String response) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.readTree(response);

        token = jsonResponse.get("access_token").asText();
        long expiresIn = jsonResponse.get("expires_in").asLong();
        // Устанавливаем время истечения токена как текущее время плюс продолжительность срока действия токена
        tokenExpirationTime = Instant.now().plusSeconds(expiresIn);
    }
}

