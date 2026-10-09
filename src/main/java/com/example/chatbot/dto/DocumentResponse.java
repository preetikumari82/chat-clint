package com.example.chatbot.dto;

import com.example.chatbot.model.Document;
import com.example.chatbot.model.DocumentStatus;

import java.time.Instant;

/**
 * Admin console ko jo dikhana hai. Entity seedha kabhi bahar mat bhejo:
 * (1) password/internal path leak ho sakte hain, (2) lazy field par error aata hai.
 *
 * DHYAN: from() uploadedBy ko chhuta hai (LAZY). Isko sirf @Transactional service ke andar bulao,
 * kyunki open-in-view band hai.
 */
public record DocumentResponse(
        Long id,
        String fileName,
        String mimeType,
        long sizeBytes,
        DocumentStatus status,
        boolean enabled,
        int version,
        int chunkCount,
        String tags,
        String uploadedBy,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        String message
) {
   

	public static DocumentResponse from(Document d, String failureReason) {
        return new DocumentResponse(
                d.getId(),
                d.getFileName(),
                d.getMimeType(),
                d.getSizeBytes(),
                d.getStatus(),
                d.isEnabled(),
                d.getVersion(),
                d.getChunkCount(),
                d.getTags(),
                d.getUploadedBy() != null ? d.getUploadedBy().getEmail() : null,
                failureReason,
                d.getCreatedAt(),
                d.getUpdatedAt(),
        		d.getMessage());
        		
    }
}