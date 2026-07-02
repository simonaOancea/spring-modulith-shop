package com.example.shopapp.catalog.internal;

import org.springframework.stereotype.Component;

/**
 * DEMO: create a dependency CYCLE and watch Spring Modulith's verify() refuse it.
 *
 * order already depends on catalog (order -> catalog). If catalog also depends on order
 * (catalog -> order), the two modules form a cycle. To make verify() report a *cycle*
 * rather than a plain illegal dependency, catalog must first be allowed to see order:
 *
 *   1. In catalog/package-info.java, temporarily set:
 *          allowedDependencies = { "order" }
 *   2. Move the import below to the top of this file and uncomment the field + constructor.
 *   3. Run:
 *          ./mvnw -q -o test -Dtest=ModularStructureTest
 *      verify() now fails with a cycle, e.g.:
 *          Cycle detected: Slice catalog -> Slice order -> Slice catalog
 *   4. Recomment the field/import here AND restore catalog/package-info.java to
 *      allowedDependencies = {}. (If you only recomment here but leave the bump, the build
 *      stays green but the module graph is silently wrong for later demos.)
 *
 * Contrast with BoundaryViolationExample: that is an *illegal dependency* (notification
 * reaching fulfillment.internal — one-way). This is a *cycle* (catalog <-> order — mutual).
 * We reference the public OrderService (the order module's API), not an order.internal type,
 * so once catalog is allowed to depend on order it is a clean cycle and not a second illegal
 * dependency.
 *
 * The cleanest of the six cycle fixes to resolve live: invert the dependency with a domain
 * event — delete the direct call and publishEvent(...) instead, reusing the same
 * @ApplicationModuleListener + outbox machinery the rest of the talk shows. Every fix is a
 * two-way door.
 */
@Component
class CycleExample {

    // import com.example.shopapp.order.OrderService;   // <- move to top of file
    //
    // private final OrderService orderService;
    //
    // CycleExample(OrderService orderService) {
    //     this.orderService = orderService;
    // }
}
