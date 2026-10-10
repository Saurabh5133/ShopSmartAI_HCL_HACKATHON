package com.shopsmart.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gives the knowledge assistant data out of the box: if there is no usable policy document yet, the bundled sample
 * (samples/shopsmart-policies.md) is ingested in the background. Disable with app.rag.seed-sample=false.
 */
@Component
@Order(2)
@Slf4j
public class SampleKnowledgeSeeder implements ApplicationRunner {
    private static final String TITLE = "ShopSmart Policies";
    private final JdbcTemplate jdbc;
    private final IngestionService ingestion;
    private final RagSchemaInitializer rag;
    private final boolean enabled;
    private final String apiKey;

    public SampleKnowledgeSeeder(JdbcTemplate jdbc, IngestionService ingestion, RagSchemaInitializer rag,
                                 @Value("${app.rag.seed-sample:true}") boolean enabled,
                                 @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.jdbc = jdbc;
        this.ingestion = ingestion;
        this.rag = rag;
        this.enabled = enabled;
        this.apiKey = apiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (rag.problem() != null) {
            log.warn("Sample policies not indexed because RAG is unavailable: {}", rag.problem());
            return;
        }
        if (apiKey == null || apiKey.isBlank() || "not-configured".equals(apiKey)) {
            log.warn("Sample policies not indexed: set GEMINI_API_KEY and restart (they are indexed automatically).");
            return;
        }
        try {
            Integer usable = jdbc.queryForObject("SELECT count(*) FROM documents WHERE status <> 'FAILED'", Integer.class);
            if (usable != null && usable > 0) return; // admin already uploaded something
            jdbc.update("DELETE FROM documents WHERE status = 'FAILED' AND source_name = ?", TITLE); // retry cleanly
            byte[] bytes;
            try (InputStream in = new ClassPathResource("samples/shopsmart-policies.md").getInputStream()) {
                bytes = in.readAllBytes();
            }
            Long id = jdbc.queryForObject("INSERT INTO documents (source_name, document_type, version, status, status_message) "
                    + "VALUES (?, 'POLICY', '2026.1', 'PROCESSING', 'Ingestion started') RETURNING id", Long.class, TITLE);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("sourceName", TITLE);
            meta.put("fileName", "shopsmart-policies.md");
            meta.put("documentType", "POLICY");
            meta.put("version", "2026.1");
            ingestion.ingest(id, "MARKDOWN", bytes, meta);
            log.info("Sample policy document queued for indexing (id {})", id);
        } catch (Exception e) {
            log.warn("Could not seed the sample policy document: {}", e.toString());
        }
    }
}
