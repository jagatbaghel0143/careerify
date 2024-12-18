package com.careerify.careerifyai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.api.client.util.Value;

@Configuration
public class GoogleApiConfig {
    @Value("${google.gemini.api.url}")
    private String apiUrl;

    @Value("${google.api.key}")
    private String apiKey;

    @Bean
    public WebClient googleGeminiWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl + "?key=" + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
