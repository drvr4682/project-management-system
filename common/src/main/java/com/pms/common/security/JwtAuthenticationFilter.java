package com.pms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                writeUnauthorized(response, request, "Token validation failed");
                return;
            }

            String email = jwtUtil.extractUsername(token);
            String role  = jwtUtil.extractRole(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                token,   // credential = raw token, propagated by FeignConfig
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {} | role: {}", email, role);
            }

        } catch (Exception e) {
            log.error("JWT validation failed for path {}: {}",
                    request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, request, "Invalid or expired JWT");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/health")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void writeUnauthorized(
            HttpServletResponse response,
            HttpServletRequest request,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
