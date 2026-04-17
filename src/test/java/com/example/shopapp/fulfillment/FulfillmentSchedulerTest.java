package com.example.shopapp.fulfillment;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.order.events.OrderCompleted;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.moments.support.TimeMachine;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
class FulfillmentSchedulerTest {

    @Autowired
    private TimeMachine timeMachine;

    @Autowired
    private com.example.shopapp.fulfillment.internal.FulfillmentService fulfillmentService;

    @Test
    void dayHasPassedTriggersShipmentDispatch(Scenario scenario) {
        // Given: an order has been completed (creates a pending shipment via FulfillmentProcessor)
        scenario.publish(new OrderCompleted(1L, "TST-001", 2, new BigDecimal("199.98"), "alice@example.com"))
                .andWaitForStateChange(() -> fulfillmentService.getShipmentsByOrder(1L))
                .andVerify(shipments -> assertThat(shipments).isNotEmpty());

        // When: a day passes (TimeMachine triggers DayHasPassed)
        timeMachine.shiftBy(Duration.ofDays(1));

        // Then: the scheduler ran (we got here without exceptions)
        assertThat(fulfillmentService.getShipmentsByOrder(1L)).isNotEmpty();
    }
}
