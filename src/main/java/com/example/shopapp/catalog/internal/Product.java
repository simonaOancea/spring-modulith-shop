package com.example.shopapp.catalog.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products", schema = "catalog")
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String sku;

    private BigDecimal price;

    private int stock;

    public Product(String name, String sku, BigDecimal price, int stock) {
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.stock = stock;
    }

    public void reserveStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("Insufficient stock for product " + sku);
        }
        this.stock -= quantity;
    }

    public void releaseStock(int quantity) {
        this.stock += quantity;
    }
}
