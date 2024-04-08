-- Global
-- Tables : enrichment_context, audit_process_table
DELETE FROM enrichment_context WHERE context_id = 820198;
insert into enrichment_context(context_id, context_name, primary_key_size, source, source_table, enrichment_table, synchronous, trigger_events, active, hidden) VALUES (820198, 'Searchable Trade Keyword', 1, 'com.calypso.tk.core.Trade', 'trade', 'trade_keyword_accel', 1, '', 1, 1);

delete from ENRICHMENT_FIELD where CONTEXT_ID = 820198;
INSERT INTO ENRICHMENT_FIELD (CONTEXT_ID, FIELD_RANK, NAME, COLUMN_NAME, CUSTOM, GETTER, "TYPE", FORMAT, "SCALE", NULLABLE, STATUS, "DOMAIN") VALUES(820198, 1, 'Trade Id', 'id', NULL, 'getLongId', 'Long', 'native', 1, 0, 'primary_key', NULL);


-- CONFIG
-- Fields 
-- Tables : enrichment_field, attribute_config, enrichment_extra_parameter
-- MirrorClientAccount
DELETE FROM domain_values where name = 'tradeKeyword' AND value = 'MirrorClientAccount';
INSERT INTO domain_values (name,value,description) VALUES('tradeKeyword', 'MirrorClientAccount','');

DELETE FROM attribute_config where id = 820698;
insert into attribute_config(source_class, attr_table_name, attr_column_name, attribute_name, attribute_class, id, version, searchable, domain_name, entered_user) VALUES ('com.calypso.tk.core.Trade', 'trade_keyword_accel', 'mirrorclientaccount', 'MirrorClientAccount', 'com.calypso.tk.refdata.Account', 820698, 0, 1, 'TradeKeyword|MirrorClientAccount', 'null');

delete from enrichment_extra_parameter where CONTEXT_ID = 820198;
insert into enrichment_extra_parameter(context_id, field_rank, param_rank, name) VALUES (820198, 3, 1, 'MirrorClientAccount');

INSERT INTO ENRICHMENT_FIELD (CONTEXT_ID, FIELD_RANK, NAME, COLUMN_NAME, CUSTOM, GETTER, "TYPE", FORMAT, "SCALE", NULLABLE, STATUS, "DOMAIN") VALUES(820198, 3, 'MirrorClientAccount', 'mirrorclientaccount', NULL, 'getKeywordPersistentValue', 'Integer', 'native', 1, 1, 'native', 'TradeKeyword|MirrorClientAccount');

-- ClientAccount
DELETE FROM domain_values where name = 'tradeKeyword' AND value = 'ClientAccount';
INSERT INTO domain_values (name,value,description) VALUES('tradeKeyword', 'ClientAccount','');

DELETE FROM attribute_config where id = 820697;
insert into attribute_config(source_class, attr_table_name, attr_column_name, attribute_name, attribute_class, id, version, searchable, domain_name, entered_user) VALUES ('com.calypso.tk.core.Trade', 'trade_keyword_accel', 'clientaccount', 'ClientAccount', 'com.calypso.tk.refdata.Account', 820697, 0, 1, 'TradeKeyword|ClientAccount', 'null');

insert into enrichment_extra_parameter(context_id, field_rank, param_rank, name) VALUES (820198, 2, 1, 'ClientAccount');

INSERT INTO ENRICHMENT_FIELD (CONTEXT_ID, FIELD_RANK, NAME, COLUMN_NAME, CUSTOM, GETTER, "TYPE", FORMAT, "SCALE", NULLABLE, STATUS, "DOMAIN") VALUES(820198, 2, 'ClientAccount', 'clientaccount', NULL, 'getKeywordPersistentValue', 'Integer', 'native', 1, 1, 'native', 'TradeKeyword|ClientAccount');


-- New Table
CREATE TABLE trade_keyword_accel ( id numeric  NOT NULL, clientaccount numeric  NULL, mirrorclientaccount numeric  NULL );
CREATE UNIQUE INDEX pk_trade_keyword_acc1 ON TRADE_KEYWORD_ACCEL ( id ) nologging parallel 8 ;
ALTER TABLE TRADE_KEYWORD_ACCEL ADD CONSTRAINT pk_trade_keyword_acc1 PRIMARY KEY ( id ) USING INDEX ;
ALTER INDEX pk_trade_keyword_acc1 noparallel logging ;




