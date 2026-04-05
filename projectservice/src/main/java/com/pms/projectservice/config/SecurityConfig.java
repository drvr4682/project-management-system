package com.pms.projectservice.config;

import com.pms.projectservice.security.JwtAuthenticationFilter;
import com.pms.projectservice.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Profile("!test")
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ❌ Disable default login
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // ❌ Disable CSRF (JWT system)
            .csrf(csrf -> csrf.disable())

            // ❌ No sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ✅ Authorization rules (basic for now)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/projects/health").permitAll()
                .anyRequest().authenticated()
            )

            // ✅ Add JWT filter BEFORE Spring security
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}