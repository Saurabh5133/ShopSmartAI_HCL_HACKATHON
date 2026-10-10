package com.shopsmart.controller;

import com.shopsmart.ai.AiErrors;
import com.shopsmart.rag.RagSchemaInitializer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin-only self-check: database, pgvector, indexed data, and one live call each to the embedding and chat model. */
@RestController
@RequestMapping("/api/v1/admin/ai")
public class AiStatusController {
    private final JdbcTemplate jdbc;
    private final EmbeddingModel embeddings;
    private final ChatClient chat;
    private final RagSchemaInitializer rag;
    private final String apiKey;
    private final int dim;

    public AiStatusController(JdbcTemplate jdbc, EmbeddingModel embeddings, ChatClient.Builder builder,
                              RagSchemaInitializer rag, @Value("${spring.ai.openai.api-key}") String apiKey,
                              @Value("${app.rag.embedding-dimension}") int dim) {
        this.jdbc = jdbc;
        this.embeddings = embeddings;
        this.chat = builder.build();
        this.rag = rag;
        this.apiKey = apiKey;
        this.dim = dim;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("geminiKeyConfigured", apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey));
        try {
            r.put("database", "OK (" + jdbc.queryForObject("SELECT current_database()", String.class) + ")");
        } catch (Exception e) {
            r.put("database", "FAILED");
        }
        List<String> ext = jdbc.queryForList("SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
        r.put("pgvector", ext.isEmpty() ? "NOT INSTALLED" : "OK (version " + ext.get(0) + ")");
        r.put("ragStorage", rag.problem() == null ? "OK (vector(" + dim + "))" : rag.problem());
        if (rag.problem() == null) {
            r.put("documentsByStatus", jdbc.queryForList("SELECT status, count(*) AS count FROM documents GROUP BY status"));
            r.put("indexedChunks", jdbc.queryForObject("SELECT count(*) FROM policy_documents", Integer.class));
        }
        try {
            r.put("embeddingTest", "OK (model returned " + embeddings.embed("test").length + " dimensions, configured " + dim + ")");
        } catch (Exception e) {
            r.put("embeddingTest", "FAILED: " + AiErrors.explain(e));
        }
        try {
            String out = chat.prompt(new Prompt(List.of(new UserMessage("Reply with the single word OK")))).call().content();
            r.put("chatTest", "OK (" + (out == null ? "" : out.strip().substring(0, Math.min(40, out.strip().length()))) + ")");
        } catch (Exception e) {
            r.put("chatTest", "FAILED: " + AiErrors.explain(e));
        }
        return r;
    }
}
