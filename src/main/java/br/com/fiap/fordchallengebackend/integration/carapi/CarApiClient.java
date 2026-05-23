package br.com.fiap.fordchallengebackend.integration.carapi;

import br.com.fiap.fordchallengebackend.exception.ExternalIntegrationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class CarApiClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;

    public CarApiClient(@Qualifier("carApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, String> searchTechnicalAttributes(String make, String model, String version, Integer year) {
        try {
            var trimsPayload = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/trims/v2")
                    .queryParam("make", make)
                    .queryParam("model", model)
                    .queryParamIfPresent("year", java.util.Optional.ofNullable(year))
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            var trims = extractRecords(trimsPayload);

            if (trims == null || trims.isEmpty()) {
                return Map.of();
            }

            var bestTrim = selectBestTrim(trims, version);
            var trimId = extractTrimId(bestTrim).orElse(null);
            var trimDetails = trimId == null ? Map.<String, Object>of() : safeGetTrimById(trimId);
            var engineData = trimId == null ? Map.<String, Object>of() : safeGetEngineByTrimId(trimId, make, model, year);
            var bodyData = trimId == null ? Map.<String, Object>of() : safeGetBodyByTrimId(trimId, make, model, year);

            var normalized = new LinkedHashMap<String, String>();

            // Match fields that the service already canonicalizes.
            putIfPresent(normalized, "Transmissao", firstNonBlank(
                engineData.get("transmission"),
                bestTrim.get("transmission")
            ));
            putIfPresent(normalized, "Tracao", firstNonBlank(
                engineData.get("drive_type"),
                bestTrim.get("drive")
            ));
            putIfPresent(normalized, "Combustivel", firstNonBlank(
                engineData.get("fuel_type"),
                bestTrim.get("fuel_type")
            ));
            putIfPresent(normalized, "Potencia", formatHorsepower(engineData));
            putIfPresent(normalized, "Torque", formatTorque(engineData));
            putIfPresent(normalized, "Motor", formatEngine(engineData, trimDetails, bestTrim));

            // Additional raw technical data that can be requested dynamically.
            putIfPresent(normalized, "Ano", firstNonBlank(engineData.get("year"), trimDetails.get("year"), bestTrim.get("year")));
            putIfPresent(normalized, "Tipo de motor", firstNonBlank(engineData.get("engine_type")));
            putIfPresent(normalized, "Cilindros", firstNonBlank(engineData.get("cylinders")));
            putIfPresent(normalized, "Cilindrada", firstNonBlank(engineData.get("size")));
            putIfPresent(normalized, "Tipo de carroceria", firstNonBlank(bodyData.get("type")));
            putIfPresent(normalized, "Portas", firstNonBlank(bodyData.get("doors")));
            putIfPresent(normalized, "Assentos", firstNonBlank(bodyData.get("seats")));
            putIfPresent(normalized, "Capacidade de reboque", firstNonBlank(bodyData.get("max_towing_capacity")));
            putIfPresent(normalized, "Versao tecnica", firstNonBlank(
                trimDetails.get("description"),
                bestTrim.get("description"),
                trimDetails.get("trim"),
                bestTrim.get("trim")
            ));

            if (trimId != null) {
                normalized.put("CarAPI Trim ID", String.valueOf(trimId));
            }

            return normalized;
        } catch (RestClientResponseException ex) {
            throw new ExternalIntegrationException(
                "Falha ao consultar especificacoes no CarAPI",
                ex,
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString()
            );
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException(
                "Falha ao consultar especificacoes no CarAPI: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                ex
            );
        }
    }

    private Map<String, Object> safeGetTrimById(Long trimId) {
        try {
            var details = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/trims/v2/{id}")
                    .build(trimId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            return toMap(details);
        } catch (RestClientException ex) {
            return Map.of();
        }
    }

    private Map<String, Object> safeGetEngineByTrimId(Long trimId, String make, String model, Integer year) {
        try {
            var payload = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/engines/v2")
                    .queryParam("trim_id", trimId)
                    .queryParam("make", make)
                    .queryParam("model", model)
                    .queryParamIfPresent("year", java.util.Optional.ofNullable(year))
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            return extractRecords(payload).stream().findFirst().orElseGet(Map::of);
        } catch (RestClientException ex) {
            return Map.of();
        }
    }

    private Map<String, Object> safeGetBodyByTrimId(Long trimId, String make, String model, Integer year) {
        try {
            var payload = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/bodies/v2")
                    .queryParam("trim_id", trimId)
                    .queryParam("make", make)
                    .queryParam("model", model)
                    .queryParamIfPresent("year", java.util.Optional.ofNullable(year))
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            return extractRecords(payload).stream().findFirst().orElseGet(Map::of);
        } catch (RestClientException ex) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> extractRecords(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return List.of();
        }

        if (payload.isArray()) {
            return StreamSupport.stream(payload.spliterator(), false)
                .map(node -> OBJECT_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {
                }))
                .toList();
        }

        if (payload.isObject() && payload.has("data") && payload.get("data").isArray()) {
            var data = payload.get("data");
            return StreamSupport.stream(data.spliterator(), false)
                .map(node -> OBJECT_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {
                }))
                .toList();
        }

        return List.of();
    }

    private Map<String, Object> selectBestTrim(List<Map<String, Object>> trims, String version) {
        if (version == null || version.isBlank()) {
            return trims.getFirst();
        }

        var normalizedVersion = normalize(version);
        return trims.stream()
            .filter(trim -> normalize(extractTrimName(trim)).contains(normalizedVersion))
            .findFirst()
            .orElseGet(trims::getFirst);
    }

    private Optional<Long> extractTrimId(Map<String, Object> trim) {
        var idValue = trim.getOrDefault("id", trim.get("trim_id"));
        if (idValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(String.valueOf(idValue)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> toMap(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return Map.of();
        }
        return OBJECT_MAPPER.convertValue(payload, new TypeReference<Map<String, Object>>() {
        });
    }

    private String formatHorsepower(Map<String, Object> engineData) {
        var hp = firstNonBlank(engineData.get("horsepower_hp"));
        if (hp.isBlank()) {
            return "";
        }

        var rpm = firstNonBlank(engineData.get("horsepower_rpm"));
        return rpm.isBlank() ? hp + " hp" : hp + " hp @ " + rpm + " rpm";
    }

    private String formatTorque(Map<String, Object> engineData) {
        var torque = firstNonBlank(engineData.get("torque_ft_lbs"));
        if (torque.isBlank()) {
            return "";
        }

        var rpm = firstNonBlank(engineData.get("torque_rpm"));
        return rpm.isBlank() ? torque + " lb-ft" : torque + " lb-ft @ " + rpm + " rpm";
    }

    private String formatEngine(
        Map<String, Object> engineData,
        Map<String, Object> trimDetails,
        Map<String, Object> bestTrim
    ) {
        var cylinders = firstNonBlank(engineData.get("cylinders"));
        var size = firstNonBlank(engineData.get("size"));
        var fuel = firstNonBlank(engineData.get("fuel_type"));

        var pieces = new java.util.ArrayList<String>();
        if (!cylinders.isBlank()) {
            pieces.add(cylinders);
        }
        if (!size.isBlank()) {
            pieces.add(size + "L");
        }
        if (!fuel.isBlank()) {
            pieces.add(fuel);
        }

        if (!pieces.isEmpty()) {
            return String.join(" ", pieces);
        }

        return firstNonBlank(
            trimDetails.get("description"),
            bestTrim.get("description"),
            trimDetails.get("trim"),
            bestTrim.get("trim")
        );
    }

    private String firstNonBlank(Object... candidates) {
        for (var candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            var text = String.valueOf(candidate).trim();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String extractTrimName(Map<String, Object> trim) {
        var directName = String.valueOf(trim.getOrDefault("name", ""));
        if (!directName.isBlank()) {
            return directName;
        }

        var description = String.valueOf(trim.getOrDefault("description", ""));
        if (!description.isBlank()) {
            return description;
        }

        var model = String.valueOf(trim.getOrDefault("model", ""));
        var submodel = String.valueOf(trim.getOrDefault("submodel", ""));
        var trimName = String.valueOf(trim.getOrDefault("trim", ""));
        return (model + " " + submodel + " " + trimName).trim();
    }

    private String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
