package com.example.shopapp.order.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-gateway", url = "${shop.payment-gateway.base-url}")
public interface PaymentGatewayClient {

    @PostMapping("/api/payments/charge")
    PaymentResponse charge(@RequestBody PaymentRequest request);
}
