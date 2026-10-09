package com.example.chatbot.service.ingestion;

import com.example.chatbot.config.TenantDefaults;
import com.example.chatbot.model.Document;
import com.example.chatbot.service.ingestion.ChunkingService.Chunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * pgvector se baat karne wali SIRF EK class.
 *  - Likhna: Spring AI VectorStore (wo khud text ko embedding me badalta hai)
 *  - Hatana: seedha SQL (metadata ke document_id / job_id par). Filter-expression API versions me badalti
 *    rehti hai, SQL nahi.
 * Phase 2 me tenant filter bhi isi ek jagah lagega (SRS 5.5 "ek hi jagah").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    @Value("${app.ingestion.embed-batch-size:20}")
    private int batchSize;

    @Value("${app.ingestion.max-retries:3}")
    private int maxRetries;

    @Value("${app.ingestion.retry-base-ms:2000}")
    private long retryBaseMs;

    /** Chunks ko embed karke vector_store me daalta hai. Har chunk ke metadata me SRS FR-ING-06 ki cheezein. */
    public void addChunks(Document doc, Long jobId, List<Chunk> chunks) {
        List<org.springframework.ai.document.Document> all = new ArrayList<>(chunks.size());

        for (Chunk c : chunks) {
            Map<String, Object> md = new HashMap<>();
            md.put("document_id", doc.getId());
            md.put("source_name", doc.getFileName());
            md.put("tenant_id", TenantDefaults.TENANT_ID);   // Phase 2 ke liye abhi se
            md.put("bot_id", TenantDefaults.BOT_ID);
            md.put("job_id", jobId);                           // kis run ne banaya (re-index me kaam aata hai)
            md.put("chunk_index", c.index());
            md.put("content_hash", c.hash());
            if (c.page() != null) {
                md.put("page", c.page());                      // null value metadata me allowed nahi, isliye if
            }
            if (c.heading() != null) {
                md.put("heading", c.heading());
            }
            all.add(org.springframework.ai.document.Document.builder()
                    .id(UUID.randomUUID().toString())          // pgvector ki id column uuid hai
                    .text(c.text())
                    .metadata(md)
                    .build());
        }

        for (int i = 0; i < all.size(); i += batchSize) {
            List<org.springframework.ai.document.Document> batch =
                    all.subList(i, Math.min(i + batchSize, all.size()));
            addWithRetry(batch);
            log.info("Embedded + stored {}/{} chunks of document {}",
                    Math.min(i + batchSize, all.size()), all.size(), doc.getId());
        }
    }

    /** Retry + exponential backoff (FR-ING-07): 2s, 4s, 8s ... Rate limit (429) par kaam aata hai. */
    private void addWithRetry(List<org.springframework.ai.document.Document> batch) {
        for (int attempt = 1; ; attempt++) {
            try {
                vectorStore.add(batch);
                return;
            } catch (RuntimeException e) {
                if (attempt >= maxRetries) {
                    throw e;
                }
                long wait = retryBaseMs * (1L << (attempt - 1));
                log.warn("Embedding batch failed (attempt {}/{}): {}. Retrying in {} ms",
                        attempt, maxRetries, e.getMessage(), wait);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /** Document delete / FAILED cleanup. */
    public int deleteByDocument(Long documentId) {
        return jdbc.update("DELETE FROM vector_store WHERE metadata->>'document_id' = ?",
                String.valueOf(documentId));
    }

    /** Re-index rule (SRS 4.4): naye chunks ready hone ke BAAD purane hatao -> bot ko kabhi khali nahi dikhta. */
    public int deleteStale(Long documentId, Long currentJobId) {
        return jdbc.update(
                "DELETE FROM vector_store WHERE metadata->>'document_id' = ? AND metadata->>'job_id' <> ?",
                String.valueOf(documentId), String.valueOf(currentJobId));
    }

    /** Job beech me fail ho gaya: sirf is job ke aadhe-adhure chunks hatao (purane safe rehte hain). */
    public int deleteByJob(Long documentId, Long jobId) {
        return jdbc.update(
                "DELETE FROM vector_store WHERE metadata->>'document_id' = ? AND metadata->>'job_id' = ?",
                String.valueOf(documentId), String.valueOf(jobId));
    }
}