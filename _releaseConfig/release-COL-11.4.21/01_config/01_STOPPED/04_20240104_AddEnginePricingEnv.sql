DELETE FROM CALYPSO.ENGINE_PARAM WHERE ENGINE_NAME='SANT_GestorSTPIncomingMessageEngine' AND PARAM_NAME = 'PricingEnv';

INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('SANT_GestorSTPIncomingMessageEngine','PricingEnv','OFFICIAL_ACCOUNTING');

COMMIT;