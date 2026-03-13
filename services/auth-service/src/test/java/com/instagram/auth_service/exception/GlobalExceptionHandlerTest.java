package com.instagram.auth_service.exception;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BadCredentialsException vraca 401")
    void handleBadCredentials_ShouldReturn401() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadCredentials(new BadCredentialsException("Bad creds"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    @DisplayName("IllegalArgumentException vraca 400")
    void handleIllegalArgument_ShouldReturn400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Pogresan argument"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Pogresan argument", response.getBody().get("error"));
    }

    @Test
    @DisplayName("IllegalStateException vraca 409")
    void handleIllegalState_ShouldReturn409() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalState(new IllegalStateException("Vec postoji"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Vec postoji", response.getBody().get("error"));
    }

    @Test
    @DisplayName("RuntimeException sa 'nije pronadjen' vraca 404")
    void handleRuntime_NotFound_ShouldReturn404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Корисник није пронађен."));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("RuntimeException sa 'nije moguca' vraca 503")
    void handleRuntime_ServiceUnavailable_ShouldReturn503() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Регистрација тренутно није могућа."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("RuntimeException sa 'Pokusajte ponovo' vraca 503")
    void handleRuntime_RetryMessage_ShouldReturn503() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Покушајте поново."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("RuntimeException genericki vraca 500")
    void handleRuntime_GenericError_ShouldReturn500() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Neka nepoznata greska"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("Opsti Exception vraca 500")
    void handleGeneral_ShouldReturn500() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGeneral(new Exception("Nesto je puklo"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("серверу"));
    }
}