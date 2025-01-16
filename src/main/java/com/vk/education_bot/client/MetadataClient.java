package com.vk.education_bot.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;

@Slf4j
@Component
public class MetadataClient {
    private static final String METADATA_URL = "http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/token";
    private static final String METADATA_FLAVOR_HEADER = "Metadata-Flavor";
    private static final String METADATA_FLAVOR_VALUE = "Google";

    private String token;
    private Instant tokenExpirationTime;

    public String getToken() {
        if (token == null || Instant.now().isAfter(tokenExpirationTime)) {
            log.info("IAM token was expired, fetching a new one from metadata");
            try {
                fetchNewToken();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return token;
    }

    private void fetchNewToken() throws Exception {
        URI uri = new URI(METADATA_URL);
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            parseTokenResponse(response.toString());
        }
    }


    private void parseTokenResponse(String response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.readTree(response);

        token = jsonResponse.get("access_token").asText();
        long expiresIn = jsonResponse.get("expires_in").asLong();

        // Устанавливаем время истечения токена как текущее время плюс продолжительность срока действия токена
        tokenExpirationTime = Instant.now().plusSeconds(expiresIn);
    }
}

