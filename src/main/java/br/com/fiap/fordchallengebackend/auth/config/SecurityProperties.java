package br.com.fiap.fordchallengebackend.auth.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    String jwtSecret,
    long jwtExpirationMs,
    long jwtRefreshExpirationMs,
    List<String> corsAllowedOrigins
) {
}
