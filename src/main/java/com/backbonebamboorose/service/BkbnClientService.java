package com.backbonebamboorose.service;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.exception.ExternalApiException;
import com.backbonebamboorose.model.bkbn.BkbnMaterialsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BkbnClientService {

    private final RestTemplate restTemplate;
    private final WebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;

    private volatile String cachedJwtToken;
    private volatile long tokenExpiryTime;

    public String getJwtToken() {
        if (cachedJwtToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedJwtToken;
        }
        return authenticate();
    }

    public synchronized String authenticate() {
        if (cachedJwtToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedJwtToken;
        }

        String url = webhookProperties.getBkbn().getBaseUrl() + "/auth/token";

        log.info("Authenticating with BKBN Sync API");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = objectMapper.writeValueAsString(JsonNode.class)
                    .replace("\"null\"", "")
                    .replace("null,", "")
                    .replace(",null", "");

            String jsonBody = String.format(
                    "{\"clientId\":\"%s\",\"secret\":\"%s\"}",
                    webhookProperties.getBkbn().getClientId(),
                    webhookProperties.getBkbn().getClientSecret()
            );

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String token = json.get("token").asText();
                cachedJwtToken = token;
                tokenExpiryTime = System.currentTimeMillis() + (55 * 60 * 1000);
                log.info("BKBN authentication successful, token cached");
                return token;
            } else {
                throw new ExternalApiException(
                        "BKBN authentication failed",
                        response.getStatusCode().value(),
                        response.getBody()
                );
            }
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to authenticate with BKBN", e);
            throw new RuntimeException("BKBN authentication failed", e);
        }
    }

    public BkbnMaterialsResponse getMaterials(String orderId, String assignmentId, String product, String visualType) {
        String token = getJwtToken();
        String url = String.format(
                "%s/materials/%s/%s?product=%s&visualType=%s",
                webhookProperties.getBkbn().getBaseUrl(),
                orderId,
                assignmentId,
                product,
                visualType
        );

        log.info("Fetching materials from BKBN: orderId={}, assignmentId={}", orderId, assignmentId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), BkbnMaterialsResponse.class);
            } else {
                throw new ExternalApiException(
                        "BKBN materials API returned error",
                        response.getStatusCode().value(),
                        response.getBody()
                );
            }
        } catch (Exception e) {
            log.error("Failed to fetch materials from BKBN", e);
            throw new RuntimeException("Failed to fetch materials from BKBN", e);
        }
    }

    public boolean healthCheck() {
        try {
            String url = webhookProperties.getBkbn().getBaseUrl() + "/auth/token";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            return response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError();
        } catch (Exception e) {
            log.warn("BKBN health check failed", e);
            return false;
        }
    }
}
