package com.example.chatbot.dto;

/** Jawab ke saath dikhne wala source: "policy.pdf, page 2". */
public record SourceDto(
        Long documentId,
        String documentName,
        Integer page,
        String heading
) {}