package com.pms.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.exception.ErrorResponse;
import com.pms.authservice.filter.CorrelationContextFilter;
import com.pms.authservice.filter.GatewayValidationFilter;
import com.pms.authservice.security.InternalServiceFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final InternalServiceFilter internalServiceFilter;
    private final CorrelationContextFilter correlationContextFilter;
    private final GatewayValidationFilter gatewayValidationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/v1/auth/health",
                            "/api/v1/auth/register",
                            "/api/v1/auth/login"

                    ).permitAll()
                    .requestMatchers("/internal/**").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/user/**").hasAnyRole("ADMIN", "USER")
                    
                    .requestMatchers("/api/v1/test/admin").hasRole("ADMIN")
                    .requestMatchers("/api/v1/test/user").hasAnyRole("ADMIN", "USER")
                    .anyRequest().denyAll()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");

                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_UNAUTHORIZED)
                            .message("Unauthorized")
                            .timestamp(System.currentTimeMillis())
                            .path(request.getRequestURI())
                            .build();

                    response.getWriter().write(objectMapper.writeValueAsString(error));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");

                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_FORBIDDEN)
                            .message("Access Denied")
                            .timestamp(System.currentTimeMillis())
                            .path(request.getRequestURI())
                            .build();

                    response.getWriter().write(objectMapper.writeValueAsString(error));
                })
            );

        http.addFilterBefore(correlationContextFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(gatewayValidationFilter, CorrelationContextFilter.class);
        http.addFilterAfter(internalServiceFilter, GatewayValidationFilter.class);
        http.addFilterAfter(jwtAuthenticationFilter, InternalServiceFilter.class);

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