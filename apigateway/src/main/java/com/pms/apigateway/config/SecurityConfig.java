package com.pms.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.filter.CorrelationIdFilter;
import com.pms.common.exception.ErrorResponse;
import com.pms.common.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final CorrelationIdFilter correlationIdFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/health",
                            "/test/public",
                            "/api/v1/auth/login",
                            "/api/v1/auth/register",
                            "/api/v1/auth/health",
                            "/api/v1/projects/health",
                            "/api/v1/tasks/health",
                            "/actuator/health",
                            "/actuator/info"
                    ).permitAll()
                    .requestMatchers("/test/admin").hasRole("ADMIN")
                    .requestMatchers("/test/user").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers(
                            "/api/v1/projects/**",
                            "/api/v1/tasks/**"
                    ).hasAnyRole("USER", "ADMIN")
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, authEx) -> {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json");

                        ErrorResponse error = ErrorResponse.builder()
                                .status(401)
                                .message("Unauthorized: " + authEx.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .path(req.getRequestURI())
                                .build();

                        res.getWriter().write(objectMapper.writeValueAsString(error));
                    })
                    .accessDeniedHandler((req, res, accessEx) -> {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json");

                        ErrorResponse error = ErrorResponse.builder()
                                .status(403)
                                .message("Access Denied: " + accessEx.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .path(req.getRequestURI())
                                .build();

                        res.getWriter().write(objectMapper.writeValueAsString(error));
                    })
            )
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, CorrelationIdFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Gateway does not load users from a store. JWT is validated directly."
            );
        };
    }
}