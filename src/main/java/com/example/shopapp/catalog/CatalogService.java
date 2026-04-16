package com.example.shopapp.catalog;

import com.example.shopapp.catalog.internal.Product;
import com.example.shopapp.catalog.internal.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final ProductRepository products;
    private final ApplicationEventPublisher events;

    @Transactional
    public ProductInfo registerProduct(String name, String sku, BigDecimal price, int initialStock) {
        Product product = new Product(name, sku, price, initialStock);
        products.save(product);

        log.info("Registered product {} ({}), price {}, stock {}", name, sku, price, initialStock);
        events.publishEvent(new ProductRegistered(sku, name));

        return new ProductInfo(product.getSku(), product.getName(), product.getPrice(), product.getStock());
    }

    @Transactional(readOnly = true)
    public ProductInfo getProduct(String sku) {
        Product product = products.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
        return new ProductInfo(product.getSku(), product.getName(), product.getPrice(), product.getStock());
    }

    @Transactional
    public void reserveStock(String sku, int quantity) {
        Product product = products.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
        product.reserveStock(quantity);
        products.save(product);
    }

    @Transactional
    public void releaseStock(String sku, int quantity) {
        Product product = products.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
        product.releaseStock(quantity);
        products.save(product);
    }
}
