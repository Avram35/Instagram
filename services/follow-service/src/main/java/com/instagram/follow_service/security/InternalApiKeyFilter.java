package com.instagram.follow_service.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

//Филтер за заштиту интерних endpoint-а follow-service-а
//Проверава X-Internal-Api-Key header. Само сервиси са исправним кључем могу да приступе

@Slf4j
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

        String uri = request.getRequestURI();
        boolean isInternalEndpoint = 
             uri.contains("/internal/") || 
            uri.contains("/accept-all/") ||
            uri.contains("/check-internal/");

        if (isInternalEndpoint) {
            String providedKey = request.getHeader(API_KEY_HEADER);

            if (providedKey == null || !providedKey.equals(internalApiKey)) {
                log.warn("Одбијен интерни позив без валидног кључа: {} {}", request.getMethod(), uri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Приступ одбијен.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}