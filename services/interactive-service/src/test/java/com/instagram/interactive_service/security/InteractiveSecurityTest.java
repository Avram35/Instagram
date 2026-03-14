package com.instagram.interactive_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InteractiveSecurityTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ─── AuthEntryPoint ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("AuthEntryPoint vraca 401 sa JSON porukom")
    void authEntryPoint_shouldReturn401() throws Exception {
        AuthEntryPoint ep = new AuthEntryPoint();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ep.commence(new MockHttpServletRequest(), response, mock(AuthenticationException.class));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("error"));
    }

    // ─── InternalApiKeyFilter ────────────────────────────────────────────────────

    @Test
    @DisplayName("Interni endpoint sa ispravnim kljucem prolazi")
    void internalFilter_validKey_shouldPass() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/like/internal/post/10");
        request.addHeader("X-Internal-Api-Key", "test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Interni endpoint bez kljuca vraca 403")
    void internalFilter_noKey_shouldReturn403() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/comment/internal/post/10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("error"));
    }

    @Test
    @DisplayName("Interni endpoint sa pogresnim kljucem vraca 403")
    void internalFilter_wrongKey_shouldReturn403() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/like/internal/user/1");
        request.addHeader("X-Internal-Api-Key", "pogresni");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("Javni endpoint prolazi bez kljuca")
    void internalFilter_publicEndpoint_shouldPass() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/like/10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    // ─── AuthTokenFilter ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Validan token postavlja autentifikaciju")
    void authTokenFilter_validToken_shouldSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.validateJwtToken("valid")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("valid")).thenReturn("ana");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ana", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Neispravan token ne postavlja autentifikaciju")
    void authTokenFilter_invalidToken_shouldNotSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.validateJwtToken("bad")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Nema Authorization headera — auth nije postavljena")
    void authTokenFilter_noHeader_shouldNotSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Basic auth header se ignorise")
    void authTokenFilter_basicHeader_shouldIgnore() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ─── JwtUtil ─────────────────────────────────────────────────────────────────

    private JwtUtil buildJwtUtil() {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "jwtSecret",
                "testSecretKeyKojaJeDovoljnoDugaZaHMACSHA256algoritam123456");
        jwt.init();
        return jwt;
    }

    @Test
    @DisplayName("JwtUtil — neispravan token vraca false")
    void jwtUtil_invalid_shouldReturnFalse() {
        assertFalse(buildJwtUtil().validateJwtToken("neispravan.token"));
    }

    @Test
    @DisplayName("JwtUtil — prazan string vraca false")
    void jwtUtil_empty_shouldReturnFalse() {
        assertFalse(buildJwtUtil().validateJwtToken(""));
    }

    @Test
    @DisplayName("JwtUtil — null vraca false")
    void jwtUtil_null_shouldReturnFalse() {
        assertFalse(buildJwtUtil().validateJwtToken(null));
    }
}