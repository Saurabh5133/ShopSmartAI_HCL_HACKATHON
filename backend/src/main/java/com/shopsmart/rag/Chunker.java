package com.shopsmart.rag;

import java.util.ArrayList;
import java.util.List;

/** Splits extracted sections into chunks of at most ~1000 characters, never mixing sections. */
public final class Chunker {
    public static final int MAX_CHARS = 1000;

    public record Section(String name, Integer page, String text) {}
    public record Chunk(String section, Integer page, String text) {}

    private Chunker() {}

    public static List<Chunk> chunk(List<Section> sections) {
        List<Chunk> out = new ArrayList<>();
        for (Section s : sections) {
            String body = s.text() == null ? "" : s.text().replace("\r", "").trim();
            if (body.isEmpty()) continue;
            StringBuilder cur = new StringBuilder();
            for (String para : body.split("\n\\s*\n")) {
                para = para.strip();
                if (para.isEmpty()) continue;
                for (String piece : splitLong(para)) {
                    if (cur.length() > 0 && cur.length() + piece.length() + 2 > MAX_CHARS) emit(out, s, cur);
                    if (cur.length() > 0) cur.append("\n\n");
                    cur.append(piece);
                }
            }
            emit(out, s, cur);
        }
        return out;
    }

    // the section name is repeated inside the chunk so the embedding and the LLM both see it
    private static void emit(List<Chunk> out, Section s, StringBuilder cur) {
        if (cur.length() == 0) return;
        out.add(new Chunk(s.name(), s.page(), "Section: " + s.name() + "\n" + cur));
        cur.setLength(0);
    }

    private static List<String> splitLong(String p) {
        List<String> parts = new ArrayList<>();
        while (p.length() > MAX_CHARS) {
            int cut = p.lastIndexOf(' ', MAX_CHARS);
            if (cut < MAX_CHARS / 2) cut = MAX_CHARS;
            parts.add(p.substring(0, cut).strip());
            p = p.substring(cut).strip();
        }
        if (!p.isEmpty()) parts.add(p);
        return parts;
    }
}
