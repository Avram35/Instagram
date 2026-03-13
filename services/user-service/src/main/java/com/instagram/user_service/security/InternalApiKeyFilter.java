package com.instagram.user_service.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/internal/")) {
            String providedKey = request.getHeader(API_KEY_HEADER);

            if (providedKey == null || !providedKey.equals(internalApiKey)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Приступ одбијен!\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}