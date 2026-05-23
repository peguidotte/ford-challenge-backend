package br.com.fiap.fordchallengebackend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.fordchallengebackend.dto.FipeBrandResponse;
import br.com.fiap.fordchallengebackend.dto.FipeModelResponse;
import br.com.fiap.fordchallengebackend.dto.FipePriceResponse;
import br.com.fiap.fordchallengebackend.dto.FipeYearResponse;
import br.com.fiap.fordchallengebackend.service.FipeCatalogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FipeCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FipeCatalogService fipeCatalogService;

    @InjectMocks
    private FipeCatalogController fipeCatalogController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(fipeCatalogController).build();
    }

    @Test
    void shouldReturnBrands() throws Exception {
        when(fipeCatalogService.getBrands("cars", null))
            .thenReturn(List.of(new FipeBrandResponse("22", "Ford")));

        mockMvc.perform(get("/api/v1/catalog/fipe/brands").param("vehicleType", "cars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("22"))
            .andExpect(jsonPath("$[0].name").value("Ford"));
    }

    @Test
    void shouldReturnModels() throws Exception {
        when(fipeCatalogService.getModels("cars", 22, null))
            .thenReturn(List.of(new FipeModelResponse("100", "Ranger Raptor 3.0 V6")));

        mockMvc.perform(get("/api/v1/catalog/fipe/brands/22/models").param("vehicleType", "cars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("100"))
            .andExpect(jsonPath("$[0].name").value("Ranger Raptor 3.0 V6"));
    }

    @Test
    void shouldReturnYears() throws Exception {
        when(fipeCatalogService.getYears("cars", 22, 100, null))
            .thenReturn(List.of(new FipeYearResponse("2024-3", "2024 Diesel")));

        mockMvc.perform(get("/api/v1/catalog/fipe/brands/22/models/100/years").param("vehicleType", "cars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("2024-3"))
            .andExpect(jsonPath("$[0].name").value("2024 Diesel"));
    }

    @Test
    void shouldReturnPrice() throws Exception {
        when(fipeCatalogService.getPrice("cars", 22, 100, "2024-3", null))
            .thenReturn(new FipePriceResponse(
                "Ford",
                "Ranger Raptor",
                2024,
                "Diesel",
                "001234-5",
                "R$ 499.000",
                "maio de 2026"
            ));

        mockMvc.perform(get("/api/v1/catalog/fipe/brands/22/models/100/years/2024-3").param("vehicleType", "cars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.brand").value("Ford"))
            .andExpect(jsonPath("$.codeFipe").value("001234-5"))
            .andExpect(jsonPath("$.price").value("R$ 499.000"));
    }
}
