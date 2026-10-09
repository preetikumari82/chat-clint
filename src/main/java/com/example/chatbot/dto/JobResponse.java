package com.example.chatbot.dto;

import com.example.chatbot.model.IngestionJob;
import com.example.chatbot.model.JobStatus;

import java.time.Instant;

/** Ek ingestion koshish ka hisaab (admin "kyun fail hua" dekh sake). */
public record JobResponse(Long id, JobStatus status, int attempts, String errorMessage,
                          Instant startedAt, Instant finishedAt, Instant createdAt) {

    public static JobResponse from(IngestionJob j) {
        return new JobResponse(j.getId(), j.getStatus(), j.getAttempts(), j.getErrorMessage(),
                j.getStartedAt(), j.getFinishedAt(), j.getCreatedAt());
    }
}