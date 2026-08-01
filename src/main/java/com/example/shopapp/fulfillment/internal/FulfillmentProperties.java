package com.example.shopapp.fulfillment.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DEMO toggle: when demo.fulfillment.fail-once=true, the OrderCompleted listener throws
 * exactly once. Its row in event_publication stays incomplete (completion_date = NULL),
 * demonstrating the transactional outbox — on restart the unfinished publication is
 * re-delivered and the listener finally succeeds.
 *
 * The one-shot is per JVM, so on the demo restart launch WITHOUT the flag (fail-once=false)
 * so the redelivered event completes instead of failing again.
 */
@Component
@ConfigurationProperties(prefix = "demo.fulfillment")
class FulfillmentProperties {

    /** When true, the next OrderCompleted delivery fails once (then succeeds). */
    @Getter
    @Setter
    private boolean failOnce = false;

    /** Tracks whether the one-shot failure has already fired this JVM run. */
    private final AtomicBoolean alreadyFailed = new AtomicBoolean(false);

    /** Returns true exactly once when fail-once is armed; subsequent calls return false. */
    boolean shouldFailNow() {
        return failOnce && alreadyFailed.compareAndSet(false, true);
    }
}
