-- Unfinished work only. Run sheet: A3b step 3.
-- Expected on stage: one row, OrderCompleted x fulfillment, status FAILED, done NULL.
-- Run it with -P null=NULL -- the narration is "the completion date is NULL", and
-- psql prints an empty cell without that flag.
SELECT (serialized_event::json->>'orderId') AS ord,
       replace(event_type, 'com.example.shopapp.order.events.', '') AS event,
       CASE WHEN listener_id LIKE '%DelegatingEventExternalizer%' THEN 'kafka'
            ELSE split_part(replace(listener_id, 'com.example.shopapp.', ''), '.', 1)
       END AS listener,
       status,
       to_char(completion_date, 'HH24:MI:SS') AS done
FROM event_publication
WHERE event_type LIKE 'com.example.shopapp.order.events.%'
  AND completion_date IS NULL;
