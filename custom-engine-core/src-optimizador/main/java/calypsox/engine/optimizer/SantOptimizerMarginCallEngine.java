package calypsox.engine.optimizer;

import calypsox.engine.optimizer.messages.OptimJMSQueueAnswer;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.event.PSEventSendOptimizerMarginCall;
import calypsox.tk.event.PSEventSendOptimizerMarginCallStatus;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tk.util.ScheduledTaskSANT_EXPORT_OPTIMIZER;
import calypsox.tk.util.optimizer.TaskErrorUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SantOptimizerMarginCallEngine extends SantOptimizerBaseEngine {

    private static final String EXPORT_OPT_MC_STATUS_REPRO = "EXPORT_OPT_MC_STATUS_REPRO";

    private static final String REPORT_FILE_NAME = "REPORT FILE NAME";

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_ImportMessageEngine_OptimizerMarginCall";

    private static SimpleDateFormat sdf_HHmm = new SimpleDateFormat("HHmm");

    public SantOptimizerMarginCallEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public boolean handleIncomingMessage(String message, List<Task> tasks)
            throws Exception {
        return false;
    }

    @Override
    public List<JMSQueueMessage> handleOutgoingJMSMessage(PSEvent event,
                                                          List<Task> tasks) throws Exception {
        if (isAcceptedEvent(event)) {
            return processMessagesEvent(event);
        }
        return null;
    }

    private List<JMSQueueMessage> processMessagesEvent(PSEvent event) {
        List<JMSQueueMessage> listJMSQueueMessage = new ArrayList<JMSQueueMessage>();
        JMSQueueMessage jmsQueueMessage = new JMSQueueMessage();

        PSEventSendOptimizerMarginCall psEvent = (PSEventSendOptimizerMarginCall) event;
        if (psEvent != null) {
            ScheduledTaskSANT_EXPORT_OPTIMIZER st;
            try {
                st = (ScheduledTaskSANT_EXPORT_OPTIMIZER) DSConnection
                        .getDefault()
                        .getRemoteBackOffice()
                        .getScheduledTaskByExternalReference(
                                EXPORT_OPT_MC_STATUS_REPRO);

                st.setCurrentDate(JDate.getNow());
                int i = Integer.valueOf(sdf_HHmm.format(new JDatetime()));
                st.setValuationTime(i);
                String tempName = (String) st.getAttributes().get(
                        REPORT_FILE_NAME);
                st.getAttributes().put(REPORT_FILE_NAME, tempName);

                st.process(DSConnection.getDefault(), null);

                String fileName = st.getFileName();
                if (!Util.isEmpty(fileName) && fileName.startsWith("file://")
                        && fileName.length() > 7) {
                    Log.system(SantOptimizerMarginCallEngine.class.getName(),
                            "File generated: " + fileName);
                    fileName = fileName.substring(7);
                }

                File file = new File(fileName);

                if (!file.exists()) {
                    Log.error(SantOptimizerMarginCallEngine.class.getName(),
                            "Cannot find file: " + fileName);
                    return listJMSQueueMessage;
                }
                BufferedReader buf = new BufferedReader(new FileReader(file));

                String line = null;
                StringBuffer fileContent = new StringBuffer();
                int nbEntries = -1;
                while ((line = buf.readLine()) != null) {
                    fileContent.append(line + "\n");
                    nbEntries++;
                }
                buf.close();

                if (nbEntries > 0 && !Util.isEmpty(fileContent.toString())) {
                    jmsQueueMessage.setCorrelationId(file.getName());
                    jmsQueueMessage.setReference(file.getName());
                    jmsQueueMessage.setText(fileContent.toString());

                    Log.system(SantOptimizerMarginCallEngine.class.getName(),
                            "Sending msg [" + file.getName() + "]: "
                                    + fileContent.toString());

                    listJMSQueueMessage.add(jmsQueueMessage);
                } else {
                    // ack message as it will not be done auto
                    getDS().getRemoteTrade().eventProcessed(event.getLongId(),
                            getEngineName());
                }

                PSEventSendOptimizerMarginCallStatus eventStatus = new PSEventSendOptimizerMarginCallStatus(
                        file.getName(), new JDatetime(), nbEntries < 0 ? 0
                        : nbEntries);
                DSConnection.getDefault().getRemoteTrade()
                        .saveAndPublish(eventStatus);

                Log.debug(SantOptimizerMarginCallEngine.class.getName(),
                        "Sending PSEvent: " + eventStatus);
            } catch (RemoteException e) {
                Log.error(SantOptimizerMarginCallEngine.class.getName(), e);
            } catch (FileNotFoundException e) {
                Log.error(SantOptimizerMarginCallEngine.class.getName(), e);
            } catch (IOException e) {
                Log.error(SantOptimizerMarginCallEngine.class.getName(), e);
            }
        }
        return listJMSQueueMessage;
    }

    @Override
    protected boolean isAcceptedEvent(PSEvent psEvent) {
        if (psEvent instanceof PSEventSendOptimizerMarginCall) {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
        if (externalMessage == null) {
            return true;
        }
        List<Task> tasks = TaskErrorUtil.getTaskErrors(
                TaskErrorUtil.EnumOptimProcessType.OPTIMIZER_MARGIN_CALL,
                externalMessage);
        if (!Util.isEmpty(tasks)) {
            Log.system(
                    SantOptimizerMarginCallEngine.class.getName(),
                    "Publishing task from message: "
                            + externalMessage.getText());
            publishTask(tasks);
            Log.debug(SantOptimizerMarginCallEngine.class.getName(),
                    "Task(s) published: " + tasks.toString());
        }
        return true;
    }

    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks)
            throws Exception {
        OptimJMSQueueAnswer importAnswer = new OptimJMSQueueAnswer();

        return importAnswer;
    }
}
