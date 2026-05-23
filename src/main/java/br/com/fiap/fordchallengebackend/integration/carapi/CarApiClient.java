package br.com.fiap.fordchallengebackend.integration.carapi;

import br.com.fiap.fordchallengebackend.exception.ExternalIntegrationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CarApiClient {

    private final RestClient restClient;

    public CarApiClient(@Qualifier("carApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, String> searchTechnicalAttributes(String make, String model, String version, Integer year) {
        try {
            var trims = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/trims/v2")
                    .queryParam("make", make)
                    .queryParam("model", model)
                    .queryParamIfPresent("year", java.util.Optional.ofNullable(year))
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                });

            if (trims == null || trims.isEmpty()) {
                return Map.of();
            }

            var bestTrim = selectBestTrim(trims, version);
            var normalized = new LinkedHashMap<String, String>();

            mapIfPresent(bestTrim, normalized, "engine", "Motor");
            mapIfPresent(bestTrim, normalized, "transmission", "Transmissao");
            mapIfPresent(bestTrim, normalized, "drive", "Tracao");
            mapIfPresent(bestTrim, normalized, "fuel_type", "Combustivel");
            mapIfPresent(bestTrim, normalized, "horsepower_hp", "Potencia");
            mapIfPresent(bestTrim, normalized, "torque_ft_lbs", "Torque");

            return normalized;
        } catch (RestClientResponseException ex) {
            throw new ExternalIntegrationException(
                "Falha ao consultar especificacoes no CarAPI",
                ex,
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString()
            );
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException("Falha ao consultar especificacoes no CarAPI", ex);
        }
    }

    private Map<String, Object> selectBestTrim(List<Map<String, Object>> trims, String version) {
        if (version == null || version.isBlank()) {
            return trims.getFirst();
        }

        var normalizedVersion = normalize(version);
        return trims.stream()
            .filter(trim -> normalize(String.valueOf(trim.getOrDefault("name", ""))).contains(normalizedVersion))
            .findFirst()
            .orElseGet(trims::getFirst);
    }

    private void mapIfPresent(Map<String, Object> source, Map<String, String> target, String sourceKey, String targetKey) {
        var value = source.get(sourceKey);
        if (value != null && !String.valueOf(value).isBlank()) {
            target.put(targetKey, String.valueOf(value));
        }
    }

    private String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
