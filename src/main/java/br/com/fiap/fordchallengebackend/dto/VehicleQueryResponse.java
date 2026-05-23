package br.com.fiap.fordchallengebackend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Saida consolidada e padronizada")
public record VehicleQueryResponse(
    String brand,
    String model,
    String version,
    Map<String, String> attributes,
    Map<String, String> sources,
    OffsetDateTime generatedAt
) {
}
