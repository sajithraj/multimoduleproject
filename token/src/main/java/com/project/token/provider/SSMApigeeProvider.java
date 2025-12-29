package com.project.token.provider;

import com.project.token.transformer.ApigeeBearerTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class SSMApigeeProvider extends BaseProvider {

    private static final Logger log = LogManager.getLogger(SSMApigeeProvider.class);

    // Cache TTL: 55 minutes (3300 seconds)
    private static final int CACHE_TTL_SECONDS = 3300;

    // Validate required environment variables at class load time
    private static final String AWS_REGION = getRequiredEnv(SdkSystemSetting.AWS_REGION.environmentVariable());
    private static final String TOKEN_SECRET_NAME = getRequiredEnv("TOKEN_SECRET_NAME");
    private static final String TOKEN_ENDPOINT_URL = getRequiredEnv("TOKEN_ENDPOINT_URL");

    private final SecretsManagerClient client;

    private static String getRequiredEnv(String name) {
        // Try environment variable first (production)
        String val = System.getenv(name);

        // Fallback to system property (testing)
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty(name);
        }

        if (val == null || val.trim().isEmpty()) {
            throw new IllegalStateException("Required environment variable '" + name + "' is not set.");
        }
        return val;
    }

    /**
     * Constructor with CacheManager and TransformationManager.
     */
    private SSMApigeeProvider(CacheManager cacheManager,
                              SecretsManagerClient client,
                              TransformationManager transformationManager) {
        super(cacheManager, transformationManager);
        this.client = client;
        super.withMaxAge(CACHE_TTL_SECONDS, ChronoUnit.SECONDS);
        super.withTransformation(ApigeeBearerTransformer.class);
        log.info("SSMApigeeProvider initialized with Powertools caching (TTL: {} seconds / {} minutes)",
                CACHE_TTL_SECONDS, CACHE_TTL_SECONDS / 60);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SSMApigeeProvider get() {
        return builder().build();
    }


    public String getToken(String secretKey) {
        String key = (secretKey == null || secretKey.trim().isEmpty())
                ? TOKEN_SECRET_NAME : secretKey;

        long startTime = System.currentTimeMillis();
        log.debug("Requesting OAuth2 token for key: {}", key);

        String token = super.get(key);

        long totalTime = System.currentTimeMillis() - startTime;

        if (totalTime < 100) {
            log.info("OAuth2 bearer token retrieved from Powertools CACHE (fetch time: {} ms)", totalTime);
        } else {
            log.info("OAuth2 bearer token fetched fresh and CACHED by Powertools (fetch time: {} ms)", totalTime);
        }

        return token;
    }

    @Override
    protected String getValue(String key) {
        log.debug("Fetching secret from Secrets Manager: {}", key);

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(key)
                .build();

        return Optional.ofNullable(client.getSecretValue(request).secretString())
                .orElseGet(() -> new String(
                        Base64.getDecoder().decode(
                                client.getSecretValue(request)
                                        .secretBinary()
                                        .asByteArray()
                        ),
                        StandardCharsets.UTF_8
                ));
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {
        throw new UnsupportedOperationException(
                "Impossible to get multiple values from AWS Secrets Manager");
    }

    @Override
    public void resetToDefaults() {
        super.resetToDefaults();
    }

    public static class Builder {
        private SecretsManagerClient client;
        private CacheManager cacheManager;
        private TransformationManager transformationManager;

        private Builder() {
        }

        private static SecretsManagerClient createDefaultClient() {
            return SecretsManagerClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .region(Region.of(AWS_REGION))
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .putAdvancedOption(
                                    SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                    "powertools-parameters")
                            .build())
                    .build();
        }

        public Builder withClient(SecretsManagerClient client) {
            this.client = client;
            return this;
        }

        public Builder withCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        public Builder withTransformationManager(TransformationManager transformationManager) {
            this.transformationManager = transformationManager;
            return this;
        }

        public SSMApigeeProvider build() {
            if (this.client == null) {
                this.client = createDefaultClient();
            }

            if (this.cacheManager == null) {
                this.cacheManager = new CacheManager();
            }

            if (this.transformationManager == null) {
                this.transformationManager = new TransformationManager();
            }

            return new SSMApigeeProvider(this.cacheManager, this.client, this.transformationManager);
        }
    }
}

