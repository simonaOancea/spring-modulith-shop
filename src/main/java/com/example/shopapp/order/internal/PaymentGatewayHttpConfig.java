package com.example.shopapp.order.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

// The group's base URL and timeouts come from spring.http.serviceclient.payment.*
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "payment", types = PaymentGatewayClient.class)
@ConditionalOnProperty(name = "shop.payment-gateway.enabled", havingValue = "true", matchIfMissing = true)
class PaymentGatewayHttpConfig {
}
