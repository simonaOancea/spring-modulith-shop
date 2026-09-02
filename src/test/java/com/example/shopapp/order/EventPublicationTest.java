package com.example.shopapp.order;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.catalog.ProductInfo;
import com.example.shopapp.order.events.OrderCompleted;
import com.example.shopapp.order.events.OrderInitiated;
import com.example.shopapp.order.internal.MockPaymentConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    void eventPublicationTableTracksEventLifecycle() {
        // Given: a product with stock
        ProductInfo product = catalogService.registerProduct("Outbox Test", "OBX-001", new BigDecimal("29.99"), 50);

        // When: an order is placed
        OrderResult order = orderService.placeOrder("outbox@example.com", product.sku(), 1);

        // Then: one row per event x listener for this order — OrderInitiated for fraud,
        // OrderCompleted for fulfillment and notification — and every row completed once
        // the async listeners are done
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Map<String, Object>> publications = publicationsFor(order.orderId());

            assertThat(publications).hasSizeGreaterThanOrEqualTo(3);
            assertThat(publications)
                    .extracting(row -> (String) row.get("event_type"))
                    .contains(OrderInitiated.class.getName(), OrderCompleted.class.getName());
            assertThat(publications)
                    .extracting(row -> row.get("completion_date"))
                    .doesNotContainNull();
        });

        // The printed table doubles as the A3a fallback capture
        List<Map<String, Object>> publications = publicationsFor(order.orderId());
        System.out.println("\n=== EVENT_PUBLICATION TABLE ===");
        publications.forEach(row -> System.out.printf("  %-45s | listener=%-60s | completed=%s%n",
                row.get("event_type"), row.get("listener_id"),
                row.get("completion_date") != null ? "YES" : "NO (incomplete!)"));
        System.out.println("================================\n");
    }

    private List<Map<String, Object>> publicationsFor(Long orderId) {
        return jdbc.queryForList(
                "SELECT event_type, listener_id, publication_date, completion_date FROM event_publication "
                        + "WHERE serialized_event::json->>'orderId' = ? ORDER BY publication_date",
                orderId.toString());
    }
}
