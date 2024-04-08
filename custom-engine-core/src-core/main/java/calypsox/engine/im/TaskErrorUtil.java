package calypsox.engine.im;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;

import calypsox.engine.im.export.input.SantInitialMarginExportInput;
import calypsox.engine.im.importim.output.SantInitialMarginImportImOutput;
import calypsox.tk.bo.JMSQueueMessage;

public class TaskErrorUtil {

	public static Task buildTask(String processType, SantInitialMarginExportInput inputReceived,
			ExternalMessage externalMessage) {
		Task task = new Task();

		JDatetime currentDatetime = new JDatetime();

		task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
		task.setNewDatetime(currentDatetime);
		task.setUnderProcessingDatetime(currentDatetime);
		task.setUndoTradeDatetime(currentDatetime);
		task.setDatetime(currentDatetime);
		task.setPriority(Task.PRIORITY_HIGH);
		task.setId(0);
		task.setSource(processType);
		task.setOwner(processType);
		task.setEventType("EX_" + processType);

		JMSQueueMessage jmsQueueMsg = (JMSQueueMessage) externalMessage;

		// task.setObjectId(getObjectIdFromCorrelationId(correlationId));
		// correlationId = getCorrelationId(message, importStatusElt.getImportKey());
		task.setComment("correlationId" + inputReceived.getErrorCode() + ": " + inputReceived.getErrorDescription());

		return task;
	}

	public static Task buildTask(String processType, SantInitialMarginImportImOutput outputSent,
			ExternalMessage externalMessage) {
		Task task = new Task();

		JDatetime currentDatetime = new JDatetime();

		task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
		task.setNewDatetime(currentDatetime);
		task.setUnderProcessingDatetime(currentDatetime);
		task.setUndoTradeDatetime(currentDatetime);
		task.setDatetime(currentDatetime);
		task.setPriority(Task.PRIORITY_HIGH);
		task.setId(0);
		task.setSource(processType);
		task.setOwner(processType);
		task.setEventType("EX_" + processType);

		JMSQueueMessage jmsQueueMsg = (JMSQueueMessage) externalMessage;

		// task.setObjectId(getObjectIdFromCorrelationId(correlationId));
		// correlationId = getCorrelationId(message, importStatusElt.getImportKey());
		task.setComment("correlationId" + outputSent.getErrorCode() + ": " + outputSent.getErrorDescription());

		return task;
	}
	
	public static Task buildTask(String processType, String coment) {
		Task task = new Task();

		JDatetime currentDatetime = new JDatetime();

		task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
		task.setNewDatetime(currentDatetime);
		task.setUnderProcessingDatetime(currentDatetime);
		task.setUndoTradeDatetime(currentDatetime);
		task.setDatetime(currentDatetime);
		task.setPriority(Task.PRIORITY_HIGH);
		task.setId(0);
		task.setSource(processType);
		task.setOwner(processType);
		task.setEventType("EX_" + processType);
		task.setComment(coment);

		return task;
	}

}
