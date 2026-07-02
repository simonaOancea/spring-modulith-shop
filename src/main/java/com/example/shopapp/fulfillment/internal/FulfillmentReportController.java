package com.example.shopapp.fulfillment.internal;

import com.example.shopapp.fulfillment.ShipmentWithProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-side endpoints for the fulfillment module.
 *
 * The shipment report drives the data-decoupling demo: it resolves product names through
 * CatalogProductView (@Subselect on catalog.products) — a single-schema, read-only view with
 * zero code coupling (data-decoupling Level 3), which the demo-profile P6Spy guard
 * deliberately lets through.
 *
 * The commented revenue report below is the counter-example: one native query that JOINs
 * three module schemas. Uncomment the block (fully-qualified names — no imports to add),
 * restart, and curl /api/fulfillment/revenue-report to watch AssertQueriesDontJoinSchemas
 * throw BEFORE the statement reaches Postgres. That hidden data-layer coupling is exactly
 * what the compile-time verify() cannot see.
 */
@RestController
@RequestMapping("/api/fulfillment")
@RequiredArgsConstructor
class FulfillmentReportController {

    private final FulfillmentService fulfillmentService;

    @GetMapping("/orders/{orderId}/report")
    List<ShipmentWithProduct> shipmentReport(@PathVariable Long orderId) {
        return fulfillmentService.getShipmentReport(orderId);
    }

    // DEMO: a "harmless" reporting feature that grows a cross-schema JOIN.
    //
    // @jakarta.persistence.PersistenceContext
    // private jakarta.persistence.EntityManager entityManager;
    //
    // @org.springframework.transaction.annotation.Transactional(readOnly = true)
    // @GetMapping("/revenue-report")
    // java.util.List<Object[]> shipmentRevenueReport() {
    //     return entityManager.createNativeQuery("""
    //             SELECT s.id, s.product_sku, p.name, o.total_amount
    //             FROM fulfillment.shipments s
    //             JOIN catalog.products p ON p.sku = s.product_sku
    //             JOIN orders.orders   o ON o.id  = s.order_id
    //             """)
    //             .getResultList();
    // }
}
