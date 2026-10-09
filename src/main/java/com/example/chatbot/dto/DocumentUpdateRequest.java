package com.example.chatbot.dto;

import jakarta.validation.constraints.Size;

/** PATCH body. Jo field null hai use chhedte nahi. */
public record DocumentUpdateRequest(
        Boolean enabled,
        @Size(max = 500) String tags
) {}