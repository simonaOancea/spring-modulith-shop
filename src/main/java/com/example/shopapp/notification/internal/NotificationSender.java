package com.example.shopapp.notification.internal;

import com.example.shopapp.catalog.ProductRegistered;
import com.example.shopapp.order.OrderCompleted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class NotificationSender {

    @ApplicationModuleListener
    void on(ProductRegistered event) {
        log.info("New product listed: {} ({})", event.name(), event.sku());
    }

    @ApplicationModuleListener
    void on(OrderCompleted event) {
        log.info("Sending order confirmation to {} for order #{}",
                event.customerEmail(), event.orderId());
    }
}
