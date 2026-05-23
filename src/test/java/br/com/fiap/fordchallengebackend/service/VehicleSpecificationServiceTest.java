package br.com.fiap.fordchallengebackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.fordchallengebackend.domain.VehicleEnrichmentOverride;
import br.com.fiap.fordchallengebackend.dto.FipeBrandResponse;
import br.com.fiap.fordchallengebackend.dto.FipeModelResponse;
import br.com.fiap.fordchallengebackend.dto.FipePriceResponse;
import br.com.fiap.fordchallengebackend.dto.FipeYearResponse;
import br.com.fiap.fordchallengebackend.dto.VehicleQueryRequest;
import br.com.fiap.fordchallengebackend.integration.carapi.CarApiClient;
import br.com.fiap.fordchallengebackend.integration.fipe.FipeClient;
import br.com.fiap.fordchallengebackend.repository.VehicleEnrichmentOverrideRepository;
import br.com.fiap.fordchallengebackend.repository.VehicleQueryHistoryRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleSpecificationServiceTest {

    @Mock
    private CarApiClient carApiClient;

    @Mock
    private FipeClient fipeClient;

    @Mock
    private VehicleEnrichmentOverrideRepository overrideRepository;

    @Mock
    private VehicleQueryHistoryRepository historyRepository;

    @InjectMocks
    private VehicleSpecificationService service;

    @Test
    void shouldResolveAttributesWithFipeAndCarApiPriorityAndFallbackToInternalDataSource() {
        when(carApiClient.searchTechnicalAttributes("Ford", "Ranger", "Raptor", null))
            .thenReturn(Map.of("Motor", "V6 3.0"));

        stubFipePriceLookup();

        var override = new VehicleEnrichmentOverride();
        override.setBrand("Ford");
        override.setModel("Ranger");
        override.setVersion("Raptor");
        override.setAttributeName("Motor");
        override.setAttributeValue("V6 3.0L Nano bi turbo");

        when(overrideRepository.findByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase("Ford", "Ranger", "Raptor"))
            .thenReturn(List.of(override));

        var request = new VehicleQueryRequest(
            "Ford",
            "Ranger",
            "Raptor",
            "cars",
            null,
            List.of("Motor", "Modos de conducao")
        );

        var result = service.query(request);

        assertThat(result.attributes().get("Motor")).isEqualTo("V6 3.0");
        assertThat(result.sources().get("Motor")).isEqualTo("CAR_API");
        assertThat(result.attributes().get("Modos de conducao")).isEqualTo("Nao disponivel");
        assertThat(result.sources().get("Modos de conducao")).isEqualTo("NOT_FOUND");

        verify(historyRepository).save(any());
    }

    @Test
    void shouldResolvePriceFromFipeWhenRequested() {
        when(carApiClient.searchTechnicalAttributes("Ford", "Ranger", "Raptor", null))
            .thenReturn(Map.of());
        when(overrideRepository.findByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase("Ford", "Ranger", "Raptor"))
            .thenReturn(List.of());

        stubFipePriceLookup();

        var request = new VehicleQueryRequest(
            "Ford",
            "Ranger",
            "Raptor",
            "cars",
            null,
            List.of("Preco de referencia", "Codigo FIPE")
        );

        var result = service.query(request);

        assertThat(result.attributes().get("Preco de referencia")).isEqualTo("R$ 499.000");
        assertThat(result.sources().get("Preco de referencia")).isEqualTo("FIPE");
        assertThat(result.attributes().get("Codigo FIPE")).isEqualTo("001234-5");
        assertThat(result.sources().get("Codigo FIPE")).isEqualTo("FIPE");
    }

    @Test
    void shouldUseInternalDataSourceWhenNotFoundInFipeAndCarApi() {
        when(carApiClient.searchTechnicalAttributes("Ford", "Ranger", "Raptor", null))
            .thenReturn(Map.of());

        stubFipePriceLookup();

        var override = new VehicleEnrichmentOverride();
        override.setBrand("Ford");
        override.setModel("Ranger");
        override.setVersion("Raptor");
        override.setAttributeName("Motor");
        override.setAttributeValue("V6 3.0L Nano bi turbo");

        when(overrideRepository.findByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase("Ford", "Ranger", "Raptor"))
            .thenReturn(List.of(override));

        var request = new VehicleQueryRequest(
            "Ford",
            "Ranger",
            "Raptor",
            "cars",
            null,
            List.of("Motor")
        );

        var result = service.query(request);

        assertThat(result.attributes().get("Motor")).isEqualTo("V6 3.0L Nano bi turbo");
        assertThat(result.sources().get("Motor")).isEqualTo("INTERNAL_DATA_SOURCE");
    }

    private void stubFipePriceLookup() {
        when(fipeClient.getBrands("cars", null))
            .thenReturn(List.of(new FipeBrandResponse("1", "Ford")));
        when(fipeClient.getModels("cars", 1, null))
            .thenReturn(List.of(new FipeModelResponse("10", "Ranger Raptor 3.0 V6")));
        when(fipeClient.getYears("cars", 1, 10, null))
            .thenReturn(List.of(new FipeYearResponse("2024-3", "2024 Diesel")));
        when(fipeClient.getPrice("cars", 1, 10, "2024-3", null))
            .thenReturn(new FipePriceResponse(
                "Ford",
                "Ranger Raptor",
                2024,
                "Diesel",
                "001234-5",
                "R$ 499.000",
                "maio de 2026"
            ));
    }
}
