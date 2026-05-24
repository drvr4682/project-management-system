package com.pms.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.filter.CorrelationIdFilter;
import com.pms.common.filter.CorrelationContextFilter;
import com.pms.common.security.JwtAuthenticationFilter;
import com.pms.common.security.JwtUtil;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class FilterConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtUtil jwtUtil,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {
        return new JwtAuthenticationFilter(jwtUtil, objectMapper, redisTemplate);
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
            CorrelationIdFilter filter
    ) {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CorrelationContextFilter> correlationContextFilterRegistration(
            CorrelationContextFilter filter
    ) {
        FilterRegistrationBean<CorrelationContextFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}