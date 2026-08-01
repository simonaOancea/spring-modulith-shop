package com.example.shopapp.fulfillment.internal;

import com.example.shopapp.fulfillment.ShipmentDispatched;
import com.example.shopapp.fulfillment.ShipmentWithProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FulfillmentService {

    private final ShipmentRepository shipments;
    private final CatalogProductViewRepository catalogProductViews;
    private final ApplicationEventPublisher events;

    @Transactional
    public void dispatchShipment(Long shipmentId) {
        Shipment shipment = shipments.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        shipment.dispatch();
        shipments.save(shipment);

        log.info("Dispatched shipment #{} for order #{}", shipmentId, shipment.getOrderId());
        events.publishEvent(new ShipmentDispatched(
                shipment.getId(), shipment.getOrderId(), shipment.getCustomerEmail()));
    }

    /**
     * Observation window for the module tests — Scenario's andWaitForStateChange polls module
     * state through it (the repository is package-private). No production callers by design.
     */
    @VisibleForTesting
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByOrder(Long orderId) {
        return shipments.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<ShipmentWithProduct> getShipmentReport(Long orderId) {
        List<Shipment> orderShipments = shipments.findByOrderId(orderId);
        return orderShipments.stream()
                .map(shipment -> {
                    String productName = catalogProductViews.findBySku(shipment.getProductSku())
                            .map(CatalogProductView::getName)
                            .orElse("Unknown");
                    return new ShipmentWithProduct(
                            shipment.getId(), shipment.getOrderId(), shipment.getProductSku(),
                            productName, shipment.getQuantity(), shipment.getStatus().name());
                })
                .toList();
    }
}
