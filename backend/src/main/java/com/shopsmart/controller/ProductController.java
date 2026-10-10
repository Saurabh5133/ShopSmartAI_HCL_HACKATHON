package com.shopsmart.controller;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {
    private final CatalogService catalog;

    @GetMapping("/products")
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String brand,
                                              @RequestParam(required = false) BigDecimal minPrice,
                                              @RequestParam(required = false) BigDecimal maxPrice,
                                              @RequestParam(required = false) String sortBy,
                                              @RequestParam(required = false) String sortDir,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return catalog.search(search, categoryId, brand, minPrice, maxPrice, sortBy, sortDir, page, size);
    }

    // declared before /products/{id} for clarity; Spring prefers the literal path anyway
    @GetMapping("/products/compare")
    public List<ProductResponse> compare(@RequestParam List<Long> ids) { return catalog.compare(ids); }

    @GetMapping("/products/{id}")
    public ProductResponse get(@PathVariable Long id) { return catalog.get(id); }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() { return catalog.categoryTree(); }

    @GetMapping("/categories/{id}/products")
    public PageResponse<ProductResponse> byCategory(@PathVariable Long id,
                                                    @RequestParam(required = false) String sortBy,
                                                    @RequestParam(required = false) String sortDir,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int size) {
        return catalog.search(null, id, null, null, null, sortBy, sortDir, page, size);
    }
}
