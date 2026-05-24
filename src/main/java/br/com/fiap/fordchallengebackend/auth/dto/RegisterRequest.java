package br.com.fiap.fordchallengebackend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisicao de cadastro de usuario")
public record RegisterRequest(
    @NotBlank @Size(min = 1, max = 120) @Schema(example = "Joao Silva") String name,
    @NotBlank @Email @Size(max = 255) @Schema(example = "joao@example.com") String email,
    @NotBlank @Size(min = 6, max = 100) @Schema(example = "senha123") String password,
    @Schema(example = "ROLE_USER", defaultValue = "ROLE_USER") String role
) {
}
