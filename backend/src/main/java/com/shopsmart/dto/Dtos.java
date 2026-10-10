package com.shopsmart.dto;

import com.shopsmart.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** All request/response DTOs in one place to keep the project easy to read. */
public final class Dtos {
    private Dtos() {}

    // ---------- Auth ----------
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserResponse(Long id, String name, String email, Role role, MembershipTier membershipTier) {}

    public record LoginResponse(String accessToken, String tokenType, UserResponse user) {}

    // ---------- Categories ----------
    public record CategoryRequest(@NotBlank @Size(max = 100) String name, String description, Long parentId) {}

    public record CategoryResponse(Long id, String name, String description, Long parentId,
                                   List<CategoryResponse> children) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                    c.getParent() == null ? null : c.getParent().getId(), new ArrayList<>());
        }
    }

    // ---------- Products ----------
    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            Long categoryId,
            @Size(max = 100) String brand,
            String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @DecimalMin("0.0") @DecimalMax("5.0") @Digits(integer = 1, fraction = 1) BigDecimal rating,
            @NotNull @Min(0) Integer stockQuantity,
            @Size(max = 500) String imageUrl) {}

    public record StockRequest(@NotNull @Min(0) Integer stockQuantity) {}

    public record ProductResponse(Long id, String name, Long categoryId, String categoryName, String brand,
                                  String description, BigDecimal price, BigDecimal rating, int stockQuantity,
                                  String availability, String imageUrl) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.getCategory() == null ? null : p.getCategory().getName(),
                    p.getBrand(), p.getDescription(), p.getPrice(), p.getRating(), p.getStockQuantity(),
                    p.getStockQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK", p.getImageUrl());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    // ---------- Wishlist / preferences ----------
    public record WishlistRequest(@NotNull Long productId) {}

    public record PreferencesRequest(List<String> preferredCategories, List<String> preferredBrands,
                                     @DecimalMin("0") BigDecimal budgetMin, @DecimalMin("0") BigDecimal budgetMax,
                                     List<String> useCases) {}

    // ---------- Cart ----------
    public record CartItemRequest(@NotNull Long productId, @NotNull @Min(1) @Max(100) Integer quantity) {}

    public record CartItemUpdate(@NotNull @Min(1) @Max(100) Integer quantity) {}

    public record CartItemResponse(Long itemId, ProductResponse product, BigDecimal unitPrice, int quantity,
                                   BigDecimal subtotal) {}

    public record CartResponse(Long cartId, List<CartItemResponse> items, BigDecimal totalAmount) {}

    // ---------- Orders ----------
    public record OrderRequest(@NotBlank String deliveryAddress, @Size(max = 30) String deliveryMethod) {}

    public record StatusRequest(@NotNull OrderStatus status) {}

    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal priceAtPurchase) {}

    public record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount, String deliveryAddress,
                                String deliveryMethod, Instant createdAt, List<OrderItemResponse> items) {}

    public record OrderEventResponse(OrderStatus status, String description, Instant createdAt) {}

    public record TrackingResponse(Long orderId, OrderStatus currentStatus, List<OrderEventResponse> events) {}

    // ---------- Membership ----------
    public record MembershipRequest(@NotNull MembershipTier tier) {}

    public record RenewRequest(@NotNull MembershipTier tier, @NotNull Integer durationDays) {}

    public record MembershipResponse(Long id, MembershipTier tier, MembershipStatus status, Instant startDate,
                                     Instant endDate) {
        public static MembershipResponse from(Membership m) {
            return new MembershipResponse(m.getId(), m.getTier(), m.getStatus(), m.getStartDate(), m.getEndDate());
        }
    }

    // ---------- AI recommendations ----------
    public record RecommendationRequest(@NotBlank @Size(max = 500) String query) {}

    public record RecommendationItem(Long productId, String productName, String reason, String availability,
                                     BigDecimal price) {}

    public record RecommendationResponse(String interpretedRequirement, BigDecimal budget,
                                         List<RecommendationItem> recommendations, String clarifyingQuestion) {}

    // ---- RAG (Phase 7) ----
    public record DocumentResponse(Long id, String title, String documentType, String version,
                                   java.time.LocalDate effectiveDate, String region, String productCategory,
                                   String membershipTier, String status, String statusMessage, int chunkCount,
                                   java.time.OffsetDateTime createdAt) {}
    public record KnowledgeRequest(@NotBlank @Size(max = 500) String question) {}
    public record SourceItem(String document, String section, String version) {}
    public record KnowledgeResponse(String answer, String confidence, List<SourceItem> sources, boolean followUpRequired) {}
}
