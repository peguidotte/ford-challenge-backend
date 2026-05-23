package br.com.fiap.fordchallengebackend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.fordchallengebackend.dto.FipeBrandResponse;
import br.com.fiap.fordchallengebackend.dto.FipeModelResponse;
import br.com.fiap.fordchallengebackend.dto.FipePriceResponse;
import br.com.fiap.fordchallengebackend.dto.FipeYearResponse;
import br.com.fiap.fordchallengebackend.integration.fipe.FipeClient;

@Service
public class FipeCatalogService {

    private final FipeClient fipeClient;

    public FipeCatalogService(FipeClient fipeClient) {
        this.fipeClient = fipeClient;
    }

    public List<FipeBrandResponse> getBrands(String vehicleType, Integer reference) {
        return fipeClient.getBrands(vehicleType, reference);
    }

    public List<FipeModelResponse> getModels(String vehicleType, int brandId, Integer reference) {
        return fipeClient.getModels(vehicleType, brandId, reference);
    }

    public List<FipeYearResponse> getYears(String vehicleType, int brandId, int modelId, Integer reference) {
        return fipeClient.getYears(vehicleType, brandId, modelId, reference);
    }

    public FipePriceResponse getPrice(String vehicleType, int brandId, int modelId, String yearId, Integer reference) {
        return fipeClient.getPrice(vehicleType, brandId, modelId, yearId, reference);
    }
}
