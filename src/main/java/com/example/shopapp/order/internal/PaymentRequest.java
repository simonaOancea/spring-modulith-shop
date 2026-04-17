package com.example.shopapp.order.internal;

import java.math.BigDecimal;

record PaymentRequest(String orderId, String customerEmail, BigDecimal amount) {
}
