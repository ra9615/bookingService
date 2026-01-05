package com.example.booking.security;

import com.example.booking.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        extractToken(request)
                .map(jwtTokenService::parseClaims)
                .ifPresent(this::authenticate);

        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(authHeader.substring(7));
    }

    private void authenticate(Claims claims) {
        String login = claims.getSubject();
        String role = claims.get("role", String.class);

        if (login == null || role == null) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        login,
                        null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        )
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
