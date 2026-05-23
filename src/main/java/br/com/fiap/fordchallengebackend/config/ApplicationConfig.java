package br.com.fiap.fordchallengebackend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

    @Bean
    RestClient fipeRestClient(AppProperties appProperties) {
        var timeout = Duration.ofMillis(appProperties.integration().fipe().timeoutMillis());
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);

        var builder = RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(appProperties.integration().fipe().baseUrl());

        var token = appProperties.integration().fipe().token();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("X-Subscription-Token", token);
        }
        return builder.build();
    }

    @Bean
    RestClient carApiRestClient(AppProperties appProperties) {
        var timeout = Duration.ofMillis(appProperties.integration().carapi().timeoutMillis());
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);

        var builder = RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(appProperties.integration().carapi().baseUrl());

        var bearerToken = appProperties.integration().carapi().bearerToken();
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + bearerToken);
        }
        return builder.build();
    }
}
