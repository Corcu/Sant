DELETE FROM CALYPSO.ENGINE_CONFIG WHERE ENGINE_NAME = 'MarginCallNotificationEngine';
DELETE FROM CALYPSO.ENGINE_PARAM WHERE  ENGINE_NAME = 'MarginCallNotificationEngine';
DELETE FROM CALYPSO.PS_EVENT_FILTER WHERE  ENGINE_NAME = 'MarginCallNotificationEngine';
DELETE FROM CALYPSO.PS_EVENT_CONFIG WHERE  ENGINE_NAME = 'MarginCallNotificationEngine';

INSERT INTO CALYPSO.ENGINE_CONFIG VALUES ((SELECT MAX(ENGINE_ID)+1 FROM ENGINE_CONFIG), 'MarginCallNotificationEngine', 'Notify Digital platform when MC entry is validated.',(SELECT MAX(VERSION_NUM)  FROM ENGINE_CONFIG));

INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('MarginCallNotificationEngine','CLASS_NAME','calypsox.engine.notification.MarginCallNotificationEngine');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('MarginCallNotificationEngine','DISPLAY_NAME','MarginCallNotificationEngine');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('MarginCallNotificationEngine','INSTANCE_NAME','cws_engineserver');
INSERT INTO CALYPSO.ENGINE_PARAM VALUES ('MarginCallNotificationEngine','STARTUP','true');

INSERT INTO CALYPSO.PS_EVENT_FILTER VALUES ('Back-Office','MarginCallNotificationEngine','MarginCallNotificationEventFilter');

INSERT INTO CALYPSO.PS_EVENT_CONFIG VALUES ('Back-Office','PSEventMarginCallEntry','MarginCallNotificationEngine');

commit;