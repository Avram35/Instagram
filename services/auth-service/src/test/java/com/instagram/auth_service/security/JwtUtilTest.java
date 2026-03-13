package com.instagram.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "testSecretKeyThatIsAtLeast32CharactersLongForHS256Algorithm");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3600000L);
        jwtUtil.init();
    }

    @Test
    @DisplayName("Generisanje tokena validan token se kreira")
    void generateToken_ShouldReturnValidToken() {
        String token = jwtUtil.generateToken("avram");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Get username iz tokena")
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("milan123");

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals("milan123", username);
    }

    @Test
    @DisplayName("Validacija ispravnog tokena vraca true")
    void validateJwtToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtUtil.generateToken("avram");

        assertTrue(jwtUtil.validateJwtToken(token));
    }

    @Test
    @DisplayName("Validacija neispravnog tokena vraca false")
    void validateJwtToken_WithInvalidToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateJwtToken("ovo.nije.validan.token"));
    }

    @Test
    @DisplayName("Validacija praznog tokena vraca false")
    void validateJwtToken_WithEmptyToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateJwtToken(""));
    }

    @Test
    @DisplayName("Validacija null tokena vraca false")
    void validateJwtToken_WithNullToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateJwtToken(null));
    }

    @Test
    @DisplayName("Istekli token vraca false")
    void validateJwtToken_WithExpiredToken_ShouldReturnFalse() {
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "jwtSecret",
                "testSecretKeyThatIsAtLeast32CharactersLongForHS256Algorithm");
        ReflectionTestUtils.setField(expiredJwtUtil, "expirationTime", 0L);
        expiredJwtUtil.init();

        String token = expiredJwtUtil.generateToken("testuser");

        assertFalse(expiredJwtUtil.validateJwtToken(token));
    }

    @Test
    @DisplayName("Razliciti korisnici dobijaju razlicite tokene")
    void generateToken_DifferentUsers_ShouldReturnDifferentTokens() {
        String token1 = jwtUtil.generateToken("milan");
        String token2 = jwtUtil.generateToken("avram");

        assertNotEquals(token1, token2);
    }

}