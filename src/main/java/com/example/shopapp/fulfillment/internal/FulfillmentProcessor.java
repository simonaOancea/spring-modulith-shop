package com.example.shopapp.fulfillment.internal;

import com.example.shopapp.order.OrderCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class FulfillmentProcessor {

    private final ShipmentRepository shipments;

    @ApplicationModuleListener
    void on(OrderCompleted event) {
        Shipment shipment = new Shipment(
                event.orderId(),
                event.customerEmail(),
                event.productSku(),
                event.quantity()
        );
        shipments.save(shipment);

        log.info("Shipment created for order #{}: {} x{} for {}",
                event.orderId(), event.productSku(), event.quantity(), event.customerEmail());
    }
}
