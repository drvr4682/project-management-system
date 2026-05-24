package com.pms.projectservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.exception.ErrorResponse;
import com.pms.common.filter.GatewayValidationFilter;
import com.pms.common.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
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
                    log.warn("Access denied for endpoint: {}", req.getRequestURI());
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
                        "/api/v1/projects/health",
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()
                .requestMatchers("/api/v1/projects/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().denyAll()
            );

        http.addFilterBefore(gatewayValidationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, GatewayValidationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Project service does not load users from a store."
            );
        };
    }
}