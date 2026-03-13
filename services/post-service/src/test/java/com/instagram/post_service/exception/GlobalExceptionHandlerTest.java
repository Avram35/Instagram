package com.instagram.post_service.exception;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException vraca 400")
    void handleIllegalArgument_shouldReturn400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Погрешан унос."));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Погрешан унос.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("RuntimeException vraca 404")
    void handleRuntime_shouldReturn404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntime(new RuntimeException("Није пронађено."));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Није пронађено.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException vraca 413")
    void handleMaxUpload_shouldReturn413() {
        ResponseEntity<Map<String, String>> response =
                handler.handleMaxUploadSize(new MaxUploadSizeExceededException(50 * 1024 * 1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }

    @Test
    @DisplayName("Genericki Exception vraca 500")
    void handleGeneral_shouldReturn500() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGeneral(new Exception("Sistemska greška."));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }
}