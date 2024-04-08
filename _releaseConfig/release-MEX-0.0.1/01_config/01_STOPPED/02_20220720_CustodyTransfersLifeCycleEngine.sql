-- Se crea la configuracion del nuevo engine
DELETE FROM CALYPSO.ENGINE_CONFIG WHERE ENGINE_NAME = 'CustodyTransfersLifeCycleEngine';
Insert into CALYPSO.ENGINE_CONFIG (ENGINE_ID,ENGINE_NAME,ENGINE_COMMENT,VERSION_NUM) values ((select max(ENGINE_ID)+1 from CALYPSO.ENGINE_CONFIG), 'CustodyTransfersLifeCycleEngine','CustodyTransfersLifeCycleEngine','1');

-- Cargando engine params
DELETE FROM CALYPSO.ENGINE_PARAM WHERE ENGINE_NAME = 'CustodyTransfersLifeCycleEngine';
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('CustodyTransfersLifeCycleEngine', 'CLASS_NAME', 'com.calypso.engine.lifecycle.LifeCycleEngine');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('CustodyTransfersLifeCycleEngine', 'DISPLAY_NAME','CustodyTransfersLifeCycleEngine');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('CustodyTransfersLifeCycleEngine', 'INSTANCE_NAME', 'imp_engineserver');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('CustodyTransfersLifeCycleEngine', 'STARTUP', 'true');
Insert into CALYPSO.ENGINE_PARAM (ENGINE_NAME, PARAM_NAME, PARAM_VALUE) values ('CustodyTransfersLifeCycleEngine', 'config', 'CustodyTransfers');

-- Se crea el pseventconfiguration para nuestro engine
DELETE FROM CALYPSO.PS_EVENT_CONFIG WHERE ENGINE_NAME = 'CustodyTransfersLifeCycleEngine'; 
INSERT INTO CALYPSO.PS_EVENT_CONFIG (EVENT_CONFIG_NAME, EVENT_CLASS, ENGINE_NAME) VALUES ('Back-Office', 'PSEventProcessTransfer', 'CustodyTransfersLifeCycleEngine');
INSERT INTO CALYPSO.PS_EVENT_CONFIG (EVENT_CONFIG_NAME, EVENT_CLASS, ENGINE_NAME) VALUES ('Back-Office', 'PSEventTransfer', 'CustodyTransfersLifeCycleEngine');

-- Se crea el eventfilter para nuestro engine
DELETE FROM CALYPSO.PS_EVENT_FILTER WHERE ENGINE_NAME = 'CustodyTransfersLifeCycleEngine';
INSERT INTO CALYPSO.PS_EVENT_FILTER (EVENT_CONFIG_NAME, ENGINE_NAME, EVENT_FILTER) VALUES ('Back-Office', 'CustodyTransfersLifeCycleEngine', 'TransferTypeExcluded');
