package com.instagram.follow_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class FollowSecurityTest {

    // ----------------- AuthEntryPoint -----------------

    private AuthEntryPoint authEntryPoint;

    @BeforeEach
    void setUp() {
        authEntryPoint = new AuthEntryPoint();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AuthEntryPoint vraca 401")
    void authEntryPoint_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = mock(AuthenticationException.class);

        authEntryPoint.commence(request, response, ex);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("error"));
    }

    @Test
    @DisplayName("AuthEntryPoint vraca JSON content type")
    void authEntryPoint_shouldReturnJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = mock(AuthenticationException.class);

        authEntryPoint.commence(request, response, ex);

        assertTrue(response.getContentType().contains("application/json"));
    }

    // ----------------- InternalApiKeyFilter -----------------

    @Test
    @DisplayName("Interni endpoint sa ispravnim kljucem prolazi")
    void internalFilter_validKey_shouldPass() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/follow/internal/unfollow");
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
        request.setRequestURI("/api/v1/follow/internal/unfollow");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("check-internal endpoint bez kljuca vraca 403")
    void internalFilter_checkInternal_noKey_shouldReturn403() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/follow/check-internal/1/2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("accept-all endpoint bez kljuca vraca 403")
    void internalFilter_acceptAll_noKey_shouldReturn403() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/follow/requests/accept-all/1");
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
        request.setRequestURI("/api/v1/follow/1/followers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    // ----------------- AuthTokenFilter --------------------

    @Test
    @DisplayName("Validan token postavlja autentifikaciju")
    void authTokenFilter_validToken_shouldSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("ana");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ana", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Neispravan token ne postavlja autentifikaciju")
    void authTokenFilter_invalidToken_shouldNotSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.validateJwtToken("bad-token")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Zahtev bez headera prolazi bez autentifikacije")
    void authTokenFilter_noHeader_shouldNotSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    // ----------------- JwtUtil -----------------

    @Test
    @DisplayName("JwtUtil - neispravan token vraca false")
    void jwtUtil_invalidToken_shouldReturnFalse() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "testSecretKeyKojiJeDovoljnoDugZaHMACSHA256algoritam1234567");
        jwtUtil.init();

        assertFalse(jwtUtil.validateJwtToken("ovo.nije.validan.token"));
    }

    @Test
    @DisplayName("JwtUtil - prazan string vraca false")
    void jwtUtil_emptyToken_shouldReturnFalse() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "testSecretKeyKojiJeDovoljnoDugZaHMACSHA256algoritam1234567");
        jwtUtil.init();

        assertFalse(jwtUtil.validateJwtToken(""));
    }

    @Test
    @DisplayName("Null vraca false")
    void validateJwtToken_null_shouldReturnFalse() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "testSecretKeyKojiJeDovoljnoDugZaHMACSHA256algoritam1234567");
        jwtUtil.init();

        assertFalse(jwtUtil.validateJwtToken(null));
    }
 
    @Test
    @DisplayName("getUsernameFromToken baca exception za neispravan token")
    void getUsernameFromToken_invalidToken_shouldThrow() {
        JwtUtil jwtUtil = new JwtUtil();

        assertThrows(Exception.class,
                () -> jwtUtil.getUsernameFromToken("invalid.token.here"));
    }

}