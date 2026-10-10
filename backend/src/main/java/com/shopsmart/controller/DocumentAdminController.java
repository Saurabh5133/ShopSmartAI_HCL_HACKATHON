package com.shopsmart.controller;

import com.shopsmart.dto.Dtos.DocumentResponse;
import com.shopsmart.rag.DocumentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;

/** Knowledge-document management. Under /api/v1/admin/** so SecurityConfig already requires ROLE_ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/documents")
public class DocumentAdminController {
    private final DocumentService documents;

    public DocumentAdminController(DocumentService documents) { this.documents = documents; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam("version") String version,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "effectiveDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "productCategory", required = false) String productCategory,
            @RequestParam(value = "membershipTier", required = false) String membershipTier) {
        DocumentResponse d = documents.upload(file, title, documentType, version, effectiveDate, region, productCategory, membershipTier);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(d);
    }

    @GetMapping
    public List<DocumentResponse> list() { return documents.list(); }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable Long id) { return documents.get(id); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { documents.delete(id); }
}
