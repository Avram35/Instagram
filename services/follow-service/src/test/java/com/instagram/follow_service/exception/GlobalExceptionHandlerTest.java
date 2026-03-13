package com.instagram.follow_service.exception;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException vraca 400")
    void handleIllegalArgument_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Не можете пратити сами себе.");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Не можете пратити сами себе.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("IllegalStateException vraca 409")
    void handleIllegalState_shouldReturn409() {
        IllegalStateException ex = new IllegalStateException("Већ пратите овог корисника.");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Већ пратите овог корисника.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("RuntimeException vraca 404")
    void handleRuntime_shouldReturn404() {
        RuntimeException ex = new RuntimeException("Захтев није пронађен.");

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Захтев није пронађен.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("Genericki Exception vraca 500")
    void handleGeneral_shouldReturn500() {
        Exception ex = new Exception("Server error");

        ResponseEntity<Map<String, String>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }
}