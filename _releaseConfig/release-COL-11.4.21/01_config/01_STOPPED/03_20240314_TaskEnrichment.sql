UPDATE task_enrichment_field_config
SET
    extra_arguments = 'Processing_Status',
    field_display_name = 'Msg Processing Status'
WHERE
    field_db_name = 'msg_processing_status';

COMMIT;