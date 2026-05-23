package br.com.fiap.fordchallengebackend.controller;

import br.com.fiap.fordchallengebackend.dto.FipeBrandResponse;
import br.com.fiap.fordchallengebackend.dto.FipeModelResponse;
import br.com.fiap.fordchallengebackend.dto.FipePriceResponse;
import br.com.fiap.fordchallengebackend.dto.FipeYearResponse;
import br.com.fiap.fordchallengebackend.service.FipeCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/fipe")
@Tag(name = "FIPE Catalog")
public class FipeCatalogController {

    private final FipeCatalogService fipeCatalogService;

    public FipeCatalogController(FipeCatalogService fipeCatalogService) {
        this.fipeCatalogService = fipeCatalogService;
    }

    @GetMapping("/brands")
    @Operation(summary = "Lista marcas FIPE por tipo de veiculo")
    public List<FipeBrandResponse> brands(
        @RequestParam(defaultValue = "cars") String vehicleType,
        @RequestParam(required = false) Integer reference
    ) {
        return fipeCatalogService.getBrands(vehicleType, reference);
    }

    @GetMapping("/brands/{brandId}/models")
    @Operation(summary = "Lista modelos FIPE por marca")
    public List<FipeModelResponse> models(
        @PathVariable int brandId,
        @RequestParam(defaultValue = "cars") String vehicleType,
        @RequestParam(required = false) Integer reference
    ) {
        return fipeCatalogService.getModels(vehicleType, brandId, reference);
    }

    @GetMapping("/brands/{brandId}/models/{modelId}/years")
    @Operation(summary = "Lista anos FIPE por modelo")
    public List<FipeYearResponse> years(
        @PathVariable int brandId,
        @PathVariable int modelId,
        @RequestParam(defaultValue = "cars") String vehicleType,
        @RequestParam(required = false) Integer reference
    ) {
        return fipeCatalogService.getYears(vehicleType, brandId, modelId, reference);
    }

    @GetMapping("/brands/{brandId}/models/{modelId}/years/{yearId}")
    @Operation(summary = "Consulta preco FIPE por versao de ano")
    public FipePriceResponse price(
        @PathVariable int brandId,
        @PathVariable int modelId,
        @PathVariable String yearId,
        @RequestParam(defaultValue = "cars") String vehicleType,
        @RequestParam(required = false) Integer reference
    ) {
        return fipeCatalogService.getPrice(vehicleType, brandId, modelId, yearId, reference);
    }
}
