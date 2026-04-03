package com.pms.projectservice;

import com.pms.projectservice.security.JwtFilter;
import com.pms.projectservice.security.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public FilterRegistrationBean<JwtFilter> jwtFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtFilter(jwtUtil));
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}