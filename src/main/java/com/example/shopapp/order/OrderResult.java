package com.example.shopapp.order;

import java.math.BigDecimal;

public record OrderResult(Long orderId, String productSku, int quantity, BigDecimal totalAmount, String status) {
}
