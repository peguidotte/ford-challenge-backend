package br.com.fiap.fordchallengebackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.fordchallengebackend.service.FipeCatalogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
class FipeCatalogControllerSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        FipeCatalogService fipeCatalogService() {
            var mock = mock(FipeCatalogService.class);
            when(mock.getBrands("cars", null)).thenReturn(List.of());
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
    void shouldDenyUserFromAccessingFipeBrands() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/fipe/brands"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void shouldAllowAnalystToAccessFipeBrands() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/fipe/brands"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToAccessFipeBrands() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/fipe/brands"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/fipe/brands"))
            .andExpect(status().isUnauthorized());
    }
}
