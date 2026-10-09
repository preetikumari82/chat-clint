package com.example.chatbot.controller;

import com.example.chatbot.dto.DocumentResponse;
import com.example.chatbot.dto.DocumentUpdateRequest;
import com.example.chatbot.dto.JobResponse;
import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.dto.UploadResult;
import com.example.chatbot.model.DocumentStatus;
import com.example.chatbot.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<UploadResult> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "tags", required = false) String tags,
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        return service.upload(files, tags, force);
    }

    @GetMapping
    public PageResponse<DocumentResponse> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) DocumentStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(q, status, page, size);
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public DocumentResponse update(@PathVariable("id") Long id,
                                   @Valid @RequestBody DocumentUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Step 4: ingestion control ----------------

    /** Dobara process / FAILED ka retry. */
    @PostMapping("/{id}/reindex")
    public DocumentResponse reindex(@PathVariable("id") Long id) {
        return service.reindex(id);
    }

    /** onlyPending=true (default): sirf UPLOADED + FAILED. false: sab (kharcha!). */
    @PostMapping("/reindex-all")
    public Map<String, Object> reindexAll(
            @RequestParam(name = "onlyPending", defaultValue = "true") boolean onlyPending) {
        return Map.of("queued", service.reindexAll(onlyPending));
    }

    /** Is document ke saare processing attempts + error reasons. */
    @GetMapping("/{id}/jobs")
    public List<JobResponse> jobs(@PathVariable("id") Long id) {
        return service.jobs(id);
    }
}