package com.example.shopapp.fulfillment;

import java.math.BigDecimal;

public record ShipmentWithProduct(
        Long shipmentId, Long orderId, String productSku,
        String productName, BigDecimal price, int quantity, String status) {
}
