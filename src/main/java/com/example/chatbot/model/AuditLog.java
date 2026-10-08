package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Kisne kya kiya (FR-AUTH-05). Email text me rakhte hain, FK nahi: admin delete ho jaye
 * to bhi audit history bachi rehni chahiye.
 */
@Entity
@Table(name = "audit_log", indexes = @Index(name = "idx_audit_created", columnList = "created_at"))
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String actorEmail;

    @Column(nullable = false, length = 50)
    private String action;          // LOGIN, UPLOAD, DELETE, SETTINGS_UPDATE ...

    @Column(length = 50)
    private String entityType;      // DOCUMENT, SETTINGS ...

    @Column(length = 100)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}