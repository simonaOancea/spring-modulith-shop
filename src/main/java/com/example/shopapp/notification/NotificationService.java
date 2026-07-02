package com.example.shopapp.notification;

import com.example.shopapp.notification.internal.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Public API of the notification module.
 *
 * Also the anchor of the live cycle demo: order/internal/OrderProcessor carries a commented
 * direct call to sendOrderConfirmation(...). Uncommenting it creates order -> notification
 * while notification -> order :: events already exists (NotificationSender listens to order
 * events), and ApplicationModules.verify() rejects the resulting cycle. The production path
 * stays event-driven — OrderCompleted -> NotificationSender — same behavior, one direction.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSender sender;

    public void sendOrderConfirmation(String customerEmail, Long orderId) {
        sender.sendOrderConfirmation(customerEmail, orderId);
    }
}
