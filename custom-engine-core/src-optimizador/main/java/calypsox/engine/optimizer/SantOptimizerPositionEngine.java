package calypsox.engine.optimizer;

import calypsox.engine.optimizer.messages.OptimJMSQueueAnswer;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.event.PSEventSendOptimizerPosition;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tk.util.optimizer.TaskErrorUtil;
import calypsox.tk.util.optimizer.position.OptimizerExportPosition;
import calypsox.tk.util.optimizer.position.OptimizerExportUtil;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventInventorySecPosition;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SantOptimizerPositionEngine extends SantOptimizerBaseEngine {

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_ImportMessageEngine_OptimizerPosition";

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    public SantOptimizerPositionEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    /* (non-Javadoc)
     * @see calypsox.engine.optimizer.SantOptimizerBaseEngine#handleOutgoingJMSMessage(com.calypso.tk.event.PSEvent, java.util.List)
     */
    public List<JMSQueueMessage> handleOutgoingJMSMessage(PSEvent event, List<Task> tasks)
            throws Exception {
        return processMessagesEvent(event);
    }

    /* (non-Javadoc)
     * @see calypsox.engine.optimizer.SantOptimizerBaseEngine#isAcceptedEvent(com.calypso.tk.event.PSEvent)
     */
    @Override
    protected boolean isAcceptedEvent(PSEvent psEvent) {
        if ((psEvent instanceof PSEventInventorySecPosition)
                || (psEvent instanceof PSEventSendOptimizerPosition)) {
            return true;
        }
        return false;
    }

    /**
     * @param event
     * @return
     */
    private List<JMSQueueMessage> processMessagesEvent(PSEvent event) {
        List<JMSQueueMessage> listJMSQueueMessage = new ArrayList<JMSQueueMessage>();
        if (event instanceof PSEventInventorySecPosition || event instanceof PSEventSendOptimizerPosition) {
            List<OptimizerExportPosition> optimizerExportPositions = OptimizerExportUtil
                    .buildExportPositions(Arrays
                            .asList(event instanceof PSEventInventorySecPosition ? ((PSEventInventorySecPosition) event)
                                    .getPosition().getSecurityId() : ((PSEventSendOptimizerPosition) event).getProductId()), Arrays
                            .asList(event instanceof PSEventInventorySecPosition ? ((PSEventInventorySecPosition) event)
                                    .getPosition().getBookId() : ((PSEventSendOptimizerPosition) event).getBookId()));
            if (!Util.isEmpty(optimizerExportPositions)) {
                for (OptimizerExportPosition optimExportPos : optimizerExportPositions) {
                    JMSQueueMessage jmsQueueMessage = new JMSQueueMessage();
                    jmsQueueMessage
                            .setCorrelationId(optimExportPos.getKeyPos());
                    jmsQueueMessage
                            .setReference(optimExportPos.getKeyPos());
                    jmsQueueMessage.setText(optimExportPos.getValuePos());

                    Log.system(SantOptimizerPositionEngine.class.getName(), "Sending msg [" + optimExportPos.getKeyPos() + "]: " + optimExportPos.getValuePos());

                    listJMSQueueMessage.add(jmsQueueMessage);
                }
            }
        }
        return listJMSQueueMessage;
    }

    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks)
            throws Exception {
        return new OptimJMSQueueAnswer();
    }

    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
        if (externalMessage == null) {
            return true;
        }
        List<Task> tasks = TaskErrorUtil
                .getTaskErrors(TaskErrorUtil.EnumOptimProcessType.OPTIMIZER_POSITION,
                        externalMessage);
        if (!Util.isEmpty(tasks)) {
            Log.system(SantOptimizerPositionEngine.class.getName(), "Publishing task from message: " + externalMessage.getText());
            publishTask(tasks);
            Log.debug(SantOptimizerPositionEngine.class.getName(), "Task(s) published: " + tasks.toString());
        }
        return true;
    }
}
