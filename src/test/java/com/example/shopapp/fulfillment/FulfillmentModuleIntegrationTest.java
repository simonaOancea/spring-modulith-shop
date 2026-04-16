package com.example.shopapp.fulfillment;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.order.OrderCompleted;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Import(TestContainersConfiguration.class)
class FulfillmentModuleIntegrationTest {

    @Autowired
    private com.example.shopapp.fulfillment.internal.FulfillmentService fulfillmentService;

    @Test
    void orderCompletedCreatesShipment(Scenario scenario) {
        // When: an OrderCompleted event is published
        scenario.publish(new OrderCompleted(1L, "TST-001", 2, new BigDecimal("199.98"), "customer@example.com"))
                .andWaitForStateChange(() -> fulfillmentService.getShipmentsByOrder(1L))
                .andVerify(shipments -> {
                    assertThat(shipments).isNotEmpty();
                });
    }
}
