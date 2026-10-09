package com.example.chatbot.service.ingestion;

/** "Ye job ab chalao" ka sandesh. Transaction commit hone ke BAAD hi worker ko jata hai. */
public record IngestionRequestedEvent(Long jobId, Long documentId) {}