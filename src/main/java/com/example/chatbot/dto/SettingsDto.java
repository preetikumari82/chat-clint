package com.example.chatbot.dto;

import jakarta.validation.constraints.*;

/**
 * Admin jo badal sakta hai (FR-SET-01/02). embedModel jaan-boojh kar nahi hai:
 * use badalna = sab documents dobara embed karna.
 */
public record SettingsDto(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String welcomeMessage,
        @Size(max = 500) String fallbackMessage,
        @Size(max = 4000) String systemPrompt,
        @NotBlank @Size(max = 100) String chatModel,
        @Min(1) @Max(20) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
        @Min(50) @Max(4000) int maxTokens
) {}