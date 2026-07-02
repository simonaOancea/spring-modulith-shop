package com.example.shopapp.notification.internal;

import com.example.shopapp.catalog.ProductRegistered;
import com.example.shopapp.order.events.OrderCancelled;
import com.example.shopapp.order.events.OrderCompleted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationSender {

    @ApplicationModuleListener
    void on(ProductRegistered event) {
        log.info("New product listed: {} ({})", event.name(), event.sku());
    }

    @ApplicationModuleListener
    void on(OrderCompleted event) {
        sendOrderConfirmation(event.customerEmail(), event.orderId());
    }

    @ApplicationModuleListener
    void on(OrderCancelled event) {
        log.info("Sending cancellation notice to {} for order #{}",
                event.customerEmail(), event.orderId());
    }

    public void sendOrderConfirmation(String customerEmail, Long orderId) {
        log.info("Sending order confirmation to {} for order #{}", customerEmail, orderId);
    }
}
