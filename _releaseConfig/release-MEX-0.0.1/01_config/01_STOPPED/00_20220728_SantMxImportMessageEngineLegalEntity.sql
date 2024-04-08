-- Se crea la configuracion del nuevo engine
DELETE FROM CALYPSO.ENGINE_CONFIG WHERE ENGINE_NAME='SANT_MX_ImportMessageEngineLegalEntity';
Insert into CALYPSO.ENGINE_CONFIG (ENGINE_ID,ENGINE_NAME,ENGINE_COMMENT,VERSION_NUM) values ((select max(ENGINE_ID)+1 from CALYPSO.ENGINE_CONFIG), 'SANT_MX_ImportMessageEngineLegalEntity','Starts and stops SANT_MX_ImportMessageEngineLegalEntity','408');

-- Cargando engine params
DELETE FROM CALYPSO.ENGINE_PARAM WHERE ENGINE_NAME='SANT_MX_ImportMessageEngineLegalEntity';
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','CLASS_NAME','com.calypso.tk.engine.UploadImportMessageEngine');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','DISPLAY_NAME','SANT_MX_ImportMessageEngineLegalEntity');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','EVENT_POOL_POLICY','UploaderImportMessageEngine');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','INSTANCE_NAME','imp_engineserver');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','PricingEnv','OFFICIAL');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','STARTUP','false');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME,PARAM_NAME,PARAM_VALUE) values ('SANT_MX_ImportMessageEngineLegalEntity','config','MxLegalEntity');

-- Se crea el pseventconfiguration para nuestro engine
DELETE FROM CALYPSO.PS_EVENT_CONFIG WHERE ENGINE_NAME='SANT_MX_ImportMessageEngineLegalEntity'; 
INSERT INTO CALYPSO.PS_EVENT_CONFIG (EVENT_CONFIG_NAME,EVENT_CLASS,ENGINE_NAME) VALUES ('Back-Office','PSEventUpload','SANT_MX_ImportMessageEngineLegalEntity');

-- Se crea el eventfilter para nuestro engine
DELETE FROM CALYPSO.PS_EVENT_FILTER WHERE ENGINE_NAME='SANT_MX_ImportMessageEngineLegalEntity';
INSERT INTO CALYPSO.PS_EVENT_FILTER (EVENT_CONFIG_NAME,ENGINE_NAME,EVENT_FILTER) VALUES ('Back-Office','SANT_MX_ImportMessageEngineLegalEntity','PSEventUploadMxLegalEntityEventFilter');

-- SELECT * FROM  CALYPSO.ENGINE_CONFIG WHERE ENGINE_NAME ='SANT_MX_ImportMessageEngineLegalEntity';
-- SELECT * FROM  CALYPSO.ENGINE_PARAM WHERE ENGINE_NAME ='SANT_MX_ImportMessageEngineLegalEntity';
-- SELECT * FROM  CALYPSO.PS_EVENT_CONFIG WHERE ENGINE_NAME ='SANT_MX_ImportMessageEngineLegalEntity';

-- SELECT * FROM  CALYPSO.PS_EVENT_FILTER WHERE ENGINE_NAME ='SANT_MX_ImportMessageEngineLegalEntity';
-- SELECT * FROM PS_EVENT_CFG_NAME;