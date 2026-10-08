package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Document ko process karne ki ek koshish. Ek document ke kai jobs ho sakte hain
 * (retry, re-index). Fail hone par yahan reason milta hai (FR-ING-08).
 */
@Entity
@Table(name = "ingestion_job", indexes = @Index(name = "idx_job_document", columnList = "document_id"))
@Getter
@Setter
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;

    private int attempts;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant startedAt;
    private Instant finishedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}