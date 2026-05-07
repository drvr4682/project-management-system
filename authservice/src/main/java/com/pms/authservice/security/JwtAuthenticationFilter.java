package com.pms.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.exception.ErrorResponse;

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
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(jwt);
            String role = jwtUtil.extractRole(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user: {}, role: {}", username, role);
            }

        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");

            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .message("Invalid or expired JWT")
                    .timestamp(System.currentTimeMillis())
                    .path(HtmlUtils.htmlEscape(request.getRequestURI()))
                    .build();

            String body = objectMapper.writeValueAsString(error);
            PrintWriter writer = response.getWriter();
            writer.write(body);
            writer.flush();

            return;
        }

        filterChain.doFilter(request, response);
    }
}