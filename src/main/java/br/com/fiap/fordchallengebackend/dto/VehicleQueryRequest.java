package br.com.fiap.fordchallengebackend.dto;

import java.util.List;

import br.com.fiap.fordchallengebackend.validation.Brand;
import br.com.fiap.fordchallengebackend.validation.ModelName;
import br.com.fiap.fordchallengebackend.validation.ValidAttributes;
import br.com.fiap.fordchallengebackend.validation.ValidVehicleType;
import br.com.fiap.fordchallengebackend.validation.VersionName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Entrada para consolidacao de especificacoes")
public record VehicleQueryRequest(
    @Brand @Schema(example = "Ford") String brand,
    @ModelName @Schema(example = "Ranger") String model,
    @VersionName @Schema(example = "Raptor") String version,
    @ValidVehicleType @Schema(example = "cars", defaultValue = "cars") String vehicleType,
    @Min(1) @Max(999) @Schema(example = "333", description = "Referencia FIPE opcional") Integer reference,
    @ValidAttributes List<String> attributes
) {
}
