package com.example.shopapp.notification.internal;

import org.springframework.stereotype.Component;

/**
 * DEMO: Uncomment the method below to see Spring Modulith's verify() catch the violation.
 *
 * The notification module declares allowedDependencies = {"catalog", "order"}.
 * Importing Shipment from fulfillment's internal package violates this boundary.
 */
@Component
class BoundaryViolationExample {

    // Uncomment the method and import below to break the boundary verification test:
    //
    // import com.example.shopapp.fulfillment.internal.Shipment;
    //
    // public void violateBoundary() {
    //     // This reaches into fulfillment's internal package — not allowed!
    //     Shipment s = new Shipment();
    // }
}
