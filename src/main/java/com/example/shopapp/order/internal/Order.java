package com.example.shopapp.order.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerEmail;
    private String productSku;
    private int quantity;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant timestamp;

    public Order(String customerEmail, String productSku, int quantity, BigDecimal totalAmount) {
        this.customerEmail = customerEmail;
        this.productSku = productSku;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.INITIATED;
        this.timestamp = Instant.now();
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }

    public enum OrderStatus {
        INITIATED, COMPLETED, FAILED
    }
}
