package com.example.shopapp.fulfillment;

public record ShipmentWithProduct(
        Long shipmentId, Long orderId, String productSku,
        String productName, int quantity, String status) {
}
