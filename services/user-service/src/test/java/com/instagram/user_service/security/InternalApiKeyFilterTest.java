package com.instagram.user_service.security;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternalApiKeyFilterTest {

    @InjectMocks
    private InternalApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-secret-key");
    }

    @Test
    @DisplayName("Interni endpoint sa ispravnim kljucem prolazi")
    void doFilterInternal_shouldPassThrough_whenValidApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/api/v1/user");
        request.addHeader("X-Internal-Api-Key", "test-secret-key");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Interni endpoint bez kljuca vraca 403")
    void doFilterInternal_shouldReturn403_whenNoApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/api/v1/user");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Interni endpoint sa pogresnim kljucem vraca 403")
    void doFilterInternal_shouldReturn403_whenWrongApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/api/v1/user");
        request.addHeader("X-Internal-Api-Key", "pogresni-kljuc");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Javni endpoint prolazi bez kljuca")
    void doFilterInternal_shouldPassThrough_whenPublicEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user/testuser");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("403 response sadrzi error poruku")
    void doFilterInternal_shouldReturnErrorMessage_whenForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/api/v1/user");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertTrue(response.getContentAsString().contains("error"));
    }
}