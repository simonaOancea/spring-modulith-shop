-- Newest first, capped at 8. Run sheet: A6 step 4 ONLY.
-- By Act 6 the filtered table is ~22 rows and the kafka row would be at the bottom
-- of the full-history query. This puts the newest order's rows on top instead.
--
-- Safe here and ONLY here: A3a's narration says "read the TAIL" and "the last row is
-- the declined order", so never use this one in Act 3.
--
-- The kafka row is what the beat points at: 'kafka' sits in the listener column next
-- to fraud, fulfillment and notification, which is the whole line -- Kafka is just one
-- more listener in that table.
SELECT (serialized_event::json->>'orderId') AS ord,
       replace(event_type, 'com.example.shopapp.order.events.', '') AS event,
       CASE WHEN listener_id LIKE '%DelegatingEventExternalizer%' THEN 'kafka'
            ELSE split_part(replace(listener_id, 'com.example.shopapp.', ''), '.', 1)
       END AS listener,
       status,
       to_char(completion_date, 'HH24:MI:SS') AS done
FROM event_publication
WHERE event_type LIKE 'com.example.shopapp.order.events.%'
ORDER BY publication_date DESC
LIMIT 8;
