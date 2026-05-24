package br.com.fiap.fordchallengebackend.auth.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.fordchallengebackend.auth.domain.Role;
import br.com.fiap.fordchallengebackend.auth.domain.User;
import br.com.fiap.fordchallengebackend.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        userRepository.deleteAll();

        var user = new User();
        user.setName("Test User");
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        var admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void shouldReturn401WithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "wrongpassword"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WithNonExistentEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "nonexistent@test.com", "password": "password123"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        var responseBody = objectMapper.readValue(
            loginResponse.getResponse().getContentAsString(), Map.class);
        var refreshToken = (String) responseBody.get("refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.email").value("user@test.com"));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "invalid.refresh.token.here"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WhenAccessingProtectedEndpointWithValidToken() throws Exception {
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        var responseBody = objectMapper.readValue(
            loginResponse.getResponse().getContentAsString(), Map.class);
        var accessToken = (String) responseBody.get("accessToken");

        mockMvc.perform(post("/api/v1/vehicle-specs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("""
                    {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAllowHealthEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenNonAdminRegistersUser() throws Exception {
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        var responseBody = objectMapper.readValue(
            loginResponse.getResponse().getContentAsString(), Map.class);
        var accessToken = (String) responseBody.get("accessToken");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("""
                    {"name": "New User", "email": "new@test.com", "password": "password123"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn201WhenAdminRegistersUser() throws Exception {
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "admin@test.com", "password": "admin123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        var responseBody = objectMapper.readValue(
            loginResponse.getResponse().getContentAsString(), Map.class);
        var accessToken = (String) responseBody.get("accessToken");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("""
                    {"name": "New User", "email": "new@test.com", "password": "password123", "role": "ROLE_ANALYST"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.email").value("new@test.com"))
            .andExpect(jsonPath("$.role").value("ROLE_ANALYST"));
    }
}
