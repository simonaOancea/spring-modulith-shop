package com.example.shopapp.order.events;

import org.springframework.modulith.events.Externalized;

import java.math.BigDecimal;

@Externalized("order-completed::#{#this.productSku()}")
public record OrderCompleted(
        Long orderId,
        String productSku,
        int quantity,
        BigDecimal totalAmount,
        String customerEmail
) {
}
