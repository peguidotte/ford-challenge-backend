package br.com.fiap.fordchallengebackend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PayloadSigningFilterTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    private String calculateHmac(String body, String timestamp) {
        return PayloadSigningFilter.calculateHmac(body, timestamp, "test-payload-secret");
    }

    @Test
    void shouldAllowGetWithoutSignature() throws Exception {
        var status = mockMvc.perform(get("/api/v1/health"))
            .andReturn()
            .getResponse()
            .getStatus();
        assertTrue(status == 200 || status == 429,
            "Health endpoint should be accessible (200 or 429 if rate limited). Got: " + status);
    }

    @Test
    void calculateHmacShouldProduceValidHash() {
        var hash = calculateHmac("test-body", "2026-01-01T00:00:00Z");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void calculateHmacShouldDifferWithDifferentBody() {
        var hash1 = calculateHmac("body1", "2026-01-01T00:00:00Z");
        var hash2 = calculateHmac("body2", "2026-01-01T00:00:00Z");
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertTrue(!hash1.equals(hash2));
    }

    @Test
    void calculateHmacShouldDifferWithDifferentTimestamp() {
        var hash1 = calculateHmac("test-body", "2026-01-01T00:00:00Z");
        var hash2 = calculateHmac("test-body", "2026-01-01T00:01:00Z");
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertTrue(!hash1.equals(hash2));
    }

    @Test
    void shouldVerifyAuthEndpointDoesNotBlockWithMissingSignature() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "user@test.com", "password": "password123"}
                    """))
            .andReturn();
        var status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 429 || status == 200,
            "Auth endpoint should not be blocked by payload signing filter. Got: " + status);
    }
}
