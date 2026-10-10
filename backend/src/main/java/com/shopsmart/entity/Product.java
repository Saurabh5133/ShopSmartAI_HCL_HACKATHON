package com.shopsmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "products") @Getter @Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 100)
    private String brand;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;
    @Column(nullable = false)
    private int stockQuantity;
    @Column(length = 500)
    private String imageUrl;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
