package com.pms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private Key key;

    public JwtUtil() {}

    public JwtUtil(String secret, long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Token generation
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Claim extraction

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // Validation
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JwtUtil] Token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JwtUtil] Malformed token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("[JwtUtil] Invalid signature: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JwtUtil] Unsupported JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JwtUtil] Empty or null token: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[JwtUtil] Token validation failed: {}", e.getMessage());
        }
        return false;
    }
}
