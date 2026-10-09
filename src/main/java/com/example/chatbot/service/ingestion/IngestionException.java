package com.example.chatbot.service.ingestion;

/** Ingestion me "samajh aane wali" galti (jaise khali PDF). Message admin ko dikhta hai. */
public class IngestionException extends RuntimeException {
    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}