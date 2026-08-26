package com.example.shopapp.order.internal;

import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import com.example.shopapp.order.events.OrderCancelled;
import com.example.shopapp.order.events.OrderCompleted;
import com.example.shopapp.order.events.OrderFailed;
import com.example.shopapp.order.events.OrderInitiated;
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
    private final PaymentGatewayClient paymentGateway;
    private final ApplicationEventPublisher events;

    // DEMO (cycle): uncomment this field and the call at the end of execute(), and set
    // order/package-info.java to allowedDependencies = { "catalog", "notification" }.
    // verify() then reports a CYCLE — order -> notification -> order — because notification
    // already listens to order :: events. The fix is deleting the sync call again: the
    // OrderCompleted event published below drives the same confirmation, in one direction.
    //private final com.example.shopapp.notification.NotificationService notificationService;

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

        // Charge payment via external gateway
        try {
            PaymentResponse payment = paymentGateway.charge(
                    new PaymentRequest(order.getId().toString(), customerEmail, totalAmount));

            if (!payment.isApproved()) {
                catalogService.releaseStock(productSku, quantity);
                return failOrder(order, "Payment declined: " + payment.declineReason());
            }
        } catch (Exception e) {
            catalogService.releaseStock(productSku, quantity);
            return failOrder(order, "Payment error: " + e.getMessage());
        }

        order.complete();
        orders.save(order);

        log.info("Order #{} completed, total {}", order.getId(), totalAmount);
        events.publishEvent(new OrderCompleted(order.getId(), productSku, quantity, totalAmount, customerEmail));

        // DEMO (cycle): the direct-call version of the confirmation — see the field above.
        // notificationService.sendOrderConfirmation(customerEmail, order.getId());

        return new OrderResult(order.getId(), productSku, quantity, totalAmount, "COMPLETED");
    }

    @Transactional
    public OrderResult fail(String customerEmail, String productSku, int quantity, String reason) {
        // findProduct, not getProduct: an exception thrown through CatalogService's
        // @Transactional proxy would mark THIS (joined) transaction rollback-only, and the
        // commit of the FAILED order would then die with UnexpectedRollbackException.
        BigDecimal totalAmount = catalogService.findProduct(productSku)
                .map(product -> product.price().multiply(BigDecimal.valueOf(quantity)))
                .orElse(BigDecimal.ZERO);

        Order order = new Order(customerEmail, productSku, quantity, totalAmount);
        return failOrder(order, reason);
    }

    // Shared failure tail: mark FAILED, log, publish OrderFailed, return the result.
    // Callers release reserved stock themselves — only the payment branches have anything
    // to compensate; after a rollback (OrderService -> fail) there is nothing to release.
    private OrderResult failOrder(Order order, String reason) {
        order.fail();
        orders.save(order);
        log.warn("Order #{} failed: {}", order.getId(), reason);
        events.publishEvent(new OrderFailed(
                order.getId(), order.getProductSku(), order.getQuantity(),
                order.getTotalAmount(), reason));
        return new OrderResult(order.getId(), order.getProductSku(), order.getQuantity(),
                order.getTotalAmount(), "FAILED");
    }

    @Transactional
    public OrderResult cancel(Long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.cancel();
        orders.save(order);

        // Release the reserved stock back to catalog
        catalogService.releaseStock(order.getProductSku(), order.getQuantity());

        log.info("Order #{} cancelled, released {} x{}", orderId, order.getProductSku(), order.getQuantity());
        events.publishEvent(new OrderCancelled(
                order.getId(), order.getProductSku(), order.getQuantity(),
                order.getTotalAmount(), order.getCustomerEmail()));

        return new OrderResult(order.getId(), order.getProductSku(), order.getQuantity(),
                order.getTotalAmount(), "CANCELLED");
    }
}
