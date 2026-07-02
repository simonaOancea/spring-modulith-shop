package com.example.shopapp.order.internal;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface PaymentGatewayClient {

    @PostExchange("/api/payments/charge")
    PaymentResponse charge(@RequestBody PaymentRequest request);
}
