ALTER TABLE TASK_ENRICHMENT ADD trade_equity_type varchar2 (128)  NULL;
ALTER TABLE TASK_ENRICHMENT_HIST ADD trade_equity_type varchar2 (128)  NULL;
DELETE FROM TASK_ENRICHMENT_FIELD_CONFIG WHERE field_db_name = 'trade_equity_type';
INSERT INTO TASK_ENRICHMENT_FIELD_CONFIG (field_display_name, field_db_name, field_domain_finder, data_source_class_name, workflow_type, data_source_getter_name, custom_class_name, db_type, db_scale) VALUES ('Trade Equity Type', 'trade_equity_type', 'SecCodeDomain|EQUITY_TYPE', 'com.calypso.tk.core.Trade', 'Trade', 'getTradeEquityType', 'calypsox.util.TaskEnrichment', 'string', '128');
COMMIT;