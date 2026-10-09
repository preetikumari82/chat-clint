package com.example.chatbot.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * FR-DOC-02: extension + ASLI content check (magic bytes). Client ka bheja Content-Type ya naam
 * par bharosa nahi karte: "virus.exe" ko "report.pdf" naam dene se wo PDF nahi ban jaata.
 *
 * Size limit Spring (multipart.max-file-size) pehle hi laga deta hai.
 */
@Component
public class FileValidator {

    public record Validated(String cleanName, String ext, String mimeType) {}

    /** Allow-list: jo yahan nahi, wo reject (exe, zip, js, html ... sab). */
    private static final Map<String, String> MIME = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "txt", "text/plain",
            "md", "text/markdown",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp");

    public Validated validate(MultipartFile f) {
        if (f == null || f.isEmpty()) {
            throw bad("File is empty");
        }
        String clean = cleanName(f.getOriginalFilename());
        String ext = extension(clean);
        if (!MIME.containsKey(ext)) {
            throw bad("File type not allowed: ." + ext + " (allowed: " + String.join(", ", new java.util.TreeSet<>(MIME.keySet())) + ")");
        }

        byte[] head = readHead(f, 8192);
        rejectExecutables(head);
        checkMagic(ext, head, f);

        return new Validated(clean, ext, MIME.get(ext));
    }

    // ---------- helpers ----------

    /** Path hata do, control characters hata do, length cap. Ye naam sirf dikhane ke liye; disk par UUID naam jata hai. */
    private String cleanName(String original) {
        String n = original == null ? "file" : original;
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        n = n.replaceAll("[\\p{Cntrl}]", "").trim();
        if (n.isEmpty()) {
            n = "file";
        }
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private byte[] readHead(MultipartFile f, int n) {
        try (InputStream in = f.getInputStream()) {
            return in.readNBytes(n);
        } catch (IOException e) {
            throw bad("Could not read file");
        }
    }

    private void rejectExecutables(byte[] h) {
        boolean exe = starts(h, 'M', 'Z');                          // Windows exe/dll
        boolean elf = h.length > 3 && h[0] == 0x7F && h[1] == 'E' && h[2] == 'L' && h[3] == 'F';  // Linux binary
        if (exe || elf) {
            throw bad("Executable files are not allowed");
        }
    }

    private void checkMagic(String ext, byte[] h, MultipartFile f) {
        boolean ok = switch (ext) {
            case "pdf" -> contains(h, "%PDF-");
            case "doc" -> h.length > 7 && (h[0] & 0xFF) == 0xD0 && (h[1] & 0xFF) == 0xCF
                    && (h[2] & 0xFF) == 0x11 && (h[3] & 0xFF) == 0xE0;
            case "docx" -> starts(h, 'P', 'K') && zipHasEntry(f, "word/document.xml");
            case "png" -> h.length > 7 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg", "jpeg" -> h.length > 2 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "webp" -> h.length > 11 && starts(h, 'R', 'I', 'F', 'F')
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            case "txt", "md" -> looksLikeText(h);
            default -> false;
        };
        if (!ok) {
            throw bad("File content does not match its ." + ext + " extension");
        }
    }

    /** Text me NUL byte (0x00) nahi hoti. Binary file ko .txt naam dena yahan pakda jata hai. */
    private boolean looksLikeText(byte[] h) {
        for (byte b : h) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * DOCX asal me ZIP hai, jisme word/document.xml hota hai. Sirf entry ke NAAM dekhte hain,
     * kuch extract/save nahi karte. (Zip-bomb ka poora bachaav Step 10 hardening me.)
     */
    private boolean zipHasEntry(MultipartFile f, String wanted) {
        try (ZipInputStream z = new ZipInputStream(f.getInputStream())) {
            ZipEntry e;
            int count = 0;
            while ((e = z.getNextEntry()) != null && count++ < 2000) {
                if (wanted.equals(e.getName())) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    private boolean starts(byte[] h, char... c) {
        if (h.length < c.length) {
            return false;
        }
        for (int i = 0; i < c.length; i++) {
            if (h[i] != (byte) c[i]) {
                return false;
            }
        }
        return true;
    }

    /** PDF kabhi shuru me kuch junk bhi rakhta hai, isliye pehle 1024 bytes me dhundte hain. */
    private boolean contains(byte[] h, String s) {
        String head = new String(h, 0, Math.min(h.length, 1024), java.nio.charset.StandardCharsets.ISO_8859_1);
        return head.contains(s);
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}