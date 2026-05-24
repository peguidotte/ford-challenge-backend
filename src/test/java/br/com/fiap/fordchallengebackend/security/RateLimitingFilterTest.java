package br.com.fiap.fordchallengebackend.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimitingFilterTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        userRepository.deleteAll();

        var user = new User();
        user.setName("Test User");
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "password123"}
                    """))
            .andReturn();

        var body = objectMapper.readValue(loginResponse.getResponse().getContentAsString(), Map.class);
        accessToken = (String) body.get("accessToken");
    }

    @Test
    void shouldReturn429ForUnauthenticatedOnExcessRequests() throws Exception {
        var payload = """
            {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
            """;

        for (int i = 0; i < 20; i++) {
            var result = mockMvc.perform(post("/api/v1/vehicle-specs/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andReturn();
            if (result.getResponse().getStatus() == 429) {
                var retryAfter = result.getResponse().getHeader("Retry-After");
                assertNotNull(retryAfter, "Retry-After header should be present on 429 response");
                return;
            }
        }

        throw new AssertionError("Should have received 429 after exceeding rate limit for unauthenticated requests");
    }

    @Test
    void shouldReturn429ForAuthenticatedUserOnExcessRequests() throws Exception {
        var payload = """
            {"brand": "Ford", "model": "Ranger", "version": "Raptor", "attributes": ["Motor"]}
            """;

        for (int i = 0; i < 50; i++) {
            var result = mockMvc.perform(post("/api/v1/vehicle-specs/query")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andReturn();
            if (result.getResponse().getStatus() == 429) {
                var retryAfter = result.getResponse().getHeader("Retry-After");
                assertNotNull(retryAfter, "Retry-After header should be present on 429 response");
                return;
            }
        }

        throw new AssertionError("Should have received 429 after exceeding rate limit for authenticated user");
    }
}
