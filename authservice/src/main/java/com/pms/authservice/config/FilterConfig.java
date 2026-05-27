package com.pms.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.security.InternalServiceFilter;
import com.pms.common.filter.CorrelationContextFilter;
import com.pms.common.filter.GatewayValidationFilter;
import com.pms.common.security.JwtAuthenticationFilter;
import com.pms.common.security.JwtUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

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
            JwtUtil jwtUtil,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {
        return new JwtAuthenticationFilter(jwtUtil, objectMapper, redisTemplate);
    }

    @Bean
    public InternalServiceFilter internalServiceFilter(
            @Value("${internal.secret}") String internalSecret,
            ObjectMapper objectMapper) {
        return new InternalServiceFilter(internalSecret, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<CorrelationContextFilter> correlationFilterRegistration(
            CorrelationContextFilter filter) {
        FilterRegistrationBean<CorrelationContextFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<GatewayValidationFilter> gatewayFilterRegistration(
            GatewayValidationFilter filter) {
        FilterRegistrationBean<GatewayValidationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<InternalServiceFilter> internalFilterRegistration(
            InternalServiceFilter filter) {
        FilterRegistrationBean<InternalServiceFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}