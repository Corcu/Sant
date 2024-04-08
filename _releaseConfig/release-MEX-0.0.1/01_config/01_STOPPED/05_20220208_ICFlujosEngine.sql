DELETE FROM ENGINE_CONFIG WHERE ENGINE_NAME = 'BondDefICExportEngine';
Insert into ENGINE_CONFIG (ENGINE_ID, ENGINE_NAME, ENGINE_COMMENT, VERSION_NUM) values ((select max(ENGINE_ID) + 1 from ENGINE_CONFIG), 'BondDefICExportEngine', 'Exports Bond Definition and flows', '100');


DELETE FROM ENGINE_PARAM WHERE ENGINE_NAME = 'BondDefICExportEngine';
Insert into ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('BondDefICExportEngine', 'DISPLAY_NAME', 'Bond Definition IC Export Engine');
Insert into ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('BondDefICExportEngine', 'CLASS_NAME', 'calypsox.engine.BondDefICExportEngine');
Insert into ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('BondDefICExportEngine', 'STARTUP', 'false');
Insert into ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('BondDefICExportEngine', 'INSTANCE_NAME', 'exp_engineserver');
Insert into ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('BondDefICExportEngine', 'config', 'bond_def_ic_exporter.properties');

DELETE FROM PS_EVENT_CONFIG WHERE ENGINE_NAME = 'BondDefICExportEngine';
INSERT INTO PS_EVENT_CONFIG (EVENT_CONFIG_NAME, EVENT_CLASS, ENGINE_NAME) VALUES ('Back-Office', 'PSEventDataUploaderAck', 'BondDefICExportEngine');
INSERT INTO PS_EVENT_CONFIG (EVENT_CONFIG_NAME, EVENT_CLASS, ENGINE_NAME) VALUES ('Back-Office', 'PSEventProduct', 'BondDefICExportEngine');

DELETE FROM PS_EVENT_FILTER WHERE ENGINE_NAME = 'BondDefICExportEngine';
INSERT INTO PS_EVENT_FILTER (EVENT_CONFIG_NAME, ENGINE_NAME, EVENT_FILTER) VALUES('Back-Office', 'BondDefICExportEngine', 'BondDefICExportAckEngineEventFilter');
