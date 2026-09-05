package com.example.shopapp.fulfillment.internal;

import com.example.shopapp.order.events.OrderCancelled;
import com.example.shopapp.order.events.OrderCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
class FulfillmentProcessor {

    private final ShipmentRepository shipments;
    private final FulfillmentProperties demoProperties;

    // Outbox delivery is at-least-once: a crash between this listener's commit and the
    // publication's completion mark redelivers the event. So the listener is idempotent —
    // a redelivered OrderCompleted must not create a second shipment.
    @ApplicationModuleListener
    void on(OrderCompleted event) {
        if (!shipments.findByOrderId(event.orderId()).isEmpty()) {
            log.info("Shipment for order #{} already exists — redelivery ignored", event.orderId());
            return;
        }

        if (demoProperties.shouldFailNow()) {
            log.warn("DEMO: failing OrderCompleted listener for order #{} — its outbox row stays incomplete", event.orderId());
            throw new IllegalStateException("Simulated fulfillment failure for outbox demo (order #" + event.orderId() + ")");
        }

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

    @ApplicationModuleListener
    void on(OrderCancelled event) {
        List<Shipment> orderShipments = shipments.findByOrderId(event.orderId());

        for (Shipment shipment : orderShipments) {
            if (shipment.getStatus() == Shipment.ShipmentStatus.PENDING) {
                shipment.cancel();
                shipments.save(shipment);
                log.info("Shipment #{} cancelled for order #{}", shipment.getId(), event.orderId());
            }
        }
    }
}
