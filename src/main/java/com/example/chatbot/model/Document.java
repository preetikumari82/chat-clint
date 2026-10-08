package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Admin ki upload ki hui ek file (SRS 4.7 DOCUMENT).
 *
 * DHYAN: Spring AI ki bhi ek class "Document" hai (org.springframework.ai.document.Document).
 * Jis file me dono chahiye, Spring AI wali ko poora naam likho:
 *     org.springframework.ai.document.Document chunk = new org.springframework.ai.document.Document(...)
 */
@Entity
@Table(name = "document", indexes = {
        @Index(name = "idx_document_sha256", columnList = "sha256"),
        @Index(name = "idx_document_status", columnList = "status")
})
@Data
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 150)
    private String mimeType;

    private long sizeBytes;

    /** File ka fingerprint. Same file dobara upload ho to pakad lete hain (FR-DOC-09). */
    @Column(nullable = false, length = 64)
    private String sha256;

    /** Disk / S3 par original file kahan rakhi hai. */
    @Column(nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)   // STRING: DB me "INDEXED" likha jayega, 2 nahi
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    /** Nayi version upload hone par badhta hai (FR-DOC-06). */
    private int version = 1;

    /** false = bot is document ko use nahi karega, par delete bhi nahi hua (FR-DOC-08). */
    private boolean enabled = true;

    private int chunkCount;

    /** Comma-separated tags, jaise "hr,policy" (FR-DOC-10, Could). */
    @Column(length = 500)
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private AdminUser uploadedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

	public Long getId() {
		// TODO Auto-generated method stub
		return null;
	}
}