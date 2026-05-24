package br.com.fiap.fordchallengebackend.auth.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import br.com.fiap.fordchallengebackend.auth.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecurityProperties securityProperties;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String generateAccessToken(UserDetails user) {
        return generateToken(user, securityProperties.jwtExpirationMs());
    }

    public String generateRefreshToken(UserDetails user) {
        return generateToken(user, securityProperties.jwtRefreshExpirationMs());
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails user) {
        try {
            var email = extractEmail(token);
            return email.equals(user.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String refreshAccessToken(String refreshToken) {
        try {
            var email = extractEmail(refreshToken);
            if (email == null || isTokenExpired(refreshToken)) {
                return null;
            }
            var user = new org.springframework.security.core.userdetails.User(
                email, "", new java.util.ArrayList<>());
            return generateAccessToken(user);
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpirationMs() {
        return securityProperties.jwtExpirationMs();
    }

    SecretKey getSigningKey() {
        var keyBytes = Decoders.BASE64.decode(securityProperties.jwtSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateToken(UserDetails user, long expirationMs) {
        var now = new Date();
        var expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(user.getUsername())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey())
            .compact();
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        var claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
