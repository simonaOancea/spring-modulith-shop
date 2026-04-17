package com.example.shopapp.order.internal;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@Profile("demo")
class PaymentGatewayStub {

    @PostMapping("/charge")
    PaymentResponse charge(@RequestBody PaymentRequest request) {
        if (request.amount().compareTo(new BigDecimal("5000")) > 0) {
            return new PaymentResponse(null, "DECLINED", "Amount exceeds limit");
        }
        return new PaymentResponse("txn-" + System.currentTimeMillis(), "APPROVED", null);
    }
}
