package com.instagram.post_service.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));

            // Ovaj handler hvata IllegalArgumentException, koji se moze javiti kada se 
            // prosledi nevalidan argument, kao sto je nepostojeci userId ili postId.
            // Vraca HTTP status 400 Bad Request i poruku o gresci u JSON formatu, 
            // koja sadrzi detalje o tome koji argument je nevalidan.   
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));

            // Ovaj handler hvata RuntimeException, koji se moze javiti kada se pokusa pristupiti resursu koji ne postoji,
            // kao sto je pokusaj azuriranja ili brisanja objave koja ne postoji.
            // Vraca HTTP status 404 Not Found i poruku o gresci u JSON formatu, koja sadrzi detalje o tome koji resurs nije pronadjen.
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity
            .status(HttpStatus.CONTENT_TOO_LARGE)
            .body(Map.of("error", "Фајл прелази максималну дозвољену величину од 50 MB."));
    }
    // Ovaj handler hvata MaxUploadSizeExceededException, koji se moze javiti kada se pokusa uploadovati fajl koji prelazi maksimalnu dozvoljenu velicinu od 50 MB.
    // Vraca HTTP status 413 Content Too Large i poruku o gresci u JSON formatu, koja sadrzi informaciju da fajl prelazi dozvoljenu velicinu.


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Дошло је до грешке: " + ex.getMessage()));
    }
    // Ovaj handler hvata sve ostale nepredvidjene izuzetke koji nisu obuhvaceni prethodnim handlerima.
    // Vraca HTTP status 500 Internal Server Error i poruku o gresci u JSON formatu, koja sadrzi genericku poruku o gresci sa detaljima izuzetka.

}

// Ovaj globalni exception handler se koristi za hvatanje i obradu razlicitih izuzetaka 
// koji mogu nastati tokom rada aplikacije