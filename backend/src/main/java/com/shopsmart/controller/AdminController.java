package com.shopsmart.controller;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.service.CatalogService;
import com.shopsmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Everything under /api/v1/admin/** requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CatalogService catalog;
    private final OrderService orders;

    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest r) { return catalog.createProduct(r); }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest r) {
        return catalog.updateProduct(id, r);
    }

    @PatchMapping("/products/{id}/stock")
    public ProductResponse stock(@PathVariable Long id, @Valid @RequestBody StockRequest r) {
        return catalog.updateStock(id, r.stockQuantity());
    }

    @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) { catalog.deactivate(id); }

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest r) { return catalog.createCategory(r); }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return catalog.updateCategory(id, r);
    }

    @PostMapping("/orders/{id}/status")
    public OrderResponse orderStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return orders.adminSetStatus(id, r.status());
    }
}
