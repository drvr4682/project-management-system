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

    /** Default constructor — used by Spring. */
    public JwtUtil() {}

    /**
     * Test constructor — bypasses Spring property injection.
     *
     * @param secret     HMAC-SHA256 secret (minimum 32 characters)
     * @param expiration token lifetime in milliseconds
     */
    public JwtUtil(String secret, long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a signed JWT containing the given subject (email) and role claim.
     *
     * @param email user's email address — stored as the JWT subject
     * @param role  user's role (e.g. "USER" or "ADMIN") — stored as the "role" claim
     * @return compact, URL-safe JWT string
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Claim extraction
    // -------------------------------------------------------------------------

    /**
     * Returns all claims from the token.
     * Throws a runtime exception (wrapped or JJWT-native) on any parse failure.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Returns the subject claim (typically the user's email). */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Returns the "role" claim.
     * Returns {@code null} if the claim is absent rather than throwing.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates the token signature and expiry.
     *
     * @return {@code true} if the token is well-formed, correctly signed, and
     *         not expired; {@code false} otherwise
     */
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
