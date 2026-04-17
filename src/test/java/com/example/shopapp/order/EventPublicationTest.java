package com.example.shopapp.order;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.order.internal.MockPaymentConfig;
import com.example.shopapp.catalog.ProductInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "shop.payment-gateway.enabled=false")
@Import({TestContainersConfiguration.class, MockPaymentConfig.class})
class EventPublicationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void eventPublicationTableTracksEventLifecycle() throws Exception {
        // Given: a product with stock
        ProductInfo product = catalogService.registerProduct("Outbox Test", "OBX-001", new BigDecimal("29.99"), 50);

        // When: an order is placed
        orderService.placeOrder("outbox@example.com", product.sku(), 1);

        // Give async listeners time to complete
        Thread.sleep(2000);

        // Then: inspect the EVENT_PUBLICATION table
        List<Map<String, Object>> publications = jdbc.queryForList(
                "SELECT event_type, listener_id, publication_date, completion_date FROM event_publication ORDER BY publication_date");

        System.out.println("\n=== EVENT_PUBLICATION TABLE ===");
        publications.forEach(row -> System.out.printf("  %-45s | listener=%-60s | completed=%s%n",
                row.get("event_type"), row.get("listener_id"),
                row.get("completion_date") != null ? "YES" : "NO (incomplete!)"));
        System.out.println("================================\n");

        assertThat(publications).isNotEmpty();
    }
}
