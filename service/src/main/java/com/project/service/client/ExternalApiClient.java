package com.project.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.service.config.AppConfig;
import com.project.service.exception.ExternalApiException;
import com.project.service.util.HttpClientFactory;
import com.project.token.provider.SSMApigeeProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for calling external APIs.
 * Uses Powertools v2 ApigeeSecretsProvider for OAuth2 token authentication.
 * Uses java.net.http.HttpClient (same as token project) for consistency.
 */
public class ExternalApiClient {

    private static final Logger log = LogManager.getLogger(ExternalApiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile ExternalApiClient instance;
    private static volatile SSMApigeeProvider tokenProvider;


    /**
     * Gets singleton instance of ExternalApiClient.
     */
    public static ExternalApiClient getInstance() {
        if (instance == null) {
            synchronized (ExternalApiClient.class) {
                if (instance == null) {
                    instance = new ExternalApiClient();
                    tokenProvider = SSMApigeeProvider.get();
                    log.info("ExternalApiClient initialized with SSMApigeeProvider (with bearer token caching)");
                }
            }
        }
        return instance;
    }

    /**
     * Calls external API with authentication token.
     *
     * @return API response as String
     * @throws ExternalApiException if call fails
     */
    public String callExternalApi() {
        log.info("Initiating external API call to: {}", AppConfig.EXTERNAL_API_URL);

        try {
            // Fetch bearer token using Powertools v2 provider with BaseProvider caching
            String accessToken = tokenProvider.getToken(null);
            log.debug("Retrieved access token from provider, length: {} characters", accessToken.length());

            // Build HTTP request using java.net.http (same as token project)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.EXTERNAL_API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("x-dealer-code", "Z3DT01")
                    .header("x-bod-id", "17b1c782-1a09-4588-ac37-9d4534e5f977")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            log.debug("Executing HTTP GET request to external API");

            // Execute request using java.net.http.HttpClient
            HttpResponse<String> response = HttpClientFactory.getClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                log.info("External API call successful: status={}", statusCode);
                return response.body();
            } else {
                log.error("External API error: status={}, body={}", statusCode, response.body());
                throw new ExternalApiException("API returned status: " + statusCode);
            }

        } catch (IOException e) {
            log.error("Network error calling external API", e);
            throw new ExternalApiException("Network error during API call", e);
        } catch (InterruptedException e) {
            log.error("Request interrupted", e);
            Thread.currentThread().interrupt();
            throw new ExternalApiException("Request interrupted", e);
        } catch (Exception e) {
            log.error("Unexpected error during API call", e);
            throw new ExternalApiException("Unexpected error during API call", e);
        }
    }
}

