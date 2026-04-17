package com.example.shopapp.order.internal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MockPaymentConfig {

    @Bean
    PaymentGatewayClient paymentGatewayClient() {
        return request -> new PaymentResponse("txn-mock", "APPROVED", null);
    }
}
