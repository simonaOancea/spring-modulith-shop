package com.example.shopapp.fulfillment.internal;

import com.example.shopapp.fulfillment.ShipmentDispatched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FulfillmentService {

    private final ShipmentRepository shipments;
    private final ApplicationEventPublisher events;

    @Transactional
    public ShipmentDispatched dispatchShipment(Long shipmentId) {
        Shipment shipment = shipments.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        shipment.dispatch();
        shipments.save(shipment);

        ShipmentDispatched dispatched = new ShipmentDispatched(
                shipment.getId(), shipment.getOrderId(), shipment.getCustomerEmail());

        log.info("Dispatched shipment #{} for order #{}", shipmentId, shipment.getOrderId());
        events.publishEvent(dispatched);
        return dispatched;
    }

    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByOrder(Long orderId) {
        return shipments.findByOrderId(orderId);
    }
}
