package org.nooshet.identity.configuration;

import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Configuration to warm up OpenAPI documentation at startup.
 */
@Configuration
public class OpenApiWarmupConfig {
    private final SpringDocConfigProperties springDocConfigProperties;
    private final OpenApiWebMvcResource openApiResource;
    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    public OpenApiWarmupConfig(
            SpringDocConfigProperties springDocConfigProperties,
            @Autowired(required = false) OpenApiWebMvcResource openApiResource) {
        this.springDocConfigProperties = springDocConfigProperties;
        this.openApiResource = openApiResource;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        try {
            String url = "http://localhost:" + serverPort + "/v3/api-docs";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.getResponseCode();
        } catch (Exception e) {
            // ignore
        }
    }
}

