package com.example.shopapp.fulfillment.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.math.BigDecimal;

/**
 * Read-only view into catalog.products — provides product data to the fulfillment
 * module without creating a code dependency on the catalog module.
 *
 * Cross-module query strategies demonstrated in this project:
 * 1. Direct service call: order -> catalog (creates code coupling, used when write access needed)
 * 2. Database VIEW: fulfillment -> catalog.products via @Subselect (read-only, no code coupling)
 * 3. Event-carried state: OrderCompleted carries productSku, quantity, etc. (most decoupled)
 */
@Entity
@Immutable
@Subselect("SELECT id, sku, name, price FROM catalog.products")
public class CatalogProductView {

    @Id
    private Long id;

    private String sku;
    private String name;
    private BigDecimal price;

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
