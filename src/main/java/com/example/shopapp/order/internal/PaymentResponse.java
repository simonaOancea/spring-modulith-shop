package com.example.shopapp.order.internal;

public record PaymentResponse(String transactionId, String status, String declineReason) {

    boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }
}
