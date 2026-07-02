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
 *
 * This is data-decoupling Level 3 (schema-per-module + @Subselect read): it crosses a schema
 * boundary but creates zero code coupling and stays a single-table read — so the P6Spy
 * cross-schema guard (demo profile) correctly lets it through.
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
