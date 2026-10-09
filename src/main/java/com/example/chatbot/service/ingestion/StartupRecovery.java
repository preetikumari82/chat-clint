package com.example.chatbot.service.ingestion;

import com.example.chatbot.model.DocumentStatus;
import com.example.chatbot.model.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Server beech me band ho gaya to kuch jobs "RUNNING" me atak jate hain (worker mar chuka hai).
 * Start par unhe FAILED kar dete hain, taaki admin "re-index" dabakar dobara chala sake.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRecovery implements CommandLineRunner {

    private final IngestionStateUpdater state;

    @Override
    public void run(String... args) {
        int jobs = state.failUnfinishedJobs(List.of(JobStatus.RUNNING, JobStatus.QUEUED),
                "Interrupted by server restart. Please re-index.");
        int docs = state.moveDocumentStatus(DocumentStatus.PROCESSING, DocumentStatus.FAILED);
        if (jobs > 0 || docs > 0) {
            log.warn("Recovered {} interrupted job(s) and {} document(s)", jobs, docs);
        }
    }
}