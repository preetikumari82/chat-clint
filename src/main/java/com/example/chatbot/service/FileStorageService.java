package com.example.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Original file disk par rakhta hai. Phase 2 me yahi class S3/MinIO se badal jayegi
 * (baaki code ko farak nahi padega).
 *
 * SECURITY: disk par file ka naam hum khud banate hain (UUID). User ka diya naam kabhi path me nahi jaata,
 * isliye "../../etc/passwd" jaisa path-traversal attack possible hi nahi.
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.storage.dir:./uploads}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("File storage root: {}", root);
    }

    /** relativePath DB me jata hai (portable), sha256 duplicate check ke liye. */
    public record Stored(String relativePath, String sha256, long size) {}

    public Stored store(MultipartFile file, String ext) throws IOException {
        LocalDate d = LocalDate.now();
        Path dir = root.resolve(String.valueOf(d.getYear()))
                       .resolve(String.format("%02d", d.getMonthValue()));
        Files.createDirectories(dir);

        Path target = dir.resolve(UUID.randomUUID() + "." + ext).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid storage path");
        }

        MessageDigest md = newSha256();
        // File ek hi baar padhte hain: save bhi, hash bhi
        try (InputStream in = new DigestInputStream(file.getInputStream(), md)) {
            Files.copy(in, target);
        }
        return new Stored(
                root.relativize(target).toString().replace('\\', '/'),
                HexFormat.of().formatHex(md.digest()),
                Files.size(target));
    }

    public Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Path outside storage root");
        }
        return p;
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not delete file {}: {}", relativePath, e.getMessage());
        }
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}