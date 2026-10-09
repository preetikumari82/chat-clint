package com.example.chatbot.repo;

import com.example.chatbot.model.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {

    List<IngestionJob> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    Optional<IngestionJob> findFirstByDocumentIdOrderByCreatedAtDesc(Long documentId);

    void deleteByDocumentId(Long documentId);
}