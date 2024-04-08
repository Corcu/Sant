BEGIN
	DECLARE
		cnt  NUMBER(32);
	BEGIN
    
        -- Xfer Attr SplitReasonFrom --
        SELECT count(*) into cnt from task_enrichment_field_config where field_db_name ='xfer_attr_split_reason_from';
        
        IF cnt <= 0 THEN
            INSERT INTO task_enrichment_field_config (
                workflow_type,
                data_source_class_name,
                data_source_getter_name,
                custom_class_name,
                field_source,
                field_conversion_class,
                extra_arguments,
                field_display_name,
                field_db_name,
                link_db_name,
                field_domain_finder,
                sql_expression,
                db_type,
                db_scale
            ) VALUES (
                'Transfer',                     --workflow_type
                'com.calypso.tk.bo.BOTransfer', --data_source_class_name
                'getSplitReasonFrom',           --data_source_getter_name
                NULL,                           --custom_class_name
                NULL,                           --field_source
                NULL,                           --field_conversion_class
                NULL,                           --extra_arguments
                'Xfer Attr SplitReasonFrom',    --field_display_name
                'xfer_attr_split_reason_from',  --field_db_name
                NULL,                           --link_db_name
                NULL,                           --field_domain_finder
                NULL,                           --sql_expression
                'string',                       --DB_TYPE
                128                             --DB_SCALE
            );
         
            EXECUTE IMMEDIATE 'ALTER TABLE TASK_ENRICHMENT ADD xfer_attr_split_reason_from VARCHAR2(128)';
         
            EXECUTE IMMEDIATE 'ALTER TABLE TASK_ENRICHMENT_HIST ADD xfer_attr_split_reason_from VARCHAR2(128)';
        
        END IF;
                
        COMMIT;

    END;
END;

/
