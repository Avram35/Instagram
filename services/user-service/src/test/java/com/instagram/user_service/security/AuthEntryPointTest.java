package com.instagram.user_service.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthEntryPointTest {

    private AuthEntryPoint authEntryPoint;

    @BeforeEach
    void setUp() {
        authEntryPoint = new AuthEntryPoint();
    }

    @Test
    @DisplayName("Vraca 401 status")
    void commence_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = mock(AuthenticationException.class);

        authEntryPoint.commence(request, response, authException);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    @DisplayName("Vraca JSON content type")
    void commence_shouldReturnJsonContentType() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = mock(AuthenticationException.class);

        authEntryPoint.commence(request, response, authException);

        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    @DisplayName("Vraca error poruku u body-u")
    void commence_shouldReturnErrorMessageInBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = mock(AuthenticationException.class);

        authEntryPoint.commence(request, response, authException);

        assertTrue(response.getContentAsString().contains("error"));
    }
}