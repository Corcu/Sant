DELETE FROM CALYPSO.ENGINE_CONFIG WHERE ENGINE_NAME = 'UpdateCABondChangeEngine';
DELETE FROM CALYPSO.ENGINE_PARAM WHERE  ENGINE_NAME = 'UpdateCABondChangeEngine';
DELETE FROM CALYPSO.PS_EVENT_FILTER WHERE  ENGINE_NAME = 'UpdateCABondChangeEngine';
DELETE FROM CALYPSO.PS_EVENT_CONFIG WHERE  ENGINE_NAME = 'UpdateCABondChangeEngine';

INSERT INTO CALYPSO.ENGINE_CONFIG VALUES ((SELECT MAX(ENGINE_ID)+1 FROM ENGINE_CONFIG), 'UpdateCABondChangeEngine', 'Auto update CA product and CA trades for Bond modification.',(SELECT MAX(VERSION_NUM)  FROM ENGINE_CONFIG));

INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('UpdateCABondChangeEngine','CLASS_NAME','calypsox.engine.updateCABond.UpdateCABondChangeEngine');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('UpdateCABondChangeEngine','DISPLAY_NAME','UpdateCABondChangeEngine');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('UpdateCABondChangeEngine','INSTANCE_NAME','gen_engineserver');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('UpdateCABondChangeEngine','STARTUP','true');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('UpdateCABondChangeEngine','PricingEnv','OFFICIAL_ACCOUNTING');

INSERT INTO CALYPSO.PS_EVENT_CONFIG VALUES ('Back-Office','PSEventDomainChange','UpdateCABondChangeEngine');

commit;