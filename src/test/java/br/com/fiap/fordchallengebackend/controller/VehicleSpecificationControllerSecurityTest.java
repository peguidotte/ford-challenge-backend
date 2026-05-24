package br.com.fiap.fordchallengebackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.fordchallengebackend.dto.VehicleQueryResponse;
import br.com.fiap.fordchallengebackend.service.VehicleSpecificationService;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
class VehicleSpecificationControllerSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        VehicleSpecificationService vehicleSpecificationService() {
            var mock = mock(VehicleSpecificationService.class);
            var response = new VehicleQueryResponse(
                "Ford", "Ranger", "Raptor",
                Map.of("Motor", "V6 3.0L"), Map.of("Motor", "FIPE"),
                OffsetDateTime.now()
            );
            when(mock.query(any())).thenReturn(response);
            return mock;
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldAllowUserToQueryVehicleSpecs() throws Exception {
        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void shouldAllowAnalystToQueryVehicleSpecs() throws Exception {
        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToQueryVehicleSpecs() throws Exception {
        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenNoAuth() throws Exception {
        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
                    """))
            .andExpect(status().isUnauthorized());
    }
}
