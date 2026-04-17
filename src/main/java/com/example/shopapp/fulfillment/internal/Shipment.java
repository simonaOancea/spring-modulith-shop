package com.example.shopapp.fulfillment.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "shipments", schema = "fulfillment")
@Getter
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String customerEmail;
    private String productSku;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    private Instant createdAt;

    public Shipment(Long orderId, String customerEmail, String productSku, int quantity) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.productSku = productSku;
        this.quantity = quantity;
        this.status = ShipmentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void dispatch() {
        this.status = ShipmentStatus.DISPATCHED;
    }

    public void cancel() {
        this.status = ShipmentStatus.CANCELLED;
    }

    public enum ShipmentStatus {
        PENDING, DISPATCHED, CANCELLED
    }
}
