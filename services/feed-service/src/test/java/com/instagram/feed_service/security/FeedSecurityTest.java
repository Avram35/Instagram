package com.instagram.feed_service.security;

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
class FeedSecurityTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    //----------------- AuthEntryPoint ---------------------

    @Test
    @DisplayName("AuthEntryPoint vraca 401 sa JSON porukom")
    void authEntryPoint_shouldReturn401() throws Exception {
        AuthEntryPoint entryPoint = new AuthEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, mock(AuthenticationException.class));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("error"));
    }

    // ---------------- AuthTokenFilter ---------------------

    @Test
    @DisplayName("Validan token postavlja autentifikaciju u SecurityContext")
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
    @DisplayName("Zahtev bez Authorization headera prolazi filter bez autentifikacije")
    void authTokenFilter_noHeader_shouldNotSetAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest()); // filter chain nastavljen
    }

    @Test
    @DisplayName("Basic auth header se ignorise")
    void authTokenFilter_basicAuthHeader_shouldIgnore() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ---------------- JwtUtil ---------------------

    private JwtUtil buildJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "testSecretKeyKojaJeDovoljnoDugaZaHMACSHA256algoritam123456");
        jwtUtil.init();
        return jwtUtil;
    }

    @Test
    @DisplayName("JwtUtil — neispravan token vraca false")
    void jwtUtil_invalidToken_shouldReturnFalse() {
        assertFalse(buildJwtUtil().validateJwtToken("ovo.nije.validan.token"));
    }

    @Test
    @DisplayName("JwtUtil — prazan string vraca false")
    void jwtUtil_emptyToken_shouldReturnFalse() {
        assertFalse(buildJwtUtil().validateJwtToken(""));
    }

    @Test
    @DisplayName("JwtUtil — null vraca false")
    void jwtUtil_null_shouldReturnFalse() {
        assertFalse(buildJwtUtil().validateJwtToken(null));
    }
}