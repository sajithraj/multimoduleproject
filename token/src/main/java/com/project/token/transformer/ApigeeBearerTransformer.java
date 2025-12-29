package com.project.token.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.constraints.NotNull;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.transform.BasicTransformer;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * ApigeeBearerTransformer extends BasicTransformer (required for Powertools v2 String transformation).
 */
public class ApigeeBearerTransformer extends BasicTransformer {

    private static final Logger log = LogManager.getLogger(ApigeeBearerTransformer.class);

    private static final String TRUST_STORE_LOCATION = "/tmp/trustStore.jks";
    private static final String TOKEN_ENDPOINT_URL = getRequiredEnv("TOKEN_ENDPOINT_URL");

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final URI tokenEndpointUrl;
    private final int timeoutSeconds;

    public ApigeeBearerTransformer() {
        this.tokenEndpointUrl = URI.create(TOKEN_ENDPOINT_URL);

        SSLContext sslContext = null;

        try {
            KeyStore ks = KeyStore.getInstance("JKS");

            String filename = System.getProperty("java.home")
                    + "/lib/security/cacerts".replace('/', File.separatorChar);

            try (InputStream fis = this.getClass().getResourceAsStream(filename);
                 InputStream inputStream =
                         this.getClass().getResourceAsStream("/svb_root_ssl_cert.pem");
                 FileOutputStream fos =
                         new FileOutputStream(TRUST_STORE_LOCATION)) {

                ks.load(fis, "".toCharArray());

                ks.setCertificateEntry(
                        "svbRoot",
                        CertificateFactory.getInstance("X.509")
                                .generateCertificate(inputStream)
                );

                ks.store(fos, "".toCharArray());

                System.setProperty("javax.net.ssl.trustStore", TRUST_STORE_LOCATION);
                System.setProperty("javax.net.ssl.trustStorePassword", "");
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            log.error("Failed to create keystore with provided certificate: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            log.error("Failed to instantiate keystore: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        try {
            sslContext = SSLContexts.custom()
                    .setProtocol("TLSv1.2")
                    .loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
                    .build();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Failed to create custom SSL context: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if (sslContext == null) {
            log.warn("Custom SSL context is null, using default SSL context");
            try {
                sslContext = SSLContext.getDefault();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        this.timeoutSeconds = getTimeoutValue();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());

        log.info("ApigeeBearerTransformer initialized successfully, endpoint: {}", this.tokenEndpointUrl);
    }

    private static String getRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required environment variable '" + key + "' is not set");
        }
        return value;
    }

    @Override
    public String applyTransformation(String value) {
        ApigeeAuthToken token;

        try {
            token = mapper.readValue(value, ApigeeAuthToken.class);
            log.debug("Parsed OAuth2 credentials - username present: {}, password present: {}",
                    (token.userName() != null && !token.userName().isEmpty()),
                    (token.password() != null && !token.password().isEmpty()));
        } catch (Exception e) {
            log.error("Failed to parse OAuth2 credentials from secret: {}", e.getMessage());
            throw new TransformationException(e);
        }

        try {
            log.debug("Sending OAuth2 token request to endpoint: {}", this.tokenEndpointUrl);

            // Create fresh request for each invocation (HttpRequest.Builder cannot be reused)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(this.tokenEndpointUrl)
                    .version(HttpClient.Version.HTTP_2)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", getBasicAuthorization(token))
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .timeout(Duration.of(this.timeoutSeconds, ChronoUnit.SECONDS))
                    .build();

            HttpResponse<String> response = this.httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (HttpStatusCode.OK == response.statusCode()) {
                log.info("Successfully retrieved OAuth2 bearer token from endpoint: {}", this.tokenEndpointUrl);
                return mapper.readValue(
                        response.body(),
                        ApigeeOauthResponse.class
                ).accessToken();
            } else {
                int statusCode = response.statusCode();
                String responseBody = response.body();
                log.error("OAuth2 endpoint returned error - status: {}, response: {}",
                        statusCode, responseBody);
                throw new RuntimeException(
                        "Error received from OAuth2 endpoint. Status: " + statusCode + ", Response: " + responseBody);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to call OAuth2 endpoint at {}: {}",
                    this.tokenEndpointUrl, e.getMessage());
            throw new RuntimeException("Exception while attempting to call OAuth2 endpoint", e);
        }
    }

    private String getBasicAuthorization(ApigeeAuthToken token) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (token.userName() + ":" + token.password()).getBytes()
        );
    }

    private Integer getTimeoutValue() {
        try {
            return Integer.parseInt(System.getenv("OAUTH2_TIMEOUT_SECONDS"));
        } catch (Exception e) {
            log.debug("OAUTH2_TIMEOUT_SECONDS not set, using default timeout of 3 seconds");
            return 3;
        }
    }

    record ApigeeAuthToken(
            @JsonProperty(value = "username", required = true)
            @NotNull String userName,
            @JsonProperty(value = "password", required = true)
            @NotNull String password
    ) implements Serializable {
    }

    record ApigeeOauthResponse(
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("issued_at") Instant issuedAt,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn
    ) implements Serializable {
    }
}

