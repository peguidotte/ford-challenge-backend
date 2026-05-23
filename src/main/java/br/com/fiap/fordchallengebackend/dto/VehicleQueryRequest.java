package br.com.fiap.fordchallengebackend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Entrada para consolidacao de especificacoes")
public record VehicleQueryRequest(
    @NotBlank @Schema(example = "Ford") String brand,
    @NotBlank @Schema(example = "Ranger") String model,
    @NotBlank @Schema(example = "Raptor") String version,
    @Schema(example = "cars", defaultValue = "cars") String vehicleType,
    @Schema(example = "308", description = "Referencia FIPE opcional") Integer reference,
    @NotEmpty List<@NotBlank String> attributes
) {
}
