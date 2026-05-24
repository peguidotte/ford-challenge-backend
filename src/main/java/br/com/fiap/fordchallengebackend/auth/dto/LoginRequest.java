package br.com.fiap.fordchallengebackend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisicao de login")
public record LoginRequest(
    @NotBlank @Email @Schema(example = "joao@example.com") String email,
    @NotBlank @Schema(example = "senha123") String password
) {
}
