-- Full outbox history for the order events. Run sheet: A3a step 3, A3b step 6.
-- Narration reads the TAIL of this table ("the last row is the declined order"),
-- so this one must stay in publication order. Never swap it for outbox-newest.sql.
--
-- Formatted for a projector, not for a grid: the listener column collapses to the
-- owning module and completion_date is a time only. Both keep the output at ~62
-- characters wide. Run it with -P null=NULL so unfinished rows print NULL rather
-- than blank -- A3b's narration points at exactly that.
SELECT (serialized_event::json->>'orderId') AS ord,
       replace(event_type, 'com.example.shopapp.order.events.', '') AS event,
       CASE WHEN listener_id LIKE '%DelegatingEventExternalizer%' THEN 'kafka'
            ELSE split_part(replace(listener_id, 'com.example.shopapp.', ''), '.', 1)
       END AS listener,
       status,
       to_char(completion_date, 'HH24:MI:SS') AS done
FROM event_publication
WHERE event_type LIKE 'com.example.shopapp.order.events.%'
ORDER BY publication_date;
