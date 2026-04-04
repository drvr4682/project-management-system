package com.pms.projectservice.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;

    public static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    public static final ThreadLocal<String> currentRole = new ThreadLocal<>();
    public static final ThreadLocal<String> currentToken = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String header = req.getHeader("Authorization");
        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                String email = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                
                currentUser.set(email);
                currentRole.set(role);
                currentToken.set(token);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid JWT");
        } finally {
            currentUser.remove();
            currentRole.remove();
            currentToken.remove();
            SecurityContextHolder.clearContext();
        }
    }
}