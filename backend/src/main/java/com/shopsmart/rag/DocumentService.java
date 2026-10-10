package com.shopsmart.rag;

import com.shopsmart.dto.Dtos.DocumentResponse;
import com.shopsmart.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Admin document management. Upload returns immediately; embedding happens in IngestionService (async). */
@Service
public class DocumentService {
    private static final Set<String> TIERS = Set.of("BASIC", "PRIME", "PREMIUM");
    private static final String SELECT = "SELECT d.id, d.source_name, d.document_type, d.version, d.effective_date, d.region, "
            + "d.product_category, d.membership_tier, d.status, d.status_message, d.created_at, "
            + "(SELECT count(*) FROM policy_documents p WHERE p.document_id = d.id) AS chunk_count FROM documents d";
    private static final RowMapper<DocumentResponse> MAP = (rs, i) -> new DocumentResponse(
            rs.getLong("id"), rs.getString("source_name"), rs.getString("document_type"), rs.getString("version"),
            rs.getObject("effective_date", LocalDate.class), rs.getString("region"), rs.getString("product_category"),
            rs.getString("membership_tier"), rs.getString("status"), rs.getString("status_message"),
            rs.getInt("chunk_count"), rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;
    private final IngestionService ingestion;
    private final RagSchemaInitializer rag;

    public DocumentService(JdbcTemplate jdbc, IngestionService ingestion, RagSchemaInitializer rag) {
        this.jdbc = jdbc;
        this.ingestion = ingestion;
        this.rag = rag;
    }

    public DocumentResponse upload(MultipartFile file, String title, String documentType, String version,
                                   LocalDate effectiveDate, String region, String productCategory, String membershipTier) {
        rag.requireReady();
        if (file == null || file.isEmpty()) throw ApiException.badRequest("EMPTY_FILE", "Please upload a non-empty file");
        String filename = baseName(file.getOriginalFilename());
        String type = detectType(filename);
        documentType = required(documentType, "documentType", 50);
        version = required(version, "version", 50);
        title = blankToNull(title);
        if (title != null && title.length() > 255) throw ApiException.badRequest("VALIDATION_ERROR", "title is too long");
        region = optional(region, "region", 100);
        productCategory = optional(productCategory, "productCategory", 100);
        membershipTier = optional(membershipTier, "membershipTier", 20);
        if (membershipTier != null) {
            membershipTier = membershipTier.toUpperCase(Locale.ROOT);
            if (!TIERS.contains(membershipTier)) throw ApiException.badRequest("VALIDATION_ERROR", "membershipTier must be BASIC, PRIME or PREMIUM");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes(); // read now: the upload temp file disappears after the request
        } catch (IOException e) {
            throw ApiException.badRequest("UPLOAD_FAILED", "Could not read the uploaded file");
        }
        String sourceName = title != null ? title : filename;
        Long id = jdbc.queryForObject("INSERT INTO documents (source_name, document_type, version, effective_date, region, "
                        + "product_category, membership_tier, status, status_message) VALUES (?,?,?,?,?,?,?, 'PROCESSING', 'Ingestion started') RETURNING id",
                Long.class, sourceName, documentType, version, effectiveDate, region, productCategory, membershipTier);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sourceName", sourceName);
        meta.put("fileName", filename);
        meta.put("documentType", documentType);
        meta.put("version", version);
        if (effectiveDate != null) meta.put("effectiveDate", effectiveDate.toString());
        if (region != null) meta.put("region", region);
        if (productCategory != null) meta.put("productCategory", productCategory);
        if (membershipTier != null) meta.put("membershipTier", membershipTier);
        ingestion.ingest(id, type, bytes, meta);
        return get(id);
    }

    public List<DocumentResponse> list() {
        rag.requireReady();
        return jdbc.query(SELECT + " ORDER BY d.created_at DESC, d.id DESC", MAP);
    }

    public DocumentResponse get(Long id) {
        rag.requireReady();
        List<DocumentResponse> r = jdbc.query(SELECT + " WHERE d.id = ?", MAP, id);
        if (r.isEmpty()) throw ApiException.notFound("Document not found");
        return r.get(0);
    }

    /** Chunks and embeddings go with it (ON DELETE CASCADE). */
    public void delete(Long id) {
        rag.requireReady();
        if (jdbc.update("DELETE FROM documents WHERE id = ?", id) == 0) throw ApiException.notFound("Document not found");
    }

    private static String detectType(String filename) {
        String f = filename.toLowerCase(Locale.ROOT);
        if (f.endsWith(".pdf")) return "PDF";
        if (f.endsWith(".md") || f.endsWith(".markdown")) return "MARKDOWN";
        if (f.endsWith(".json")) return "JSON";
        throw ApiException.badRequest("UNSUPPORTED_FILE_TYPE", "Only PDF, Markdown (.md) and JSON files are supported");
    }

    private static String baseName(String name) {
        if (name == null || name.isBlank()) throw ApiException.badRequest("VALIDATION_ERROR", "File name is missing");
        String n = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1).strip();
        if (n.isEmpty() || n.length() > 255) throw ApiException.badRequest("VALIDATION_ERROR", "Invalid file name");
        return n;
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.strip(); }

    private static String optional(String s, String field, int max) {
        s = blankToNull(s);
        if (s != null && s.length() > max) throw ApiException.badRequest("VALIDATION_ERROR", field + " is too long");
        return s;
    }

    private static String required(String s, String field, int max) {
        s = optional(s, field, max);
        if (s == null) throw ApiException.badRequest("VALIDATION_ERROR", field + " is required");
        return s;
    }
}
