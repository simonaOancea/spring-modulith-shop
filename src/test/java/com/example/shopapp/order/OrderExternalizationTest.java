package com.example.shopapp.order;

import com.example.shopapp.TestContainersConfiguration;
import com.example.shopapp.catalog.CatalogService;
import com.example.shopapp.order.internal.MockPaymentConfig;
import com.example.shopapp.catalog.ProductInfo;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "shop.payment-gateway.enabled=false")
@Import({TestContainersConfiguration.class, MockPaymentConfig.class})
class OrderExternalizationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Test
    void orderCompletedEventIsExternalizedToKafka() throws Exception {
        // Given: a product with stock
        ProductInfo product = catalogService.registerProduct("Kafka Test Product", "KFK-001", new BigDecimal("49.99"), 20);

        // When: an order is placed
        orderService.placeOrder("kafka@example.com", product.sku(), 3);

        // Then: the event arrives on the Kafka topic
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of("order-completed"));

            ConsumerRecords<String, String> records = ConsumerRecords.empty();
            for (int i = 0; i < 30; i++) {
                records = consumer.poll(Duration.ofSeconds(1));
                if (!records.isEmpty()) break;
            }

            // Consume all records — other tests may have externalized events to the same topic
            boolean found = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.value().contains("KFK-001")) {
                    assertThat(record.value()).contains("productSku");
                    found = true;
                    break;
                }
            }
            assertThat(found).as("Expected event with KFK-001 on Kafka topic").isTrue();
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
        ));
    }
}
