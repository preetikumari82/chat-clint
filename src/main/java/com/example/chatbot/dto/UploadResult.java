package com.example.chatbot.dto;

/**
 * Ek upload request me kai files ho sakti hain. Har file ka alag result:
 * ek kharab file se baaki ki upload nahi rukti.
 * status: UPLOADED | DUPLICATE | FAILED
 */
public record UploadResult(String fileName, String status, DocumentResponse document, String message) {

    public static UploadResult uploaded(String name, DocumentResponse d) {
        return new UploadResult(name, "UPLOADED", d, "Document uploaded successfully (id " + d.id() + ").");
    }

    public static UploadResult duplicate(String name, DocumentResponse existing) {
        return new UploadResult(name, "DUPLICATE", existing,
                "Same file already exists (id " + existing.id() + "). Use force=true to upload anyway.");
    }

    public static UploadResult failed(String name, String reason) {
        return new UploadResult(name, "FAILED", null, reason);
    }
}