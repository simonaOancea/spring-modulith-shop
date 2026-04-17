@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "catalog", "order :: events" }
)
package com.example.shopapp.notification;
