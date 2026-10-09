package com.example.chatbot.service.ingestion;

import com.example.chatbot.service.ingestion.TextExtractionService.Segment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lamba text -> chhote tukde (chunks). RAG ka sabse important hissa.
 *
 * Niyam (SRS 3.7): chunk na bahut bada (token waste, dhundhla matlab), na bahut chhota (matlab toot jata hai).
 * Hum SIZE ko characters me naapte hain (1 token ~ 4 English chars). 2400 chars ~ 600 tokens.
 * Hindi/Devanagari me 1 token ~ 1.5-2 chars hota hai, to Hindi-heavy docs ke liye app.rag.chunk-chars ~1000 rakho.
 *
 * Tareeka: paragraph ki seema par todo; lamba paragraph ho to sentence par; fir bhi lamba ho to word par.
 * Har agle chunk ke shuru me pichhle chunk ka thoda hissa (overlap) dohrate hain, taaki
 * seema par baat adhoori na kat jaye. Chunk kabhi page ke paar nahi jaata (page citation sahi rahe).
 */
@Service
public class ChunkingService {

    public record Chunk(int index, Integer page, String heading, String text, String hash) {}

    private record Section(String heading, String body) {}

    private static final int MIN_CHARS = 20;
    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.+?)\\s*$");

    private final int maxChars;
    private final int overlap;

    public ChunkingService(@Value("${app.rag.chunk-chars:2400}") int maxChars,
                           @Value("${app.rag.chunk-overlap-chars:300}") int overlap) {
        this.maxChars = Math.max(200, maxChars);
        this.overlap = Math.max(0, Math.min(overlap, this.maxChars / 2));
    }

    public List<Chunk> chunk(List<Segment> segments, boolean markdown) {
        List<Chunk> out = new ArrayList<>();
        int idx = 0;
        String heading = null;

        for (Segment seg : segments) {
            String text = normalize(seg.text());
            List<Section> sections = markdown
                    ? splitByHeadings(text, heading)
                    : List.of(new Section(heading, text));

            for (Section s : sections) {
                heading = s.heading();     // agle page/section ko bhi yaad rahe
                for (String piece : pack(s.body())) {
                    String t = piece.strip();
                    if (t.length() < MIN_CHARS) {
                        continue;          // page number jaisa kachra
                    }
                    out.add(new Chunk(idx++, seg.page(), s.heading(), t, sha256(t)));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------ markdown headings
    private List<Section> splitByHeadings(String text, String carriedHeading) {
        List<Section> sections = new ArrayList<>();
        String current = carriedHeading;
        StringBuilder body = new StringBuilder();

        for (String line : text.split("\n", -1)) {
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                if (body.toString().strip().length() > 0) {
                    sections.add(new Section(current, body.toString()));
                    body = new StringBuilder();
                }
                current = m.group(1);
            }
            body.append(line).append('\n');   // heading line chunk ke andar bhi rakhte hain (search me kaam aati hai)
        }
        if (body.toString().strip().length() > 0) {
            sections.add(new Section(current, body.toString()));
        }
        return sections;
    }

    // ------------------------------------------------------------ packing with overlap
    private List<String> pack(String body) {
        List<String> pieces = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        for (String para : body.split("\\n{2,}")) {
            String p = para.strip();
            if (p.isEmpty()) {
                continue;
            }
            for (String part : splitLong(p)) {
                if (buf.length() > 0 && buf.length() + 2 + part.length() > maxChars) {
                    pieces.add(buf.toString());
                    buf = new StringBuilder(tail(buf.toString()));
                }
                if (buf.length() > 0) {
                    buf.append("\n\n");
                }
                buf.append(part);
            }
        }
        if (buf.length() > 0) {
            pieces.add(buf.toString());
        }
        return pieces;
    }

    /** Lamba paragraph: sentence par todo; sentence bhi lamba ho to word par. */
    private List<String> splitLong(String p) {
        if (p.length() <= maxChars) {
            return List.of(p);
        }
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String sent : p.split("(?<=[.!?\u0964])\\s+|\\n")) {
            if (sent.length() > maxChars) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur = new StringBuilder();
                }
                hardCut(sent, out);
                continue;
            }
            if (cur.length() > 0 && cur.length() + 1 + sent.length() > maxChars) {
                out.add(cur.toString());
                cur = new StringBuilder();
            }
            if (cur.length() > 0) {
                cur.append(' ');
            }
            cur.append(sent);
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    private void hardCut(String s, List<String> out) {
        while (s.length() > maxChars) {
            int cut = s.lastIndexOf(' ', maxChars);
            if (cut < maxChars / 2) {
                cut = maxChars;            // space mila hi nahi (jaise lamba URL): seedha kaato
            }
            out.add(s.substring(0, cut));
            s = s.substring(cut).stripLeading();
        }
        if (!s.isEmpty()) {
            out.add(s);
        }
    }

    /** Pichhle chunk ke aakhri ~overlap characters (word ki seema se shuru). */
    private String tail(String s) {
        if (overlap == 0 || s.length() <= overlap) {
            return "";
        }
        int from = s.length() - overlap;
        for (int i = from; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return s.substring(i + 1);
            }
        }
        return s.substring(from);
    }

    // ------------------------------------------------------------ helpers
    private String normalize(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n').replace("\0", "")
                .replaceAll("[ \\t\\u00A0]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}