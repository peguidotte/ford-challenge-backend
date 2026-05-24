package br.com.fiap.fordchallengebackend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisicao de refresh de token")
public record RefreshRequest(
    @NotBlank @Schema(description = "Token de refresh JWT") String refreshToken
) {
}
