package com.example.shopapp.notification.internal;

import org.springframework.stereotype.Component;

/**
 * DEMO: Uncomment the method below — select the commented lines and hit Cmd-/ — to watch
 * Spring Modulith's verify() fail. The type is fully qualified on purpose: there is no import
 * to move, so breaking the build and repairing it are one keystroke each. (Same shape as the
 * cross-schema JOIN block in fulfillment/internal/FulfillmentReportController.)
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

//     void violateBoundary() {
//         com.example.shopapp.fulfillment.internal.Shipment shipment =
//                 new com.example.shopapp.fulfillment.internal.Shipment();
//     }
}
