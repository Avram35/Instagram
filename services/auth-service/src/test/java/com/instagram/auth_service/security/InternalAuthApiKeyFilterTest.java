package com.instagram.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;

class InternalAuthApiKeyFilterTest {

    private InternalAuthApiKeyFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalAuthApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "secret-key-123");
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Interni endpoint sa ispravnim kljucem propusta")
    void doFilter_InternalEndpointWithValidKey_ShouldPass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/auth/internal/update-username");
        request.addHeader("X-Internal-Api-Key", "secret-key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Interni endpoint bez kljuca vraca 403")
    void doFilter_InternalEndpointWithoutKey_ShouldReturn403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/auth/internal/update-username");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Приступ одбијен"));
    }

    @Test
    @DisplayName("Interni endpoint sa pogresnim kljucem vraca 403")
    void doFilter_InternalEndpointWithWrongKey_ShouldReturn403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/auth/internal/update-username");
        request.addHeader("X-Internal-Api-Key", "pogresan-kljuc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Obican endpoint bez kljuca propusta normalno")
    void doFilter_NonInternalEndpoint_ShouldPassWithoutKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Signup endpoint ne zahteva kljuc")
    void doFilter_SignupEndpoint_ShouldPassWithoutKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}