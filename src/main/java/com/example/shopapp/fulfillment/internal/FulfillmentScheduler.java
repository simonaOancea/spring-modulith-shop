package com.example.shopapp.fulfillment.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.moments.DayHasPassed;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
class FulfillmentScheduler {

    private final ShipmentRepository shipments;
    private final FulfillmentService fulfillmentService;

    // Plain @EventListener, not @ApplicationModuleListener: Moments publishes DayHasPassed from a
    // @Scheduled method with no transaction open, and @ApplicationModuleListener is AFTER_COMMIT —
    // Spring would skip it silently and the batch would never run (spring-modulith issue #1431).
    // Trade-off: no outbox row, so a crash mid-batch is not redelivered; the next day's event
    // picks up whatever is still PENDING.
    @Async
    @EventListener
    void on(DayHasPassed event) {
        log.info("Daily fulfillment batch starting");

        List<Shipment> pending = shipments.findByStatus(Shipment.ShipmentStatus.PENDING);

        for (Shipment shipment : pending) {
            fulfillmentService.dispatchShipment(shipment.getId());
        }

        log.info("Dispatched {} shipments", pending.size());
    }
}
