package com.example.chatbot.repo;

import com.example.chatbot.model.Document;
import com.example.chatbot.model.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Method ke naam se Spring khud SQL bana leta hai (findBy + field naam). */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /** Duplicate upload pakadne ke liye (FR-DOC-09). */
    Optional<Document> findFirstBySha256(String sha256);

    /** Admin list: file naam me search + pagination (FR-DOC-04). */
    Page<Document> findByFileNameContainingIgnoreCase(String q, Pageable pageable);

    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    long countByStatus(DocumentStatus status);
}