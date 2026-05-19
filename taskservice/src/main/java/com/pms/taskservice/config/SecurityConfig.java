package com.pms.taskservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.taskservice.exception.ErrorResponse;
import com.pms.taskservice.filter.CorrelationContextFilter;
import com.pms.taskservice.filter.GatewayValidationFilter;
import com.pms.taskservice.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final CorrelationContextFilter correlationContextFilter;
    private final GatewayValidationFilter gatewayValidationFilter;

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

                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_UNAUTHORIZED)
                            .message("Unauthorized")
                            .timestamp(System.currentTimeMillis())
                            .path(req.getRequestURI())
                            .build();

                    res.getWriter().write(objectMapper.writeValueAsString(error));
                })
                .accessDeniedHandler((req, res, accessEx) -> {
                    log.warn("Access denied for endpoint: {}", req.getRequestURI());

                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");

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
                        "/health",
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()
                .requestMatchers("/api/v1/tasks/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().denyAll()
            )

            .addFilterBefore(correlationContextFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(gatewayValidationFilter, CorrelationContextFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, GatewayValidationFilter.class);

        return http.build();
    }
}
