package com.shopsmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "order_events") @Getter @Setter
public class OrderEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id")
    private Order order;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private OrderStatus status;
    @Column(columnDefinition = "text")
    private String description;
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
