package com.pms.projectservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.projectservice.exception.ErrorResponse;
import com.pms.projectservice.security.GatewayAuthenticationFilter;
import com.pms.projectservice.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("!test")
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, ex2) -> {
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
                
                .accessDeniedHandler((req, res, ex2) -> {
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
                .requestMatchers("/api/v1/projects/health").permitAll()
                .requestMatchers("/api/v1/projects/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )

            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, GatewayAuthenticationFilter.class);
            
        return http.build();
    }
}