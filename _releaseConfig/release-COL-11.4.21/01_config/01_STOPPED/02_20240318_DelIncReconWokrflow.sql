DELETE FROM wfw_transition
WHERE
        process_org_id = 0
    AND event_class = 'PSEventMessage'
    AND product_type = 'ALL'
    AND msg_type = 'INC_RECON';

COMMIT;