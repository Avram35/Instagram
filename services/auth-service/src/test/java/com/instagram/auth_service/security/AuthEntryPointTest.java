package com.instagram.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class AuthEntryPointTest {

    private final AuthEntryPoint authEntryPoint = new AuthEntryPoint();

    @Test
    @DisplayName("Neautorizovan pristup vraca 401 sa JSON porukom")
    void commence_ShouldReturn401WithJsonMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authEntryPoint.commence(request, response,
                new BadCredentialsException("Unauthorized"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertTrue(response.getContentAsString().contains("Нисте Пријављени"));
    }

    @Test
    @DisplayName("Razliciti AuthenticationException-i uvek vraca 401")
    void commence_DifferentExceptions_ShouldAlwaysReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authEntryPoint.commence(request, response,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Full auth required"));

        assertEquals(401, response.getStatus());
    }
}