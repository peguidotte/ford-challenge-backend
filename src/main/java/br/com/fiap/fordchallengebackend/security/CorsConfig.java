package br.com.fiap.fordchallengebackend.security;

import br.com.fiap.fordchallengebackend.auth.config.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    public CorsConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var origins = securityProperties.corsAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            return;
        }
        registry.addMapping("/api/v1/**")
            .allowedOrigins(origins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("Content-Type", "Authorization", "X-Payload-Signature", "X-Timestamp")
            .exposedHeaders("Retry-After", "X-RateLimit-Remaining")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
