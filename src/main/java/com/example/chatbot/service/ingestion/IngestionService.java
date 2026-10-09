package com.example.chatbot.service.ingestion;

import com.example.chatbot.model.Document;
import com.example.chatbot.model.DocumentStatus;
import com.example.chatbot.model.IngestionJob;
import com.example.chatbot.model.JobStatus;
import com.example.chatbot.repo.DocumentRepository;
import com.example.chatbot.repo.IngestionJobRepository;
import com.example.chatbot.service.FileStorageService;
import com.example.chatbot.service.ingestion.ChunkingService.Chunk;
import com.example.chatbot.service.ingestion.TextExtractionService.Segment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Background worker: ek job ka poora safar (SRS 4.4):
 *   extract -> clean+chunk -> embed -> upsert -> INDEXED   (ya FAILED + reason)
 *
 * DHYAN: Is class me @Transactional NAHI hai, jaan-boojh kar. Embedding me kai second lag sakte hain;
 * itni der DB connection / transaction khula rakhna galat hai. Har save() apni chhoti transaction hai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final IngestionJobRepository jobRepo;
    private final DocumentRepository docRepo;
    private final FileStorageService storage;
    private final TextExtractionService extractor;
    private final ChunkingService chunker;
    private final VectorIndexService index;
    private final IngestionStateUpdater state;

    @Async("ingestionExecutor")
    public void process(Long jobId, Long documentId) {
        IngestionJob job = jobRepo.findById(jobId).orElse(null);
        Document doc = docRepo.findById(documentId).orElse(null);
        if (job == null || doc == null) {
            log.warn("Ingestion skipped: job {} or document {} no longer exists", jobId, documentId);
            return;
        }

        long t0 = System.currentTimeMillis();
        try {
            job.setStatus(JobStatus.RUNNING);
            job.setAttempts(job.getAttempts() + 1);
            job.setStartedAt(Instant.now());
            jobRepo.save(job);

            Path file = storage.resolve(doc.getStoragePath());
            String ext = extensionOf(doc.getStoragePath());

            List<Segment> segments = extractor.extract(file, ext);
            List<Chunk> chunks = chunker.chunk(segments, "md".equals(ext));
            if (chunks.isEmpty()) {
                throw new IngestionException("No usable text found in this file");
            }
            log.info("Document {}: {} segments -> {} chunks", documentId, segments.size(), chunks.size());

            index.addChunks(doc, jobId, chunks);      // naye chunks pehle
            index.deleteStale(documentId, jobId);     // phir purane (no downtime)

            state.updateDocumentState(documentId, DocumentStatus.INDEXED, chunks.size());
            job.setStatus(JobStatus.SUCCEEDED);
            job.setErrorMessage(null);
            job.setFinishedAt(Instant.now());
            jobRepo.save(job);

            log.info("Document {} INDEXED in {} ms ({} chunks)", documentId,
                    System.currentTimeMillis() - t0, chunks.size());

        } catch (Exception e) {
            log.error("Ingestion failed for document {}", documentId, e);
            try {
                index.deleteByJob(documentId, jobId);   // aadha kaam saaf
            } catch (Exception cleanup) {
                log.warn("Cleanup after failure also failed: {}", cleanup.getMessage());
            }
            state.updateDocumentState(documentId, DocumentStatus.FAILED, 0);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(shorten(e));
            job.setFinishedAt(Instant.now());
            jobRepo.save(job);
        }
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase();
    }

    /** Admin ko dikhane layak chhota message (stack trace nahi). */
    private String shorten(Exception e) {
        String m = (e instanceof IngestionException && e.getCause() != null)
                ? e.getMessage()
                : "Processing failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        return m != null && m.length() > 500 ? m.substring(0, 500) : m;
    }
}