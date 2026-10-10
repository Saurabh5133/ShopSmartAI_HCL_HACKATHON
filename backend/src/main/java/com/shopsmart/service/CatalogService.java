package com.shopsmart.service;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.Category;
import com.shopsmart.entity.Product;
import com.shopsmart.exception.ApiException;
import com.shopsmart.repository.CategoryRepository;
import com.shopsmart.repository.ProductRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;

    // ---------- Public reads ----------
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, Long categoryId, String brand, BigDecimal minPrice,
                                                BigDecimal maxPrice, String sortBy, String sortDir, int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw ApiException.badRequest("INVALID_PRICE_RANGE", "minPrice cannot be greater than maxPrice");
        }
        Set<Long> categoryIds = categoryId == null ? null : categoryWithDescendants(categoryId);
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField(sortBy))
                .and(Sort.by("id"));
        Page<Product> result = products.findAll(filter(search, categoryIds, brand, minPrice, maxPrice),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort));
        return new PageResponse<>(result.getContent().stream().map(ProductResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(products.findById(id).filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product not found")));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> compare(List<Long> ids) {
        if (ids == null || ids.size() < 2 || ids.size() > 4) {
            throw ApiException.badRequest("INVALID_COMPARE", "Provide between 2 and 4 product ids");
        }
        List<Product> found = products.findAllById(new LinkedHashSet<>(ids)).stream().filter(Product::isActive).toList();
        if (found.size() < 2) throw ApiException.notFound("Fewer than 2 of the requested products were found");
        return found.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categoryTree() {
        List<Category> all = categories.findAll();
        Map<Long, CategoryResponse> byId = new LinkedHashMap<>();
        all.forEach(c -> byId.put(c.getId(), CategoryResponse.from(c)));
        List<CategoryResponse> roots = new ArrayList<>();
        byId.values().forEach(c -> {
            if (c.parentId() != null && byId.containsKey(c.parentId())) byId.get(c.parentId()).children().add(c);
            else roots.add(c);
        });
        return roots;
    }

    // ---------- Admin: categories ----------
    public CategoryResponse createCategory(CategoryRequest r) {
        if (categories.existsByName(r.name().trim())) {
            throw ApiException.conflict("CATEGORY_EXISTS", "A category with this name already exists");
        }
        Category c = new Category();
        applyCategory(c, r);
        return CategoryResponse.from(categories.save(c));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest r) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
        if (r.parentId() != null && categoryWithDescendants(id).contains(r.parentId())) {
            throw ApiException.badRequest("INVALID_PARENT", "A category cannot be its own parent or descendant");
        }
        applyCategory(c, r);
        return CategoryResponse.from(categories.save(c));
    }

    private void applyCategory(Category c, CategoryRequest r) {
        c.setName(r.name().trim());
        c.setDescription(r.description());
        c.setParent(r.parentId() == null ? null : categories.findById(r.parentId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_PARENT", "Parent category not found")));
    }

    // ---------- Admin: products ----------
    public ProductResponse createProduct(ProductRequest r) {
        Product p = new Product();
        applyProduct(p, r);
        return ProductResponse.from(products.save(p));
    }

    public ProductResponse updateProduct(Long id, ProductRequest r) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        applyProduct(p, r);
        return ProductResponse.from(products.save(p));
    }

    public ProductResponse updateStock(Long id, int stock) {
        Product p = products.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        p.setStockQuantity(stock);
        return ProductResponse.from(p);
    }

    /** Soft delete so existing orders keep their product references. */
    public void deactivate(Long id) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        p.setActive(false);
    }

    private void applyProduct(Product p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setBrand(r.brand());
        p.setPrice(r.price());
        p.setRating(r.rating());
        p.setStockQuantity(r.stockQuantity());
        p.setImageUrl(r.imageUrl());
        p.setCategory(r.categoryId() == null ? null : categories.findById(r.categoryId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Category not found")));
    }

    // ---------- helpers ----------
    private Set<Long> categoryWithDescendants(Long rootId) {
        List<Category> all = categories.findAll();
        if (all.stream().noneMatch(c -> c.getId().equals(rootId))) throw ApiException.notFound("Category not found");
        Set<Long> ids = new HashSet<>(Set.of(rootId));
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Category c : all) {
                if (c.getParent() != null && ids.contains(c.getParent().getId()) && ids.add(c.getId())) grew = true;
            }
        }
        return ids;
    }

    private static String sortField(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy) {
            case "price" -> "price";
            case "rating" -> "rating";
            case "name" -> "name";
            default -> "createdAt"; // allow-list: never pass client text into ORDER BY
        };
    }

    private static Specification<Product> filter(String search, Set<Long> categoryIds, String brand,
                                                 BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Product, Category> cat = root.join("category", JoinType.LEFT);
            ps.add(cb.isTrue(root.get("active")));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("brand"), "")), like),
                        cb.like(cb.lower(cb.coalesce(cat.<String>get("name"), "")), like)));
            }
            if (categoryIds != null) ps.add(cat.get("id").in(categoryIds));
            if (brand != null && !brand.isBlank()) ps.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            if (min != null) ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), min));
            if (max != null) ps.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), max));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
