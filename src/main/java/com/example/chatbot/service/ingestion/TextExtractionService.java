package com.example.chatbot.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** File se text nikalna (SRS 3.6). Page number yaad rakhte hain taaki citation ban sake. */
@Slf4j
@Service
public class TextExtractionService {

    /** page null = page ki ginti nahi (docx, txt ...) */
    public record Segment(Integer page, String text) {}

    public List<Segment> extract(Path file, String ext) {
        return switch (ext) {
            case "pdf" -> extractPdf(file);
            case "docx", "doc" -> extractWithTika(file);
            case "txt", "md" -> extractPlainText(file);
            case "png", "jpg", "jpeg", "webp" -> throw new IngestionException(
                    "Image files need OCR, which is added in Step 4B. Not supported yet.");
            default -> throw new IngestionException("Unsupported file type: ." + ext);
        };
    }

    // ---------------------------------------------------------------- PDF
    private List<Segment> extractPdf(Path file) {
        List<Segment> out = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
            if (pdf.isEncrypted()) {
                throw new IngestionException("PDF is password protected");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            int pages = pdf.getNumberOfPages();
            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(pdf);
                if (text != null && !text.isBlank()) {
                    out.add(new Segment(p, text));
                }
            }
        } catch (IngestionException e) {
            throw e;
        } catch (IOException e) {
            throw new IngestionException("Could not read PDF: " + e.getMessage(), e);
        }
        if (out.isEmpty()) {
            throw new IngestionException(
                    "No text found in PDF. It may be a scanned PDF (needs OCR, coming in Step 4B).");
        }
        return out;
    }

    // ---------------------------------------------------------------- DOC / DOCX
    private List<Segment> extractWithTika(Path file) {
        try {
            var docs = new TikaDocumentReader(new FileSystemResource(file)).get();
            List<Segment> out = new ArrayList<>();
            for (var d : docs) {
                String t = d.getText();
                if (t != null && !t.isBlank()) {
                    out.add(new Segment(null, t));
                }
            }
            if (out.isEmpty()) {
                throw new IngestionException("No text found in document");
            }
            return out;
        } catch (IngestionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IngestionException("Could not read document: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------- TXT / MD
    private List<Segment> extractPlainText(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            String text = decode(bytes);
            if (text.isBlank()) {
                throw new IngestionException("File is empty");
            }
            return List.of(new Segment(null, text));
        } catch (IOException e) {
            throw new IngestionException("Could not read file: " + e.getMessage(), e);
        }
    }

    /** Pehle strict UTF-8; na ban sake to Windows-1252 (SRS 3.6: UTF-8 / Windows-1252). */
    private String decode(byte[] bytes) {
        String s;
        try {
            s = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            s = new String(bytes, Charset.forName("windows-1252"));
        }
        return s.startsWith("\uFEFF") ? s.substring(1) : s;   // BOM hatao
    }
}