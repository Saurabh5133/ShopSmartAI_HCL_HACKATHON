package com.shopsmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "categories") @Getter @Setter
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @ManyToOne @JoinColumn(name = "parent_id")
    private Category parent;
}
