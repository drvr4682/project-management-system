package com.pms.taskservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.taskservice.exception.ErrorResponse;
import com.pms.taskservice.security.GatewayAuthenticationFilter;

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
import org.springframework.web.util.HtmlUtils;

import java.io.PrintWriter;

@Configuration
@Profile("!test")
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/tasks/health", "/actuator/**")
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_UNAUTHORIZED)
                            .message("Unauthorized")
                            .timestamp(System.currentTimeMillis())
                            .path(HtmlUtils.htmlEscape(req.getRequestURI()))
                            .build();
                    String body = objectMapper.writeValueAsString(error);
                    PrintWriter writer = res.getWriter();
                    writer.write(body);
                    writer.flush();
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    ErrorResponse error = ErrorResponse.builder()
                            .status(HttpServletResponse.SC_FORBIDDEN)
                            .message("Access Denied")
                            .timestamp(System.currentTimeMillis())
                            .path(HtmlUtils.htmlEscape(req.getRequestURI()))
                            .build();
                    String body = objectMapper.writeValueAsString(error);
                    PrintWriter writer = res.getWriter();
                    writer.write(body);
                    writer.flush();
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/tasks/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}