package com.example.chatbot.service.ingestion;

import com.example.chatbot.model.DocumentStatus;
import com.example.chatbot.model.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Worker ke "targeted UPDATE" yahan hain, seedhe SQL se.
 *
 * Kyun JPA @Query nahi? Worker ko poora entity save() nahi karna (admin ka beech me badla hua "enabled"
 * overwrite ho jata). Isliye sirf status/chunk_count wale columns badalte hain. Pehle ye @Query(HQL) the,
 * par Hibernate 7 ke HQL parser me parameter-naam ki dikkat se app start nahi hui. SQL me ye risk hai hi nahi.
 *
 * Table/column naam Hibernate ke snake_case se aate hain (Step 2 ke create table log me dekha tha).
 */
@Service
@RequiredArgsConstructor
public class IngestionStateUpdater {

    private final JdbcTemplate jdbc;

    /** Document ka final haal: INDEXED (chunks ke saath) ya FAILED. */
    public void updateDocumentState(Long documentId, DocumentStatus status, int chunkCount) {
        jdbc.update("UPDATE document SET status = ?, chunk_count = ?, updated_at = ? WHERE id = ?",
                status.name(), chunkCount, now(), documentId);
    }

    /** Restart ke baad atke documents: PROCESSING -> FAILED. */
    public int moveDocumentStatus(DocumentStatus from, DocumentStatus to) {
        return jdbc.update("UPDATE document SET status = ?, updated_at = ? WHERE status = ?",
                to.name(), now(), from.name());
    }

    /** Restart ke baad atke jobs (RUNNING / QUEUED) -> FAILED. */
    public int failUnfinishedJobs(List<JobStatus> unfinished, String message) {
        String in = unfinished.stream().map(s -> "'" + s.name() + "'").collect(Collectors.joining(","));
        // in-list enum ke naam se bana hai (hamare apne constants), user input nahi
        return jdbc.update("UPDATE ingestion_job SET status = ?, error_message = ?, finished_at = ? WHERE status IN (" + in + ")",
                JobStatus.FAILED.name(), message, now());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }
}