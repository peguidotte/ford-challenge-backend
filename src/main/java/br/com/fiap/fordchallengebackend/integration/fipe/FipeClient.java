package br.com.fiap.fordchallengebackend.integration.fipe;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.com.fiap.fordchallengebackend.dto.FipeBrandResponse;
import br.com.fiap.fordchallengebackend.dto.FipeModelResponse;
import br.com.fiap.fordchallengebackend.dto.FipePriceResponse;
import br.com.fiap.fordchallengebackend.dto.FipeYearResponse;
import br.com.fiap.fordchallengebackend.exception.ExternalIntegrationException;

@Component
public class FipeClient {

    private final RestClient restClient;

    public FipeClient(@Qualifier("fipeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<FipeBrandResponse> getBrands(String vehicleType, Integer reference) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{vehicleType}/brands")
                    .queryParamIfPresent("reference", java.util.Optional.ofNullable(reference))
                    .build(vehicleType))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {
                });
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException("Falha ao consultar marcas na FIPE", ex);
        }
    }

    public List<FipeModelResponse> getModels(String vehicleType, int brandId, Integer reference) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{vehicleType}/brands/{brandId}/models")
                    .queryParamIfPresent("reference", java.util.Optional.ofNullable(reference))
                    .build(vehicleType, brandId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {
                });
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException("Falha ao consultar modelos na FIPE", ex);
        }
    }

    public List<FipeYearResponse> getYears(String vehicleType, int brandId, int modelId, Integer reference) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{vehicleType}/brands/{brandId}/models/{modelId}/years")
                    .queryParamIfPresent("reference", java.util.Optional.ofNullable(reference))
                    .build(vehicleType, brandId, modelId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {
                });
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException("Falha ao consultar anos na FIPE", ex);
        }
    }

    public FipePriceResponse getPrice(String vehicleType, int brandId, int modelId, String yearId, Integer reference) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{vehicleType}/brands/{brandId}/models/{modelId}/years/{yearId}")
                    .queryParamIfPresent("reference", java.util.Optional.ofNullable(reference))
                    .build(vehicleType, brandId, modelId, yearId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(FipePriceResponse.class);
        } catch (RestClientException ex) {
            throw new ExternalIntegrationException("Falha ao consultar preco na FIPE", ex);
        }
    }
}
