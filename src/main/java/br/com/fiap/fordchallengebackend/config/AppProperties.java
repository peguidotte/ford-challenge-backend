package br.com.fiap.fordchallengebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    IntegrationProperties integration
) {

    public record IntegrationProperties(
        FipeProperties fipe,
        CarApiProperties carapi
    ) {
    }

    public record FipeProperties(
        String baseUrl,
        String token,
        int timeoutMillis
    ) {
    }

    public record CarApiProperties(
        String baseUrl,
        String bearerToken,
        int timeoutMillis
    ) {
    }
}
