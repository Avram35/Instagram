package com.instagram.interactive_service.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Штити интерне endpoint-е:
 * - DELETE /api/v1/like/internal/post/{postId}
 * - DELETE /api/v1/comment/internal/post/{postId}
 */
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

        if (uri.contains("/internal/")) {
            String providedKey = request.getHeader(API_KEY_HEADER);

            if (providedKey == null || !providedKey.equals(internalApiKey)) {
                log.warn("Одбијен интерни позив без кључа: {} {}", request.getMethod(), uri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Приступ одбијен.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}