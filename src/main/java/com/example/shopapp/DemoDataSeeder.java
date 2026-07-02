package com.example.shopapp;

import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import java.math.BigDecimal;

@Configuration
@Slf4j
@Profile("demo")
class DemoDataSeeder {

    @Bean
    ApplicationRunner seedDemoData(CatalogService catalogService) {
        return args -> {
            if (catalogService.findProduct("LAP-001").isPresent()) {
                log.info("Demo products already present — skipping seed");
                return;
            }

            ProductInfo laptop = catalogService.registerProduct(
                    "Laptop Pro 16", "LAP-001", new BigDecimal("1299.99"), 50);

            ProductInfo phone = catalogService.registerProduct(
                    "Phone Ultra", "PHN-001", new BigDecimal("899.99"), 100);

            ProductInfo headphones = catalogService.registerProduct(
                    "Wireless Headphones", "HPH-001", new BigDecimal("249.99"), 200);

            log.info("=== Demo products seeded ===");
            log.info("Laptop:     {} (price: {}, stock: {})", laptop.sku(), laptop.price(), laptop.stock());
            log.info("Phone:      {} (price: {}, stock: {})", phone.sku(), phone.price(), phone.stock());
            log.info("Headphones: {} (price: {}, stock: {})", headphones.sku(), headphones.price(), headphones.stock());
            log.info("=============================");
        };
    }

    // Pre-create the externalization target so the first @Externalized publish doesn't stall
    // on UNKNOWN_TOPIC_OR_PARTITION while broker-side auto-creation kicks in. Three partitions
    // make the SKU-keyed routing (order-completed::#{#this.productSku()}) visible.
    @Bean
    NewTopic orderCompletedTopic() {
        return TopicBuilder.name("order-completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
