package com.example.shopapp.catalog.internal;

import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
class CatalogController {

    private final CatalogService catalogService;

    record RegisterProductRequest(
            String name,
            String sku,
            BigDecimal price,
            int initialStock
    ) {}

    @PostMapping
    ResponseEntity<ProductInfo> registerProduct(@RequestBody RegisterProductRequest request) {
        ProductInfo product = catalogService.registerProduct(
                request.name(), request.sku(), request.price(), request.initialStock());
        return ResponseEntity.created(URI.create("/api/products/" + product.sku()))
                .body(product);
    }

    @GetMapping("/{sku}")
    ResponseEntity<ProductInfo> getProduct(@PathVariable String sku) {
        return ResponseEntity.ok(catalogService.getProduct(sku));
    }
}
