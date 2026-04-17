package com.example.shopapp.order.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = PaymentGatewayClient.class)
@ConditionalOnProperty(name = "shop.payment-gateway.enabled", havingValue = "true", matchIfMissing = true)
class FeignConfig {
}
