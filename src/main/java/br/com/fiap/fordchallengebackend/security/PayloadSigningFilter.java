package br.com.fiap.fordchallengebackend.security;

import br.com.fiap.fordchallengebackend.auth.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class PayloadSigningFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadSigningFilter.class);
    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300;
    private static final Set<String> IGNORED_PATHS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/health"
    );
    private static final int HMAC_HEX_LENGTH = 64;

    private final SecurityProperties securityProperties;

    public PayloadSigningFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            return true;
        }
        var path = request.getRequestURI();
        return IGNORED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var payloadSecret = securityProperties.payloadSecret();
        if (payloadSecret == null || payloadSecret.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        var signature = request.getHeader("X-Payload-Signature");
        var timestamp = request.getHeader("X-Timestamp");

        if (signature == null || signature.isBlank()) {
            sendUnauthorized(response, "Assinatura de payload ausente");
            return;
        }

        if (timestamp == null || timestamp.isBlank()) {
            sendUnauthorized(response, "Timestamp ausente");
            return;
        }

        if (!isTimestampValid(timestamp)) {
            sendUnauthorized(response, "Timestamp expirado");
            return;
        }

        var body = readBody(request);

        var expectedSignature = calculateHmac(body, timestamp, payloadSecret);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            sendUnauthorized(response, "Assinatura de payload invalida");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isTimestampValid(String timestamp) {
        try {
            var ts = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestamp));
            var now = Instant.now();
            var diff = java.time.Duration.between(ts, now).abs();
            return diff.getSeconds() <= MAX_TIMESTAMP_AGE_SECONDS;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String readBody(HttpServletRequest request) {
        try {
            return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    static String calculateHmac(String body, String timestamp, String secret) {
        try {
            var data = body + "." + timestamp;
            var mac = Mac.getInstance("HmacSHA256");
            var keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            var bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(bytes.length * 2);
            for (var b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        LOGGER.warn("Payload signing rejected: {}", message);
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("""
            {"status":401,"error":"Unauthorized","message":"%s"}
            """.stripIndent().strip().formatted(message));
    }
}
