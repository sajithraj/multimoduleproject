package com.project.service.util;

import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;

public class HttpClientFactory {

    private static final Logger log = LogManager.getLogger(HttpClientFactory.class);
    private static volatile HttpClient client;

    public static HttpClient getClient() {
        if (client == null) {
            synchronized (HttpClientFactory.class) {
                if (client == null) {
                    try {
                        // Create SSL context that trusts all certificates (same approach as token project)
                        SSLContext sslContext = SSLContexts.custom()
                                .setProtocol("TLSv1.2")
                                .loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
                                .build();

                        client = HttpClient.newBuilder()
                                .sslContext(sslContext)
                                .connectTimeout(Duration.ofSeconds(30))
                                .build();

                        log.info("HTTP client initialized with custom SSL configuration (java.net.http)");
                    } catch (Exception e) {
                        log.error("Failed to create HTTP client with custom SSL, falling back to default", e);
                        client = HttpClient.newHttpClient();
                    }
                }
            }
        }
        return client;
    }
}

