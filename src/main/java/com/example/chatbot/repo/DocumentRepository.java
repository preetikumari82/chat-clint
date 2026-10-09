package com.example.chatbot.repo;

import com.example.chatbot.model.Document;
import com.example.chatbot.model.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Sirf naam se bani queries. Worker ke bulk updates IngestionStateUpdater (SQL) me hain. */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findFirstBySha256(String sha256);

    Page<Document> findByFileNameContainingIgnoreCase(String q, Pageable pageable);

    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    List<Document> findByStatusIn(Collection<DocumentStatus> statuses);

    long countByStatus(DocumentStatus status);
}