package com.example.shopapp.fulfillment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CatalogProductViewRepository extends JpaRepository<CatalogProductView, Long> {

    Optional<CatalogProductView> findBySku(String sku);
}
