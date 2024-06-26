set serveroutput on

DECLARE
	task_count NUMBER;
BEGIN
	DELETE FROM (SELECT * FROM BO_TASK WHERE EVENT_TYPE='READY_TO_PRICE_TRADE') WHERE ROWNUM <= 3500000;
	DBMS_OUTPUT.PUT_LINE(TO_Char(SQL%ROWCOUNT)||' tasks deleted.');
	SELECT COUNT(*) INTO task_count FROM BO_TASK;
	DBMS_OUTPUT.PUT_LINE(task_count ||' total tasks on table.');
END;
/
COMMIT;