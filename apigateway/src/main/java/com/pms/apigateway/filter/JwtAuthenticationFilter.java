package com.pms.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.exception.ErrorResponse;
import com.pms.apigateway.security.JwtUtil;

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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
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

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                writeUnauthorizedResponse(response, request, "Token validation failed");
                return;
            }

            String email = jwtUtil.extractUsername(token);
            String role  = jwtUtil.extractRole(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            MutableHttpServletRequest wrappedRequest =
                    new MutableHttpServletRequest(request);

            wrappedRequest.putHeader("X-Authenticated-User", email);
            wrappedRequest.putHeader("X-Authenticated-Role", role);

            log.info(
                    "Authenticated User: {} | Role: {}",
                    email,
                    role
            );

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {

            log.error(
                    "JWT validation failed for path {}: {}",
                    request.getRequestURI(),
                    e.getMessage()
            );

            // FIX: Clear any partial authentication that may have been set
            SecurityContextHolder.clearContext();

            writeUnauthorizedResponse(response, request, "Invalid or expired JWT");
        }
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error =
                ErrorResponse.builder()
                        .status(401)
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .path(request.getRequestURI())
                        .build();

        response.getWriter().write(
                objectMapper.writeValueAsString(error)
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.equals("/health")
                || path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/health");
    }
}