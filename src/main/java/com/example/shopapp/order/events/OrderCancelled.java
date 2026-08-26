package com.example.shopapp.order.events;

import java.math.BigDecimal;

public record OrderCancelled(
        Long orderId,
        String productSku,
        int quantity,
        BigDecimal totalAmount,
        String customerEmail
) {
}
