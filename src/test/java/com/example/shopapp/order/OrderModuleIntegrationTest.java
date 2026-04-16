package com.example.shopapp.order;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;

@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestContainersConfiguration.class)
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
}
