package com.instagram.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        ReflectionTestUtils.setField(rateLimitFilter, "signinMaxAttempts", 5);
        ReflectionTestUtils.setField(rateLimitFilter, "signinWindowMinutes", 15);
        ReflectionTestUtils.setField(rateLimitFilter, "signupMaxAttempts", 3);
        ReflectionTestUtils.setField(rateLimitFilter, "signupWindowMinutes", 60);
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("GET zahtev propusta bez provere")
    void doFilter_GetRequest_ShouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/signin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("POST signin unutar limita propusta")
    void doFilter_SigninWithinLimit_ShouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signin");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("POST signin prekoracen limit vraca 429")
    void doFilter_SigninExceedLimit_ShouldReturn429() throws Exception {
        for (int i = 0; i < 5; i++) 
        {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/signin");
            req.setRemoteAddr("10.0.0.99");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, res, filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signin");
        request.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Превише покушаја"));
    }

    @Test
    @DisplayName("POST signup prekoracen limit vraca 429")
    void doFilter_SignupExceedLimit_ShouldReturn429() throws Exception {
        for (int i = 0; i < 3; i++) 
        {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
            req.setRemoteAddr("10.0.0.50");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, res, filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
        request.setRemoteAddr("10.0.0.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
    }

    @Test
    @DisplayName("Razlicite IP adrese nezavisni limiti")
    void doFilter_DifferentIps_ShouldHaveIndependentLimits() throws Exception {
        for (int i = 0; i < 5; i++) 
        {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/signin");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, res, filterChain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signin");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

}