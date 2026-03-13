package com.instagram.user_service.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "testSecretKeyKojaJeDovoljnoDugaZaHMACSHA256algoritam123456");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3600000L);
        jwtUtil.init();
    }

    @Test
    @DisplayName("Generise validan token")
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("avram");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Izvlaci username iz tokena")
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("avram");

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals("avram", username);
    }

    @Test
    @DisplayName("Validan token prolazi validaciju")
    void validateJwtToken_shouldReturnTrue_whenTokenValid() {
        String token = jwtUtil.generateToken("avram");

        boolean valid = jwtUtil.validateJwtToken(token);

        assertTrue(valid);
    }

    @Test
    @DisplayName("Neispravan token ne prolazi validaciju")
    void validateJwtToken_shouldReturnFalse_whenTokenInvalid() {
        boolean valid = jwtUtil.validateJwtToken("ovo.nije.validan.token");

        assertFalse(valid);
    }

    @Test
    @DisplayName("Istekao token ne prolazi validaciju")
    void validateJwtToken_shouldReturnFalse_whenTokenExpired() {
        JwtUtil expiredUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredUtil, "jwtSecret",
                "testSecretKeyKojaJeDovoljnoDugaZaHMACSHA256algoritam123456");
        ReflectionTestUtils.setField(expiredUtil, "expirationTime", -1000L);
        expiredUtil.init();

        String token = expiredUtil.generateToken("avram");

        assertFalse(jwtUtil.validateJwtToken(token));
    }

    @Test
    @DisplayName("Prazan string ne prolazi validaciju")
    void validateJwtToken_shouldReturnFalse_whenTokenEmpty() {
        boolean valid = jwtUtil.validateJwtToken("");

        assertFalse(valid);
    }

    @Test
    @DisplayName("Razliciti korisnici dobijaju razlicite tokene")
    void generateToken_shouldReturnDifferentTokensForDifferentUsers() {
        String token1 = jwtUtil.generateToken("user1");
        String token2 = jwtUtil.generateToken("user2");

        assertNotEquals(token1, token2);
    }
}