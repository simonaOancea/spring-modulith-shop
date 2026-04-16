package com.example.shopapp.order.internal;

import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import com.example.shopapp.order.OrderCompleted;
import com.example.shopapp.order.OrderFailed;
import com.example.shopapp.order.OrderInitiated;
import com.example.shopapp.order.OrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessor {

    private final OrderRepository orders;
    private final CatalogService catalogService;
    private final ApplicationEventPublisher events;

    @Transactional
    public OrderResult execute(String customerEmail, String productSku, int quantity) {
        ProductInfo product = catalogService.getProduct(productSku);
        BigDecimal totalAmount = product.price().multiply(BigDecimal.valueOf(quantity));

        Order order = new Order(customerEmail, productSku, quantity, totalAmount);
        orders.save(order);

        log.info("Order #{} initiated: {} x{} for {}", order.getId(), productSku, quantity, customerEmail);
        events.publishEvent(new OrderInitiated(order.getId(), productSku, quantity, totalAmount, customerEmail));

        // Reserve stock — throws if insufficient
        catalogService.reserveStock(productSku, quantity);
        order.complete();
        orders.save(order);

        log.info("Order #{} completed, total {}", order.getId(), totalAmount);
        events.publishEvent(new OrderCompleted(order.getId(), productSku, quantity, totalAmount, customerEmail));

        return new OrderResult(order.getId(), productSku, quantity, totalAmount, "COMPLETED");
    }

    @Transactional
    public OrderResult fail(String customerEmail, String productSku, int quantity, String reason) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        try {
            ProductInfo product = catalogService.getProduct(productSku);
            totalAmount = product.price().multiply(BigDecimal.valueOf(quantity));
        } catch (Exception ignored) {
        }

        Order order = new Order(customerEmail, productSku, quantity, totalAmount);
        order.fail();
        orders.save(order);

        log.warn("Order #{} failed: {}", order.getId(), reason);
        events.publishEvent(new OrderFailed(order.getId(), productSku, quantity, totalAmount, reason));

        return new OrderResult(order.getId(), productSku, quantity, totalAmount, "FAILED");
    }
}
