package com.orderflow.gateway.security;

import java.util.List;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        var accessCookie = exchange.getRequest()
                .getCookies()
                .getFirst("access_token");

        // No token → let SecurityConfig decide
        if (accessCookie == null ||
                accessCookie.getValue().isBlank()) {
            return chain.filter(exchange);
        }

        String token = accessCookie.getValue();

        // Invalid token
        if (!jwtService.isTokenValid(token)) {
            return chain.filter(exchange);
        }

        Claims claims;

        try {
            claims = jwtService.extractClaims(token);
        } catch (Exception ex) {
            return chain.filter(exchange);
        }

        // Only ACCESS token can authenticate
        if (!"ACCESS".equals(
                claims.get("type", String.class))) {
            return chain.filter(exchange);
        }

        Long userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);

        if (role == null || role.isBlank()) {
            return chain.filter(exchange);
        }

        // Prevent ROLE_ROLE_ADMIN
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        )
                );

        ServerHttpRequest mutatedRequest = exchange
                .getRequest()
                .mutate()
                .header("X-User-Id", userId.toString())
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        ServerWebExchange mutatedExchange = exchange
                .mutate()
                .request(mutatedRequest)
                .build();

        return chain
                .filter(mutatedExchange)
                .contextWrite(
                        ReactiveSecurityContextHolder
                                .withAuthentication(authentication)
                );
    }
}