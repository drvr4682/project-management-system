package com.pms.taskservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.filter.CorrelationContextFilter;
import com.pms.common.filter.GatewayValidationFilter;
import com.pms.common.security.JwtAuthenticationFilter;
import com.pms.common.security.JwtUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Bean
    public GatewayValidationFilter gatewayValidationFilter(ObjectMapper objectMapper) {
        return new GatewayValidationFilter(gatewaySecret, objectMapper);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtUtil jwtUtil, ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtUtil, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<CorrelationContextFilter> correlationFilterRegistration(
            CorrelationContextFilter filter) {
        FilterRegistrationBean<CorrelationContextFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<GatewayValidationFilter> gatewayFilterRegistration(
            GatewayValidationFilter filter) {
        FilterRegistrationBean<GatewayValidationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}