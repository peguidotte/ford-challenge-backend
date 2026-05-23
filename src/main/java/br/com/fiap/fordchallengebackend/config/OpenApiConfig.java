package br.com.fiap.fordchallengebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
            .title("Ford Challenge Backend API")
            .description("API para consolidacao de especificacoes tecnicas automotivas")
            .version("v1"));
    }
}
