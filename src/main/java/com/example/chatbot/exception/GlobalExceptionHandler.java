package com.example.chatbot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Har error ek jaisa JSON:
 *   { "timestamp", "status", "error", "message", ("fields") }
 *
 * STEP 3 FIX: Step 1 wale handler me "Exception.class" wala catch-all Spring ke apne errors
 * (404 URL nahi mila, 405 galat method, 415 galat content-type, missing param) ko bhi 500 bana deta tha.
 * Ab wo errors apna asli status code rakhte hain (ErrorResponse check).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(body(ex.getStatusCode(), ex.getReason()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooLarge(MaxUploadSizeExceededException ex) {
        // 413: numeric code (enum ka naam Spring versions me badla hai)
        HttpStatusCode tooLarge = HttpStatusCode.valueOf(413);
        return ResponseEntity.status(tooLarge)
                .body(body(tooLarge, "File is too large (max 25 MB per file)"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "Malformed request body"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        // Spring ke apne HTTP errors (404, 405, 415, missing param ...) apna status code rakhein
        if (ex instanceof ErrorResponse er) {
            HttpStatusCode code = er.getStatusCode();
            return ResponseEntity.status(code).body(body(code, er.getBody().getDetail()));
        }
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"));
    }

    private Map<String, Object> body(HttpStatusCode code, String message) {
        HttpStatus status = HttpStatus.resolve(code.value());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", code.value());
        m.put("error", status != null ? status.getReasonPhrase() : "Error");
        m.put("message", message);
        return m;
    }
}