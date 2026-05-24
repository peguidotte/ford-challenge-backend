package br.com.fiap.fordchallengebackend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CorsConfigTest {

    @Autowired
    private CorsConfig corsConfig;

    @Test
    void shouldLoadCorsConfig() {
        assertThat(corsConfig).isNotNull();
    }
}
