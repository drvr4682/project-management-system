package com.pms.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.exception.ErrorResponse;
import com.pms.authservice.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.util.HtmlUtils;

import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/health"
                )
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/health"
                ).permitAll()

                // Internal service-to-service endpoint
                .requestMatchers("/api/v1/auth/users/**").hasAnyRole("ADMIN", "MANAGER", "USER")

                // Role-based endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/management/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/v1/user/**").hasAnyRole("ADMIN", "MANAGER", "USER")

                // Everything else must be authenticated
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");

                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_UNAUTHORIZED)
                            .message("Unauthorized")
                            .timestamp(System.currentTimeMillis())
                            .path(HtmlUtils.htmlEscape(request.getRequestURI()))
                            .build();

                    String body = objectMapper.writeValueAsString(error);
                    PrintWriter writer = response.getWriter();
                    writer.write(body);
                    writer.flush();
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");

                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_FORBIDDEN)
                            .message("Access Denied")
                            .timestamp(System.currentTimeMillis())
                            .path(HtmlUtils.htmlEscape(request.getRequestURI()))
                            .build();

                    String body = objectMapper.writeValueAsString(error);
                    PrintWriter writer = response.getWriter();
                    writer.write(body);
                    writer.flush();
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}