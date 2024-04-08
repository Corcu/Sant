DELETE FROM CALYPSO.ENGINE_CONFIG WHERE ENGINE_NAME = 'FilenetSenderEngine';
DELETE FROM CALYPSO.ENGINE_PARAM WHERE  ENGINE_NAME = 'FilenetSenderEngine';
DELETE FROM CALYPSO.PS_EVENT_FILTER WHERE  ENGINE_NAME = 'FilenetSenderEngine';
DELETE FROM CALYPSO.PS_EVENT_CONFIG WHERE  ENGINE_NAME = 'FilenetSenderEngine';

INSERT INTO CALYPSO.ENGINE_CONFIG VALUES ((SELECT MAX(ENGINE_ID)+1 FROM ENGINE_CONFIG), 'FilenetSenderEngine', 'Send files to filenet.',(SELECT MAX(VERSION_NUM)  FROM ENGINE_CONFIG));

INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('FilenetSenderEngine','CLASS_NAME','com.calypso.engine.advice.SenderEngine');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('FilenetSenderEngine','DISPLAY_NAME','Filenet Sender Engine');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('FilenetSenderEngine','INSTANCE_NAME','cws_engineserver');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('FilenetSenderEngine','STARTUP','true');

INSERT INTO CALYPSO.PS_EVENT_FILTER VALUES ('Back-Office','FilenetSenderEngine','FilenetSenderEngineEventFilter');

INSERT INTO CALYPSO.PS_EVENT_CONFIG VALUES ('Back-Office','PSEventMessage','FilenetSenderEngine');
INSERT INTO CALYPSO.PS_EVENT_CONFIG VALUES ('Back-Office','PSEventProcessMessage','FilenetSenderEngine');

commit;