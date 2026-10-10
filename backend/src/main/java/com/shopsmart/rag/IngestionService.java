package com.shopsmart.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.shopsmart.ai.AiErrors;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Background pipeline: extract -> chunk -> embed -> store. Sets the document to COMPLETED or FAILED. */
@Service
@Slf4j
public class IngestionService {
    private static final int BATCH = 16;
    private static final int MAX_CHUNKS = 500;
    private static final String INSERT = "INSERT INTO policy_documents (document_id, content, chunk_index, embedding, metadata) "
            + "VALUES (?, ?, ?, ?::vector, ?::jsonb)";

    private final JdbcTemplate jdbc;
    private final EmbeddingModel embeddings;
    private final TextExtractor extractor;
    private final ObjectMapper mapper;
    private final int dim;

    public IngestionService(JdbcTemplate jdbc, EmbeddingModel embeddings, TextExtractor extractor, ObjectMapper mapper,
                            @Value("${app.rag.embedding-dimension}") int dim) {
        this.jdbc = jdbc;
        this.embeddings = embeddings;
        this.extractor = extractor;
        this.mapper = mapper;
        this.dim = dim;
    }

    @Async
    public void ingest(long docId, String type, byte[] bytes, Map<String, Object> docMeta) {
        try {
            List<Chunker.Chunk> chunks = Chunker.chunk(extractor.extract(type, bytes));
            if (chunks.isEmpty()) throw new IllegalArgumentException("No extractable text found in the file");
            if (chunks.size() > MAX_CHUNKS) throw new IllegalArgumentException("Document is too large (over " + MAX_CHUNKS + " chunks)");

            List<float[]> vectors = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i += BATCH) {
                List<String> texts = chunks.subList(i, Math.min(i + BATCH, chunks.size())).stream().map(Chunker.Chunk::text).toList();
                List<float[]> batch;
                try {
                    batch = embeddings.embed(texts);
                } catch (Exception first) {
                    if (texts.size() == 1) throw first;
                    log.warn("Batch embedding failed ({}); retrying one by one", AiErrors.explain(first));
                    batch = new ArrayList<>();
                    for (String t : texts) batch.add(embeddings.embed(t));
                }
                if (batch.size() != texts.size()) throw new IllegalStateException("embedding count mismatch");
                for (float[] v : batch) vectors.add(Vectors.fit(v, dim));
            }

            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Chunker.Chunk c = chunks.get(i);
                Map<String, Object> meta = new LinkedHashMap<>(docMeta);
                meta.put("section", c.section());
                if (c.page() != null) meta.put("page", c.page());
                rows.add(new Object[]{docId, c.text(), i, Vectors.literal(vectors.get(i)), mapper.writeValueAsString(meta)});
            }
            jdbc.batchUpdate(INSERT, rows);
            setStatus(docId, "COMPLETED", chunks.size() + " chunks indexed");
            log.info("Document {} ingested ({} chunks)", docId, chunks.size());
        } catch (IllegalArgumentException e) {
            fail(docId, e.getMessage()); // our own, admin-safe messages
        } catch (Exception e) {
            log.error("Ingestion of document {} failed", docId, e);
            fail(docId, "Ingestion failed: " + AiErrors.explain(e));
        }
    }

    private void fail(long docId, String message) {
        try {
            jdbc.update("DELETE FROM policy_documents WHERE document_id = ?", docId);
            setStatus(docId, "FAILED", message);
        } catch (Exception e) {
            log.error("Could not mark document {} as failed", docId, e);
        }
    }

    private void setStatus(long docId, String status, String message) {
        jdbc.update("UPDATE documents SET status = ?, status_message = ?, updated_at = now() WHERE id = ?", status, message, docId);
    }
}
