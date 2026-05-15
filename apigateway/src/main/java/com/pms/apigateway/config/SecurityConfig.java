package com.pms.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.exception.ErrorResponse;
import com.pms.apigateway.filter.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable())

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .authorizeHttpRequests(auth -> auth

                    .requestMatchers(
                            "/health",
                            "/api/v1/auth/login",
                            "/api/v1/auth/register",
                            "/api/v1/auth/health"
                    ).permitAll()

                    .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex

                    .authenticationEntryPoint((req, res, ex2) -> {

                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json");

                        ErrorResponse error =
                                ErrorResponse.builder()
                                        .status(401)
                                        .message("Unauthorized")
                                        .timestamp(System.currentTimeMillis())
                                        .path(req.getRequestURI())
                                        .build();

                        res.getWriter().write(
                                objectMapper.writeValueAsString(error)
                        );
                    })
            )

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}