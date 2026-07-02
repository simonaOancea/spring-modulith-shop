package com.example.shopapp.fulfillment.internal;

import org.springframework.stereotype.Component;

/**
 * DEMO: a "harmless" reporting helper that grows a cross-schema JOIN.
 *
 * Uncomment the block below (fully-qualified names, so there are no imports to add) and call
 * shipmentRevenueReport() — e.g. from a scratch endpoint — to watch the demo-profile P6Spy guard
 * (AssertQueriesDontJoinSchemas) throw at runtime: the SQL joins fulfillment.shipments to
 * catalog.products and orders.orders in ONE statement, welding three modules together at the
 * data layer.
 *
 * Contrast with CatalogProductView's @Subselect, which reads a SINGLE foreign schema and is
 * deliberately allowed through — that is the difference between a decoupled read and a hidden
 * coupling, and why data coupling is the sneaky one the compile-time check can't see.
 */
@Component
class FulfillmentReporting {

    // @jakarta.persistence.PersistenceContext
    // private jakarta.persistence.EntityManager entityManager;
    //
    // @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
