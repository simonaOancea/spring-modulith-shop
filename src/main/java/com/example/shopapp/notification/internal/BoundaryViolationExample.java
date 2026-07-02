package com.example.shopapp.notification.internal;

import org.springframework.stereotype.Component;

/**
 * DEMO: Uncomment the import and method below to watch Spring Modulith's verify() fail.
 *
 * The notification module declares allowedDependencies = { "catalog", "order :: events" }.
 * That grants access to catalog's API and to order's "events" named interface only.
 * Reaching into fulfillment's internal package (Shipment) is a dependency the module never
 * declared, so verify() reports an illegal dependency:
 *
 *   Module 'notification' depends on module 'fulfillment' via
 *   com.example.shopapp.notification.internal.BoundaryViolationExample
 *     -> com.example.shopapp.fulfillment.internal.Shipment.
 *   Allowed targets: catalog, order :: events.
 *
 * This is an *illegal dependency*, not a cycle — notification reaches somewhere it isn't
 * allowed, but fulfillment does not depend back on notification. (Contrast the cycle demo
 * in order/internal/OrderProcessor.)
 */
@Component
class BoundaryViolationExample {

    // Uncomment the import (move it to the top of the file, with the other imports) and the
    // method below to break the boundary verification test:
    //
    // import com.example.shopapp.fulfillment.internal.Shipment;
    //
    // void violateBoundary() {
    //     // Reaches into fulfillment's internal package — never declared, not allowed.
    //     Shipment shipment = new Shipment();
    // }
}
