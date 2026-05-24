package com.pms.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.security.InternalServiceFilter;
import com.pms.common.exception.ErrorResponse;
import com.pms.common.filter.GatewayValidationFilter;
import com.pms.common.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayValidationFilter gatewayValidationFilter;
    private final InternalServiceFilter   internalServiceFilter;
    private final ObjectMapper            objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_UNAUTHORIZED)
                            .message("Unauthorized")
                            .timestamp(System.currentTimeMillis())
                            .path(req.getRequestURI())
                            .build();
                    res.getWriter().write(objectMapper.writeValueAsString(error));
                })
                .accessDeniedHandler((req, res, accessEx) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_FORBIDDEN)
                            .message("Access Denied")
                            .timestamp(System.currentTimeMillis())
                            .path(req.getRequestURI())
                            .build();
                    res.getWriter().write(objectMapper.writeValueAsString(error));
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/health"
                ).permitAll()
                .requestMatchers("/internal/**").permitAll()
                .anyRequest().authenticated()
            );

        /*
         * FIX: Correct filter order — GatewayValidationFilter must run first so that
         * direct callers without the gateway secret are rejected before JWT parsing.
         *
         * Previously both filters used addFilterBefore(UsernamePasswordAuthenticationFilter)
         * which made registration order non-deterministic; the last-registered one ran
         * first, meaning JWT was validated before the gateway secret check.
         *
         * Correct chain:  GatewayValidationFilter → JwtAuthenticationFilter → ...
         */
        http.addFilterBefore(gatewayValidationFilter,   UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter,   GatewayValidationFilter.class);
        http.addFilterBefore(internalServiceFilter,     GatewayValidationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}