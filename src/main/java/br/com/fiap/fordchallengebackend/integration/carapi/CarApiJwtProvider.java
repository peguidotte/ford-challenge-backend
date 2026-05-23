package br.com.fiap.fordchallengebackend.integration.carapi;

import br.com.fiap.fordchallengebackend.config.AppProperties;
import br.com.fiap.fordchallengebackend.exception.ExternalIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CarApiJwtProvider {

    private static final int SAFETY_WINDOW_SECONDS = 120;

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private volatile String cachedJwt;
    private volatile Instant jwtExpiresAt = Instant.EPOCH;

    public CarApiJwtProvider(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public String getBearerToken() {
        var apiToken = appProperties.integration().carapi().apiToken();
        var apiSecret = appProperties.integration().carapi().apiSecret();
        if (isBlank(apiToken) || isBlank(apiSecret)) {
            return null;
        }

        if (isTokenValid()) {
            return cachedJwt;
        }

        synchronized (this) {
            if (isTokenValid()) {
                return cachedJwt;
            }
            refreshToken(apiToken.trim(), apiSecret.trim());
            return cachedJwt;
        }
    }

    private boolean isTokenValid() {
        if (cachedJwt == null || cachedJwt.isBlank()) {
            return false;
        }
        return jwtExpiresAt.minusSeconds(SAFETY_WINDOW_SECONDS).isAfter(Instant.now());
    }

    private void refreshToken(String apiToken, String apiSecret) {
        var timeout = java.time.Duration.ofMillis(appProperties.integration().carapi().timeoutMillis());
        var requestFactory = new org.springframework.http.client.JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);

        var authClient = RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(appProperties.integration().carapi().baseUrl())
            .build();

        try {
            var rawToken = authClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "api_token", apiToken,
                    "api_secret", apiSecret
                ))
                .retrieve()
                .body(String.class);

            if (rawToken == null || rawToken.isBlank()) {
                throw new ExternalIntegrationException("Falha ao autenticar no CarAPI: token JWT vazio", null);
            }

            var normalizedToken = normalizeJwtValue(rawToken);
            cachedJwt = normalizedToken;
            jwtExpiresAt = parseJwtExpiration(normalizedToken).orElse(Instant.now().plusSeconds(86400));
        } catch (RestClientResponseException ex) {
            throw new ExternalIntegrationException(
                "Falha ao autenticar no CarAPI",
                ex,
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString()
            );
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException("Falha ao autenticar no CarAPI", ex);
        }
    }

    private Optional<Instant> parseJwtExpiration(String jwt) {
        try {
            var parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Optional.empty();
            }

            var payloadPart = padBase64(parts[1]);
            var payloadBytes = Base64.getUrlDecoder().decode(payloadPart);
            var payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payloadJson);
            if (!node.hasNonNull("exp")) {
                return Optional.empty();
            }
            return Optional.of(Instant.ofEpochSecond(node.get("exp").asLong()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String normalizeJwtValue(String token) {
        var trimmed = token.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String padBase64(String input) {
        var padding = (4 - (input.length() % 4)) % 4;
        return input + "=".repeat(padding);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
