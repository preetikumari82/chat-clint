package com.example.chatbot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Har error ek jaisa JSON format me jaayega:
 *   { "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "fields": {...} }
 * Frontend ko ek hi format handle karna padega.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid fail: jaise email khali ya galat format
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    // Humne khud throw kiya: new ResponseStatusException(UNAUTHORIZED, "...")
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(code.value());
        return ResponseEntity.status(code)
                .body(body(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, ex.getReason()));
    }

    // Kuch bhi anjaan: andar ki detail user ko mat dikhao, bas log me rakho
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", status.value());
        m.put("error", status.getReasonPhrase());
        m.put("message", message);
        return m;
    }
}