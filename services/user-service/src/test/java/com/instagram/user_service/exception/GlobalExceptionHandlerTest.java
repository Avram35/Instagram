package com.instagram.user_service.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException vraca 400")
    void handleIllegalArgument_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Neispravni podaci");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Neispravni podaci", response.getBody().get("error"));
    }

    @Test
    @DisplayName("IllegalStateException vraca 409")
    void handleIllegalState_shouldReturn409() {
        IllegalStateException ex = new IllegalStateException("Conflict");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().get("error"));
    }

    @Test
    @DisplayName("RuntimeException sa nije pronađen vraca 404")
    void handleRuntime_shouldReturn404_whenMessageContainsNijePronađen() {
        RuntimeException ex = new RuntimeException("Корисник није пронађен.");

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("није пронађен"));
    }

    @Test
    @DisplayName("Genericki RuntimeException vraca 500")
    void handleRuntime_shouldReturn500_whenGenericError() {
        RuntimeException ex = new RuntimeException("Neka greska");

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }

    @Test
    @DisplayName("Genericki Exception vraca 500")
    void handleGeneral_shouldReturn500() {
        Exception ex = new Exception("Server error");

        ResponseEntity<Map<String, String>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException vraca 413")
    void handleMaxUploadSize_shouldReturn413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5 * 1024 * 1024);

        ResponseEntity<Map<String, String>> response = handler.handleMaxUploadSize(ex);

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }
}