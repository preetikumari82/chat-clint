package com.example.chatbot.service;

import com.example.chatbot.dto.DocumentResponse;
import com.example.chatbot.dto.DocumentUpdateRequest;
import com.example.chatbot.dto.JobResponse;
import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.dto.UploadResult;
import com.example.chatbot.model.AdminUser;
import com.example.chatbot.model.Document;
import com.example.chatbot.model.DocumentStatus;
import com.example.chatbot.model.IngestionJob;
import com.example.chatbot.model.JobStatus;
import com.example.chatbot.repo.AdminUserRepository;
import com.example.chatbot.repo.DocumentRepository;
import com.example.chatbot.repo.IngestionJobRepository;
import com.example.chatbot.security.CurrentUser;
import com.example.chatbot.service.ingestion.IngestionRequestedEvent;
import com.example.chatbot.service.ingestion.VectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Documents ka business logic. (Step 4: upload ke baad ingestion job queue hota hai.)
 *
 * @Transactional ZAROORI hai: entity -> DTO conversion (uploadedBy LAZY) transaction ke andar hona chahiye.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository docRepo;
    private final IngestionJobRepository jobRepo;
    private final AdminUserRepository userRepo;
    private final FileStorageService storage;
    private final FileValidator validator;
    private final AuditService audit;
    private final VectorIndexService vectorIndex;
    private final ApplicationEventPublisher events;

    // ------------------------------------------------------------------ upload

    @Transactional
    public List<UploadResult> upload(List<MultipartFile> files, String tags, boolean force) {
        AdminUser uploader = userRepo.findByEmail(CurrentUser.email()).orElse(null);
        List<UploadResult> results = new ArrayList<>();
        for (MultipartFile f : files) {
            results.add(uploadOne(f, tags, force, uploader));
        }
        return results;
    }

    private UploadResult uploadOne(MultipartFile f, String tags, boolean force, AdminUser uploader) {
        String shownName = f.getOriginalFilename();
        FileStorageService.Stored stored = null;
        try {
            FileValidator.Validated v = validator.validate(f);
            shownName = v.cleanName();

            stored = storage.store(f, v.ext());

            Optional<Document> dup = docRepo.findFirstBySha256(stored.sha256());
            if (dup.isPresent() && !force) {
                storage.delete(stored.relativePath());
                return UploadResult.duplicate(shownName, toResponse(dup.get()));
            }

            Document d = new Document();
            d.setFileName(v.cleanName());
            d.setMimeType(v.mimeType());
            d.setSizeBytes(stored.size());
            d.setSha256(stored.sha256());
            d.setStoragePath(stored.relativePath());
            d.setTags(tags);
            d.setUploadedBy(uploader);
            d = docRepo.save(d);

            audit.log("UPLOAD", "DOCUMENT", String.valueOf(d.getId()), v.cleanName());

            enqueue(d);   // STEP 4: ingestion shuru (commit ke baad worker chalega)
            return UploadResult.uploaded(shownName, toResponse(d));

        } catch (ResponseStatusException e) {
            return UploadResult.failed(shownName, e.getReason());
        } catch (IOException | RuntimeException e) {
            log.error("Upload failed for {}", shownName, e);
            if (stored != null) {
                storage.delete(stored.relativePath());
            }
            return UploadResult.failed(shownName, "Could not store file");
        }
    }

    // ------------------------------------------------------------------ ingestion control

    /** Job banao, document ko PROCESSING dikhao, aur commit ke baad worker ko event bhejo. */
    private void enqueue(Document d) {
        IngestionJob job = new IngestionJob();
        job.setDocument(d);
        job.setStatus(JobStatus.QUEUED);
        job = jobRepo.save(job);

        d.setStatus(DocumentStatus.PROCESSING);   // managed entity: commit par save ho jayega
        events.publishEvent(new IngestionRequestedEvent(job.getId(), d.getId()));
    }

    /** Ek document dobara process karo (FR-DOC-07). FAILED ho to "retry" bhi yahi hai. */
    @Transactional
    public DocumentResponse reindex(Long id) {
        Document d = find(id);
        if (d.getStatus() == DocumentStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document is already being processed");
        }
        enqueue(d);
        audit.log("REINDEX", "DOCUMENT", String.valueOf(id), d.getFileName());
        return toResponse(d);
    }

    /**
     * onlyPending=true  : sirf UPLOADED + FAILED (kam kharch, default)
     * onlyPending=false : SAARE documents dobara embed (Gemini ka kharch / rate limit dhyan me rakho)
     */
    @Transactional
    public int reindexAll(boolean onlyPending) {
        List<Document> docs = onlyPending
                ? docRepo.findByStatusIn(List.of(DocumentStatus.UPLOADED, DocumentStatus.FAILED))
                : docRepo.findAll();
        int queued = 0;
        for (Document d : docs) {
            if (d.getStatus() != DocumentStatus.PROCESSING) {
                enqueue(d);
                queued++;
            }
        }
        audit.log("REINDEX_ALL", "DOCUMENT", null, "queued=" + queued + ", onlyPending=" + onlyPending);
        return queued;
    }

    @Transactional(readOnly = true)
    public List<JobResponse> jobs(Long id) {
        find(id);
        return jobRepo.findByDocumentIdOrderByCreatedAtDesc(id).stream().map(JobResponse::from).toList();
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(String q, DocumentStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Document> p;
        if (q != null && !q.isBlank()) {
            p = docRepo.findByFileNameContainingIgnoreCase(q.trim(), pr);
        } else if (status != null) {
            p = docRepo.findByStatus(status, pr);
        } else {
            p = docRepo.findAll(pr);
        }
        return PageResponse.of(p.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long id) {
        return toResponse(find(id));
    }

    // ------------------------------------------------------------------ update / delete

    @Transactional
    public DocumentResponse update(Long id, DocumentUpdateRequest req) {
        Document d = find(id);
        if (req.enabled() != null) {
            d.setEnabled(req.enabled());
        }
        if (req.tags() != null) {
            d.setTags(req.tags());
        }
        audit.log("DOCUMENT_UPDATE", "DOCUMENT", String.valueOf(id),
                "enabled=" + d.isEnabled() + ", tags=" + d.getTags());
        return toResponse(d);
    }

    @Transactional
    public void delete(Long id) {
        Document d = find(id);
        if (d.getStatus() == DocumentStatus.PROCESSING) {
            // Worker abhi vectors likh raha ho sakta hai; beech me delete karne se kachra bachta
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Document is being processed. Try again in a moment.");
        }
        String path = d.getStoragePath();
        String name = d.getFileName();

        int vectors = vectorIndex.deleteByDocument(id);   // bot isko turant bhool jata hai (FR-DOC-05)
        jobRepo.deleteByDocumentId(id);
        docRepo.delete(d);

        storage.delete(path);
        audit.log("DELETE", "DOCUMENT", String.valueOf(id), name + " (vectors removed: " + vectors + ")");
    }

    // ------------------------------------------------------------------ helpers

    private Document find(Long id) {
        return docRepo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private DocumentResponse toResponse(Document d) {
        String reason = jobRepo.findFirstByDocumentIdOrderByCreatedAtDesc(d.getId())
                .filter(j -> j.getStatus() == JobStatus.FAILED)
                .map(IngestionJob::getErrorMessage)
                .orElse(null);
        return DocumentResponse.from(d, reason);
    }
}