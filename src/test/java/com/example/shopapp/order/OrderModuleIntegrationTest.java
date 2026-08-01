package com.example.shopapp.order;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import com.example.shopapp.order.events.OrderCancelled;
import com.example.shopapp.order.events.OrderCompleted;
import com.example.shopapp.order.internal.MockPaymentConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@Import({TestContainersConfiguration.class, MockPaymentConfig.class})
@TestPropertySource(properties = "shop.payment-gateway.enabled=false")
class OrderModuleIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private OrderService orderService;

    @Test
    void completingOrderPublishesEvents(Scenario scenario) {
        // Given: a product with stock
        ProductInfo product = catalogService.registerProduct("Test Product", "TST-001", new BigDecimal("99.99"), 10);

        // When: an order is placed
        // Then: OrderCompleted event is published
        scenario.stimulate(() -> orderService.placeOrder("customer@example.com", product.sku(), 2))
                .andWaitForEventOfType(OrderCompleted.class)
                .matchingMappedValue(OrderCompleted::productSku, "TST-001")
                .toArrive();
    }

    @Test
    void placingOrderForUnknownProductFailsCleanly() {
        // Regression: getProduct throwing through CatalogService's @Transactional proxy used
        // to mark fail()'s transaction rollback-only -> UnexpectedRollbackException -> 500.
        OrderResult result = orderService.placeOrder("ghost@example.com", "NO-SUCH-SKU", 1);

        assertThat(result.status()).isEqualTo("FAILED");
    }

    @Test
    void cancellingOrderReleasesStockAndPublishesEvent(Scenario scenario) {
        // Given: a product with stock and a completed order
        ProductInfo product = catalogService.registerProduct("Cancel Test", "CNC-001", new BigDecimal("49.99"), 10);
        OrderResult order = orderService.placeOrder("customer@example.com", product.sku(), 3);
        assertThat(order.status()).isEqualTo("COMPLETED");

        // Stock should be reduced
        ProductInfo afterOrder = catalogService.getProduct(product.sku());
        assertThat(afterOrder.stock()).isEqualTo(7);

        // When: the order is cancelled
        // Then: OrderCancelled event is published and stock is restored
        scenario.stimulate(() -> orderService.cancelOrder(order.orderId()))
                .andWaitForEventOfType(OrderCancelled.class)
                .matchingMappedValue(OrderCancelled::orderId, order.orderId())
                .toArrive();

        ProductInfo afterCancel = catalogService.getProduct(product.sku());
        assertThat(afterCancel.stock()).isEqualTo(10);
    }
}
