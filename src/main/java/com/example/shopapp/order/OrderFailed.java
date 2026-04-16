package com.example.shopapp.order;

import java.math.BigDecimal;

public record OrderFailed(Long orderId, String productSku, int quantity, BigDecimal totalAmount, String reason) {
}
