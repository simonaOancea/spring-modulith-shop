package com.example.shopapp.fulfillment;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.fulfillment.internal.Shipment;
import com.example.shopapp.order.OrderCompleted;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.moments.support.TimeMachine;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
class FulfillmentSchedulerTest {

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TimeMachine timeMachine;

    @Autowired
    private com.example.shopapp.fulfillment.internal.FulfillmentService fulfillmentService;

    @Test
    void dayHasPassedTriggersShipmentDispatch() {
        // Given: some orders have been fulfilled (shipments created)
        events.publishEvent(new OrderCompleted(1L, "TST-001", 2, new BigDecimal("199.98"), "alice@example.com"));
        events.publishEvent(new OrderCompleted(2L, "TST-002", 1, new BigDecimal("49.99"), "bob@example.com"));

        // When: a day passes (TimeMachine triggers DayHasPassed)
        timeMachine.shiftBy(Duration.ofDays(1));

        // Then: shipments should have been dispatched
        List<Shipment> shipments = fulfillmentService.getShipmentsByOrder(1L);
        assertThat(shipments).isNotEmpty();
    }
}
