package com.backbonebamboorose.service;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.exception.ExternalApiException;
import com.backbonebamboorose.exception.UnrecoverableException;
import com.backbonebamboorose.model.bamboorose.BambooRoseQuote;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BambooRoseClientService {

    private final RestTemplate restTemplate;
    private final WebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;

    @Retry(name = "bamboorose", fallbackMethod = "syncQuoteFallback")
    @CircuitBreaker(name = "bamboorose", fallbackMethod = "syncQuoteCircuitBreakerFallback")
    public BambooRoseQuote syncQuote(BambooRoseQuote quote) {
        String url = webhookProperties.getBamboorose().getBaseUrl() + "/api/v1/quotes";

        log.info("Sending quote to Bamboo Rose: quoteNumber={}", quote.getQuoteNumber());

        try {
            HttpHeaders headers = createHeaders();
            String requestBody = objectMapper.writeValueAsString(quote);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Quote synced successfully to Bamboo Rose: quoteNumber={}", quote.getQuoteNumber());
                return objectMapper.readValue(response.getBody(), BambooRoseQuote.class);
            } else {
                throw new ExternalApiException(
                        "Unexpected response from Bamboo Rose",
                        response.getStatusCode().value(),
                        response.getBody()
                );
            }
        } catch (HttpClientErrorException e) {
            log.error("Client error from Bamboo Rose: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UnrecoverableException("Authentication failed with Bamboo Rose", e);
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new UnrecoverableException("Invalid quote data sent to Bamboo Rose", e);
            }
            throw new ExternalApiException(
                    "Bamboo Rose API client error",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e
            );
        } catch (HttpServerErrorException e) {
            log.error("Server error from Bamboo Rose: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalApiException(
                    "Bamboo Rose API server error",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e
            );
        } catch (ResourceAccessException e) {
            log.error("Connection error to Bamboo Rose", e);
            throw new RuntimeException("Failed to connect to Bamboo Rose API", e);
        } catch (UnrecoverableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error syncing quote to Bamboo Rose", e);
            throw new RuntimeException("Failed to sync quote to Bamboo Rose", e);
        }
    }

    @Retry(name = "bamboorose", fallbackMethod = "updateQuoteFallback")
    @CircuitBreaker(name = "bamboorose", fallbackMethod = "updateQuoteCircuitBreakerFallback")
    public BambooRoseQuote updateQuote(String quoteId, BambooRoseQuote quote) {
        String url = webhookProperties.getBamboorose().getBaseUrl() + "/api/v1/quotes/" + quoteId;

        log.info("Updating quote in Bamboo Rose: quoteId={}", quoteId);

        try {
            HttpHeaders headers = createHeaders();
            headers.set("X-External-Quote-Id", quote.getExternalQuoteId());
            String requestBody = objectMapper.writeValueAsString(quote);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Quote updated successfully in Bamboo Rose: quoteId={}", quoteId);
                return objectMapper.readValue(response.getBody(), BambooRoseQuote.class);
            } else {
                throw new ExternalApiException(
                        "Unexpected response from Bamboo Rose",
                        response.getStatusCode().value(),
                        response.getBody()
                );
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Quote not found in Bamboo Rose, will create instead: quoteId={}", quoteId);
            return syncQuote(quote);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new UnrecoverableException("Authentication failed with Bamboo Rose", e);
            }
            throw new ExternalApiException(
                    "Bamboo Rose API client error",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error updating quote in Bamboo Rose", e);
            throw new RuntimeException("Failed to update quote in Bamboo Rose", e);
        }
    }

    public boolean healthCheck() {
        try {
            String url = webhookProperties.getBamboorose().getBaseUrl() + "/api/v1/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Bamboo Rose health check failed", e);
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + webhookProperties.getBamboorose().getApiKey());
        headers.set("X-API-Key", webhookProperties.getBamboorose().getApiKey());
        headers.set("X-Source", "BackboneToBbr-WebhookService");
        return headers;
    }

    public BambooRoseQuote syncQuoteFallback(BambooRoseQuote quote, Throwable t) {
        log.error("Fallback triggered for syncQuote: quoteNumber={}, error={}",
                quote.getQuoteNumber(), t.getMessage());
        throw new ExternalApiException(
                "Bamboo Rose sync failed after retries",
                503,
                "Service unavailable after retries: " + t.getMessage()
        );
    }

    public BambooRoseQuote syncQuoteCircuitBreakerFallback(BambooRoseQuote quote, Throwable t) {
        log.error("Circuit breaker open for syncQuote: quoteNumber={}, error={}",
                quote.getQuoteNumber(), t.getMessage());
        throw new ExternalApiException(
                "Bamboo Rose circuit breaker open",
                503,
                "Circuit breaker is open: " + t.getMessage()
        );
    }

    public BambooRoseQuote updateQuoteFallback(String quoteId, BambooRoseQuote quote, Throwable t) {
        log.error("Fallback triggered for updateQuote: quoteId={}, error={}", quoteId, t.getMessage());
        throw new ExternalApiException(
                "Bamboo Rose update failed after retries",
                503,
                "Service unavailable after retries: " + t.getMessage()
        );
    }

    public BambooRoseQuote updateQuoteCircuitBreakerFallback(String quoteId, BambooRoseQuote quote, Throwable t) {
        log.error("Circuit breaker open for updateQuote: quoteId={}, error={}", quoteId, t.getMessage());
        throw new ExternalApiException(
                "Bamboo Rose circuit breaker open",
                503,
                "Circuit breaker is open: " + t.getMessage()
        );
    }
}
