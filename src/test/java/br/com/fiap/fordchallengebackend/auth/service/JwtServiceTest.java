package br.com.fiap.fordchallengebackend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.fordchallengebackend.auth.config.SecurityProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        var securityProperties = new SecurityProperties(
            "dGhpcyBpcyBhIHZlcnkgc2VjdXJlIGtleSBmb3IgdGVzdGluZyBwdXJwb3NlcyBvbmx5",
            900000L,
            604800000L,
            List.of("*")
        );
        jwtService = new JwtService(securityProperties);

        userDetails = User.builder()
            .username("john@example.com")
            .password("encoded-password")
            .roles("USER")
            .build();
    }

    @Test
    void shouldGenerateTokenAndExtractEmail() {
        var token = jwtService.generateAccessToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);

        var extractedEmail = jwtService.extractEmail(token);
        assertThat(extractedEmail).isEqualTo("john@example.com");
    }

    @Test
    void shouldRejectTokenWithAlteredSignature() {
        var token = jwtService.generateAccessToken(userDetails);

        var tamperedToken = token.substring(0, token.length() - 4) + "xxxx";

        assertThat(jwtService.isTokenValid(tamperedToken, userDetails)).isFalse();
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        var token = jwtService.generateAccessToken(userDetails);

        var otherUser = User.builder()
            .username("other@example.com")
            .password("password")
            .roles("USER")
            .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void shouldGenerateAndUseRefreshToken() {
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.split("\\.")).hasSize(3);

        var extractedEmail = jwtService.extractEmail(refreshToken);
        assertThat(extractedEmail).isEqualTo("john@example.com");

        var newAccessToken = jwtService.refreshAccessToken(refreshToken);
        assertThat(newAccessToken).isNotNull();
        assertThat(newAccessToken.split("\\.")).hasSize(3);

        var newExtractedEmail = jwtService.extractEmail(newAccessToken);
        assertThat(newExtractedEmail).isEqualTo("john@example.com");
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        assertThat(jwtService.refreshAccessToken("invalid.token.here")).isNull();
    }
}
