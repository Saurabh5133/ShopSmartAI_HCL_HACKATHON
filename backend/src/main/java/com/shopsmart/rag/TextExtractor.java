package com.shopsmart.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsmart.rag.Chunker.Section;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Turns PDF / Markdown / JSON bytes into named sections. IllegalArgumentException messages are safe to show admins. */
@Component
public class TextExtractor {
    private static final int MAX_PDF_PAGES = 200;
    private final ObjectMapper mapper;

    public TextExtractor(ObjectMapper mapper) { this.mapper = mapper; }

    public List<Section> extract(String type, byte[] bytes) {
        return switch (type) {
            case "PDF" -> pdf(bytes);
            case "MARKDOWN" -> markdown(new String(bytes, StandardCharsets.UTF_8));
            case "JSON" -> json(bytes);
            default -> throw new IllegalArgumentException("Unsupported file type");
        };
    }

    private List<Section> pdf(byte[] b) {
        if (b.length < 5 || b[0] != '%' || b[1] != 'P' || b[2] != 'D' || b[3] != 'F') {
            throw new IllegalArgumentException("File is not a valid PDF");
        }
        List<Section> out = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(b)) {
            if (doc.getNumberOfPages() > MAX_PDF_PAGES) throw new IllegalArgumentException("PDF has more than " + MAX_PDF_PAGES + " pages");
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String t = stripper.getText(doc);
                if (!t.isBlank()) out.add(new Section("Page " + p, p, t));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the PDF (encrypted or corrupted?)");
        }
        return out;
    }

    /** Section name = heading path, e.g. "Membership Benefits > Prime Delivery". */
    private List<Section> markdown(String md) {
        List<Section> out = new ArrayList<>();
        String[] path = new String[7];
        String heading = "Introduction";
        StringBuilder buf = new StringBuilder();
        boolean fence = false;
        for (String line : md.replace("\r", "").split("\n", -1)) {
            String t = line.stripLeading();
            if (t.startsWith("```")) fence = !fence;
            int level = 0;
            while (!fence && level < t.length() && t.charAt(level) == '#') level++;
            if (!fence && level >= 1 && level <= 6 && t.length() > level && t.charAt(level) == ' ') {
                flush(out, heading, buf);
                path[level] = t.substring(level).strip();
                for (int i = level + 1; i < 7; i++) path[i] = null;
                StringBuilder h = new StringBuilder();
                for (int i = 1; i <= level; i++) if (path[i] != null) h.append(h.length() > 0 ? " > " : "").append(path[i]);
                heading = h.toString();
            } else {
                buf.append(line).append('\n');
            }
        }
        flush(out, heading, buf);
        return out;
    }

    private static void flush(List<Section> out, String heading, StringBuilder buf) {
        if (!buf.toString().isBlank()) out.add(new Section(heading, null, buf.toString()));
        buf.setLength(0);
    }

    private List<Section> json(byte[] b) {
        List<Section> out = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(b);
            if (root == null || root.isMissingNode() || root.isNull()) throw new IllegalArgumentException("JSON file is empty");
            if (root.isObject()) {
                for (Map.Entry<String, JsonNode> e : root.properties()) {
                    out.add(new Section(e.getKey(), null, e.getKey() + ": " + render(e.getValue())));
                }
            } else if (root.isArray()) {
                int i = 0;
                for (JsonNode n : root) {
                    i++;
                    String name = n.isObject() ? firstText(n, "title", "name", "section", "id") : null;
                    out.add(new Section(name != null ? name : "Item " + i, null, render(n)));
                }
            } else {
                out.add(new Section("Document", null, root.asText()));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("File is not valid JSON");
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the JSON file");
        }
        return out;
    }

    private String render(JsonNode n) throws JsonProcessingException {
        return n.isValueNode() ? n.asText() : mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }

    private static String firstText(JsonNode obj, String... keys) {
        for (String k : keys) {
            JsonNode v = obj.get(k);
            if (v != null && v.isValueNode() && !v.asText().isBlank()) return v.asText();
        }
        return null;
    }
}
