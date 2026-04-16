package com.example.shopapp.fulfillment;

public record ShipmentDispatched(Long shipmentId, Long orderId, String customerEmail) {
}
