package br.com.fiap.fordchallengebackend.security;

import br.com.fiap.fordchallengebackend.auth.service.JwtService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(0)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final int UNAUTH_CAPACITY = 10;
    private static final int AUTH_CAPACITY = 30;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final JwtService jwtService;

    public RateLimitingFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var key = resolveKey(request);
        var bucket = buckets.computeIfAbsent(key, k -> createBucket(key, request));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            LOGGER.warn("Rate limit exceeded for key: {}", key);
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "1");
            response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Muitas requisicoes. Tente novamente em alguns segundos."}
                """.stripIndent().strip());
        }
    }

    private String resolveKey(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                var token = authHeader.substring(7);
                var email = jwtService.extractEmail(token);
                if (email != null && !email.isBlank()) {
                    return "user:" + email;
                }
            } catch (Exception e) {
                // Token invalido - fallback para IP
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private Bucket createBucket(String key, HttpServletRequest request) {
        var capacity = key.startsWith("user:") ? AUTH_CAPACITY : UNAUTH_CAPACITY;
        var limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
