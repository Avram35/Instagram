package com.instagram.feed_service.exception;

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
    @DisplayName("RuntimeException vraca 404 sa porukom")
    void handleRuntime_shouldReturn404() {
        RuntimeException ex = new RuntimeException("Корисник није пронађен.");

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Корисник није пронађен.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("Genericki Exception vraca 500")
    void handleGeneral_shouldReturn500() {
        Exception ex = new Exception("neka greška");

        ResponseEntity<Map<String, String>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }
}