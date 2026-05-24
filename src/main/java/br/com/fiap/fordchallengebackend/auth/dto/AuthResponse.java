package br.com.fiap.fordchallengebackend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticacao")
public record AuthResponse(
    @Schema(description = "Token de acesso JWT") String accessToken,
    @Schema(description = "Token de refresh JWT") String refreshToken,
    @Schema(description = "Tempo de expiracao em milissegundos") long expiresIn,
    @Schema(example = "joao@example.com") String email,
    @Schema(example = "ROLE_USER") String role
) {
}
