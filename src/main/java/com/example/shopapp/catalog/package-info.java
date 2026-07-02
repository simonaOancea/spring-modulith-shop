// For the live cycle demo (see catalog/internal/CycleExample.java): temporarily change
// allowedDependencies to { "order" } so verify() reports a CYCLE rather than an illegal
// dependency, then restore to {} afterwards.
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {}
)
package com.example.shopapp.catalog;
