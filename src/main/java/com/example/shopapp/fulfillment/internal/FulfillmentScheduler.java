package com.example.shopapp.fulfillment.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.moments.DayHasPassed;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
class FulfillmentScheduler {

    private final ShipmentRepository shipments;
    private final FulfillmentService fulfillmentService;

    @ApplicationModuleListener
    void on(DayHasPassed event) {
        log.info("Daily fulfillment batch starting");

        List<Shipment> pending = shipments.findByStatus(Shipment.ShipmentStatus.PENDING);

        for (Shipment shipment : pending) {
            fulfillmentService.dispatchShipment(shipment.getId());
        }

        log.info("Dispatched {} shipments", pending.size());
    }
}
