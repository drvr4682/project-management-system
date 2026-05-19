package com.pms.taskservice.config;

import com.pms.taskservice.filter.CorrelationContextFilter;
import com.pms.taskservice.filter.GatewayValidationFilter;
import com.pms.taskservice.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

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
