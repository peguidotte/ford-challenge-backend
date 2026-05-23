package br.com.fiap.fordchallengebackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.fordchallengebackend.dto.VehicleQueryResponse;
import br.com.fiap.fordchallengebackend.exception.ExternalIntegrationException;
import br.com.fiap.fordchallengebackend.exception.GlobalExceptionHandler;
import br.com.fiap.fordchallengebackend.service.VehicleSpecificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class VehicleSpecificationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private VehicleSpecificationService vehicleSpecificationService;

    @InjectMocks
    private VehicleSpecificationController vehicleSpecificationController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(vehicleSpecificationController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnConsolidatedVehicleResponse() throws Exception {
        var response = new VehicleQueryResponse(
            "Ford",
            "Ranger",
            "Raptor",
            Map.of("Motor", "V6 3.0L Nano bi turbo"),
            Map.of("Motor", "INTERNAL_DATA_SOURCE"),
            OffsetDateTime.parse("2026-05-23T12:00:00Z")
        );

        when(vehicleSpecificationService.query(any())).thenReturn(response);

        var request = Map.of(
            "brand", "Ford",
            "model", "Ranger",
            "version", "Raptor",
            "vehicleType", "cars",
            "attributes", new String[] {"Motor"}
        );

        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.brand").value("Ford"))
            .andExpect(jsonPath("$.attributes.Motor").value("V6 3.0L Nano bi turbo"))
                .andExpect(jsonPath("$.sources.Motor").value("INTERNAL_DATA_SOURCE"));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        var invalidRequest = Map.of(
            "brand", "",
            "model", "Ranger",
            "version", "Raptor",
            "attributes", new String[] {}
        );

        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Dados de entrada invalidos"));
    }

    @Test
    void shouldReturnBadGatewayWhenServiceThrowsExternalIntegrationException() throws Exception {
        when(vehicleSpecificationService.query(any()))
            .thenThrow(new ExternalIntegrationException("Falha externa", new RuntimeException()));

        var request = Map.of(
            "brand", "Ford",
            "model", "Ranger",
            "version", "Raptor",
            "vehicleType", "cars",
            "attributes", new String[] {"Motor"}
        );

        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.status").value(502))
            .andExpect(jsonPath("$.message").value("Falha externa"));
    }
}
