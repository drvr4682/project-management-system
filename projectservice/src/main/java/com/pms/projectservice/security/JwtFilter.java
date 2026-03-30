package com.pms.projectservice.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Profile("!test")
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String username = jwtUtil.extractUsername(token);
                currentUser.set(username);
            } catch (Exception e) {
                throw new RuntimeException("Invalid JWT");
            }
        }

        chain.doFilter(request, response);
        currentUser.remove();
    }
}