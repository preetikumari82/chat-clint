package com.example.chatbot.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Spring ka Page seedha JSON me nahi bhejte (format badal sakta hai). Apna simple shape. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}