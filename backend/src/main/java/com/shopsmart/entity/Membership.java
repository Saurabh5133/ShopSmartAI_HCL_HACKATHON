package com.shopsmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "memberships") @Getter @Setter
public class Membership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MembershipTier tier;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MembershipStatus status;
    @Column(nullable = false)
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
