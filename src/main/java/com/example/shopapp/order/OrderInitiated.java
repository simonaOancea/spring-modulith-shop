package com.example.shopapp.order;

import java.math.BigDecimal;

public record OrderInitiated(Long orderId, String productSku, int quantity, BigDecimal totalAmount, String customerEmail) {
}
