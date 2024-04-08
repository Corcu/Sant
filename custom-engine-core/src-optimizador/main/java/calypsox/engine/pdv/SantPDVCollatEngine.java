package calypsox.engine.pdv;

import calypsox.engine.optimizer.SantOptimizerBaseEngine;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationsImporter;
import calypsox.tk.collateral.allocation.importer.jms.PDVJMSQueueAnswer;
import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.pdv.importer.PDVUtil.EnumMessageType;
import calypsox.tk.event.PSEventPDVAllocation;
import calypsox.tk.event.PSEventPDVAllocationFut;
import calypsox.tk.util.AbstractProcessFeedScheduledTask;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tk.util.ScheduledTaskImportCSVExposureTrades;
import calypsox.tk.util.ScheduledTaskOPTIM_ALLOC_IMPORT;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.sun.xml.bind.StringInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.*;

/**
 * @author aela
 */
public class SantPDVCollatEngine extends SantOptimizerBaseEngine implements
        PDVConstants {

    private static final String PDV_TRADE_FILE_NAME = "PDV.txt";
    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_ImportMessageEngine_PDVCollat";
    private static final String ST_PDV_TRADE_IMPORT = "IMPORT_PDV_TRADE";

    /**
     * @param configName
     * @param dsCon
     * @param hostName
     * @param esPort
     */
    public SantPDVCollatEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.engine.optimizer.SantOptimizerBaseEngine#importMessage(calypsox
     * .tk.bo.JMSQueueMessage, java.util.List)
     */
    @Override
    protected JMSQueueAnswer importMessage(JMSQueueMessage jmsMessage,
                                           List<Task> tasks) throws Exception {
        boolean isProcessingOK = false;
        String pdvMsg = jmsMessage.getText();
        String fileName = PDV_TRADE_FILE_NAME;

        Log.system(SantPDVCollatEngine.class.getName(), "Receiving message: "
                + jmsMessage.getText());

        if (!Util.isEmpty(pdvMsg)) {
            String pdvType = getPDVType(pdvMsg);

            if (SEC_LENDING_PDV_TYPE.equals(pdvType)) {

                PDVJMSQueueAnswer importAnswer = new PDVJMSQueueAnswer(
                        Util.isEmpty(jmsMessage.getCorrelationId()) ? PDV_TRADE_FILE_NAME
                                : jmsMessage.getCorrelationId());
                String numFrontId = getNumFrontId(pdvMsg);
                // first get the scheduledTask of PDV Trade import
                ScheduledTaskImportCSVExposureTrades st = null;
                try {
                    st = (ScheduledTaskImportCSVExposureTrades) DSConnection
                            .getDefault()
                            .getRemoteBO()
                            .getScheduledTaskByExternalReference(
                                    ST_PDV_TRADE_IMPORT);
                } catch (RemoteException e) {
                    Log.error(SantPDVCollatEngine.class.getName(), e);
                }
                if (st == null) {
                    tasks.add(buildTask(
                            "No configuration found for the PDV Trade import process",
                            0, PDV_TRADE_EXCEPTION_TYPE, "Collateral"));
                    importAnswer.setCode(JMSQueueAnswer.KO);
                    importAnswer.addMessage(JMSQueueAnswer.KO, numFrontId,
                            "No Trade import configuration found to process message: "
                                    + pdvMsg);
                    return importAnswer;
                } else {
                    File file = writeFile(
                            st.getAttribute(ScheduledTaskOPTIM_ALLOC_IMPORT.FILEPATH),
                            fileName, pdvMsg, tasks);
                    if (file.exists()) {
                        st.setAttribute(
                                AbstractProcessFeedScheduledTask.FILEPATH,
                                st.getAttribute(ScheduledTaskOPTIM_ALLOC_IMPORT.FILEPATH));
                        st.setAttribute(
                                AbstractProcessFeedScheduledTask.STARTFILENAME,
                                PDV_TRADE_FILE_NAME);
                    } else {
                        importAnswer.setCode(JMSQueueAnswer.KO);
                        importAnswer.addMessage(
                                JMSQueueAnswer.KO,
                                numFrontId,
                                "No processing, file not found: "
                                        + file.getAbsolutePath());
                        return importAnswer;
                    }

                    // Import PDV Trade Message
                    isProcessingOK = st
                            .process(DSConnection.getDefault(), null);

                    importAnswer = new PDVJMSQueueAnswer(
                            (isProcessingOK ? JMSQueueAnswer.OK
                                    : JMSQueueAnswer.KO));
                    if (st.getTradeImportTracker() != null) {
                        if (st.getTradeImportTracker().getPDVProcessingErrors()
                                .size() > 0) {
                            isProcessingOK = false;
                            Vector<Task> tasksToLog = PDVUtil
                                    .getTasksToPublish(st
                                            .getTradeImportTracker());
                            for (Task task : tasksToLog) {
                                Log.error(SantPDVCollatEngine.class.getName(),
                                        task.getComment());
                            }
                            // Done by ST
                            // tasks.addAll(PDVUtil.getTasksToPublish(st.getTradeImportTracker()));
                        }
                    }
                    importAnswer.setCode((isProcessingOK ? JMSQueueAnswer.OK
                            : JMSQueueAnswer.KO));
                    importAnswer.addMessage(isProcessingOK ? JMSQueueAnswer.OK
                                    : JMSQueueAnswer.KO, numFrontId,
                            isProcessingOK ? "" : (st.getTradeImportTracker()
                                    .getPDVProcessingErrors() != null && st
                                    .getTradeImportTracker()
                                    .getPDVProcessingErrors().size() > 0) ? st
                                    .getTradeImportTracker()
                                    .getPDVProcessingErrors().get(0)
                                    .getErrorMessage()
                                    : "Error while processing message: "
                                    + pdvMsg);
                    return importAnswer;
                }

            } else if (COLLAT_SECURITY_PDV_TYPE.equals(pdvType)
                    || COLLAT_CASH_PDV_TYPE.equals(pdvType)) {

                HashMap<String, String> intrCollatValues = (HashMap<String, String>) PDVUtil
                        .getFieldValues(EnumMessageType.COLLAT_MESSAGE, pdvMsg,
                                Arrays.asList(COLLAT_ACTION_FIELD,
                                        COLLAT_VALUE_DATE_FIELD,
                                        COLLAT_COLLAT_ID_FIELD));

                JDate valDate = JDate.valueOf(intrCollatValues
                        .get(COLLAT_VALUE_DATE_FIELD));
                String action = intrCollatValues.get(COLLAT_ACTION_FIELD);
                String collatId = intrCollatValues.get(COLLAT_COLLAT_ID_FIELD);

                if (needPreProcess(valDate, action)) {
                    isProcessingOK = preProcessTaskStation(valDate, action,
                            collatId, pdvMsg, tasks);
                }

                if (valDate.equals(JDate.getNow())) {

                    JDatetime processingDate = new JDatetime();
                    final int calculationOffSet = ServiceRegistry
                            .getDefaultContext().getValueDateDays() * -1;
                    final JDate valuatioDate = Holiday.getCurrent()
                            .addBusinessDays(
                                    processingDate.getJDate(TimeZone.getDefault()),
                                    DSConnection.getDefault().getUserDefaults()
                                            .getHolidays(), calculationOffSet);
                    ExternalAllocationImportContext context = new ExternalAllocationImportContext(
                            "|", true);

                    String pricingEnv = (Util.isEmpty(getPricingEnvName()) ? "DirtyPrice"
                            : getPricingEnvName());

                    context.init(processingDate, new JDatetime(valuatioDate, TimeZone.getDefault()),
                            pricingEnv);

                    // already done for PDV_ALLOCATION_FUT
                    if (!PSEventPDVAllocationFut.PDV_ALLOCATION_FUT
                            .equals(jmsMessage.getReference())) {
                        // complete tasks for CANCEL and ACCOUNT_CLOSING events
                        PDVUtil.completeAllocFutureTasks(action,
                                Long.valueOf(collatId), valDate, null, tasks);
                    }

                    // Import PDV Collateral Message
                    isProcessingOK = importPDVMessage(pdvMsg, new JDatetime(),
                            context, tasks);

                    if (!Util.isEmpty(context.getTasksToPublish())) {
                        for (Task task : context.getTasksToPublish()) {
                            if (PDVConstants.PDV_ALLOC_EXCEPTION_TYPE.equals(task.getEventType())) {
                                // contractId saved in ObjectId while processing import
                                task.setLinkId(task.getObjectLongId());
                                // set collatId as ObjectId for processing issues
                                task.setObjectLongId(Long.valueOf(collatId));
                                // set message as internal reference for reprocessing issues
                                task.setInternalReference(pdvMsg);

                                // add task in order to by publish
                                tasks.add(task);
                            }
                        }
                    }

                    isProcessingOK &= Util.isEmpty(context.getInvalidItems());

                    PDVJMSQueueAnswer importAnswer = new PDVJMSQueueAnswer(
                            (isProcessingOK ? JMSQueueAnswer.OK
                                    : JMSQueueAnswer.KO));

                    importAnswer.setCode((isProcessingOK ? JMSQueueAnswer.OK
                            : JMSQueueAnswer.KO));

                    if (!isProcessingOK) {
                        List<Object> invalidItems = context.getInvalidItems();
                        if (!Util.isEmpty(invalidItems)) {
                            importAnswer.addMessages(invalidItems,
                                    JMSQueueAnswer.KO);
                        }
                    } else {
                        // get valid items...
                        List<Object> validItems = context.getValidItems();
                        if (!Util.isEmpty(validItems)) {
                            importAnswer.addAckMessages(validItems,
                                    JMSQueueAnswer.OK);
                        }
                    }
                    return importAnswer;
                } else if (valDate.after(JDate.getNow())) {
                    PDVJMSQueueAnswer importAnswer = new PDVJMSQueueAnswer(
                            (isProcessingOK ? JMSQueueAnswer.OK
                                    : JMSQueueAnswer.KO));

                    importAnswer.setCode((isProcessingOK ? JMSQueueAnswer.OK
                            : JMSQueueAnswer.KO));

                    if (isProcessingOK) {
                        importAnswer.addMessage(JMSQueueAnswer.OK, "0",
                                "OK-TEC for message received: " + pdvMsg);
                    } else {
                        importAnswer.addMessage(JMSQueueAnswer.KO, "0",
                                "KO-TEC for message received: " + pdvMsg);
                    }

                    return importAnswer;
                } else {
                    PDVJMSQueueAnswer importAnswer = new PDVJMSQueueAnswer(
                            JMSQueueAnswer.KO);
                    importAnswer.setCode(JMSQueueAnswer.KO);
                    importAnswer.addMessage(JMSQueueAnswer.KO, "0",
                            "Message not processed as message valDate is in the past: "
                                    + pdvMsg);
                    return importAnswer;
                }
            } else {
                PDVJMSQueueAnswer importAnswer = new PDVJMSQueueAnswer(
                        JMSQueueAnswer.KO);
                importAnswer.setCode(JMSQueueAnswer.KO);
                importAnswer.addMessage(JMSQueueAnswer.KO, "0",
                        "Unknown intrument type " + pdvType
                                + " on message received: " + pdvMsg);
                return importAnswer;
            }
        }

        return null;
    }

    /**
     * Pre Process in Task Station
     *
     * @param valDate
     * @param action
     * @param collatId
     * @param pdvMsg
     * @param tasks
     */
    private boolean preProcessTaskStation(JDate valDate, String action,
                                          String collatId, String pdvMsg, List<Task> tasks) {

        boolean isFuture = valDate.after(JDate.getNow());

        if (isFuture) {
            // NEW, MATURE, SLA and ACCOUNT_CLOSING events in the future are
            // saved as tasks
            if (PDV_ACTION.NEW.toString().equals(action)
                    || PDV_ACTION.MATURE.toString().equals(action)
                    || PDV_ACTION.SLA.toString().equals(action)
                    || PDV_ACTION.ACCLOSING.toString().equals(action)) {
                tasks.add(PDVUtil.buildTaskAllocFuture(pdvMsg, action,
                        collatId, valDate));
            }
        } else {
            Log.error(SantPDVCollatEngine.class.getName(),
                    "preProcessTaskStation / case not expected: " + pdvMsg);
            return false;
        }

        return true;
    }

    /**
     * Check if Collat needs PreProcess
     *
     * @param valDate
     * @param action
     * @return
     */
    private boolean needPreProcess(JDate valDate, String action) {

        boolean isToday = valDate.equals(JDate.getNow());

        if (isToday) {
            return false;
        }

        return true;
    }

    /**
     * @param pdvMsg
     * @return
     */
    private String getPDVType(String pdvMsg) {
        List<String> instrCollat = Arrays
                .asList(PDVConstants.COLLAT_INSTRUMENT_FIELD);

        HashMap<String, String> intrCollatValues = (HashMap<String, String>) PDVUtil
                .getFieldValues(EnumMessageType.COLLAT_MESSAGE, pdvMsg,
                        instrCollat);

        String pdvType = intrCollatValues.get(COLLAT_INSTRUMENT_FIELD);

        if (!COLLAT_SECURITY_PDV_TYPE.equals(pdvType)
                && !COLLAT_CASH_PDV_TYPE.equals(pdvType)) {
            List<String> intrTradeFields = Arrays
                    .asList(PDVConstants.TRADE_INSTRUMENT_FIELD);

            HashMap<String, String> intrTradeValues = (HashMap<String, String>) PDVUtil
                    .getFieldValues(EnumMessageType.TRADE_MESSAGE, pdvMsg,
                            intrTradeFields);
            pdvType = intrTradeValues.get(TRADE_INSTRUMENT_FIELD);
        }
        return pdvType;
    }

    /**
     * @param pdvMsg
     * @return
     */
    private String getNumFrontId(String pdvMsg) {
        List<String> field = Arrays
                .asList(PDVConstants.TRADE_NUM_FRONT_ID_FIELD);

        HashMap<String, String> fieldValues = (HashMap<String, String>) PDVUtil
                .getFieldValues(EnumMessageType.TRADE_MESSAGE, pdvMsg, field);

        String pdvType = fieldValues.get(TRADE_NUM_FRONT_ID_FIELD);

        return pdvType;
    }

    /**
     * @param pdvMsg
     * @param jDatetime
     * @param context
     * @param tasks
     * @return
     */
    private boolean importPDVMessage(String pdvMsg, JDatetime jDatetime,
                                     ExternalAllocationImportContext context, List<Task> tasks) {
        boolean processOk = false;
        if (!Util.isEmpty(pdvMsg)) {

            ExternalAllocationsImporter importer = new ExternalAllocationsImporter(
                    context);
            processOk = importer.importFileAllocations(new JDatetime(), 1, 1,
                    100, 100, 100, new StringInputStream(pdvMsg));
        }

        return processOk;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.engine.optimizer.SantOptimizerBaseEngine#isAcceptedEvent(com
     * .calypso.tk.event.PSEvent)
     */
    protected boolean isAcceptedEvent(PSEvent psEvent) {
        if (psEvent instanceof PSEventPDVAllocation
                || psEvent instanceof PSEventPDVAllocationFut) {
            return true;
        }
        return false;
    }

    @Override
    public List<JMSQueueMessage> handleOutgoingJMSMessage(PSEvent event,
                                                          List<Task> tasks) throws Exception {
        if (isAcceptedEvent(event)) {
            return processMessagesEvent(event, tasks);
        }
        return null;
    }

    private List<JMSQueueMessage> processMessagesEvent(PSEvent event,
                                                       List<Task> tasks) {
        List<JMSQueueMessage> listJMSQueueMessage = new ArrayList<JMSQueueMessage>();

        if (event instanceof PSEventPDVAllocation) {

            PSEventPDVAllocation psEvent = (PSEventPDVAllocation) event;
            if (psEvent != null && !Util.isEmpty(psEvent.getMessageAlloc())) {
                Log.system(
                        SantPDVCollatEngine.class.getName(),
                        "Receiving PSEventPDVAllocation message: "
                                + psEvent.getMessageAlloc());

                JMSQueueMessage jmsQueueMessage = new JMSQueueMessage();
                jmsQueueMessage.setText(psEvent.getMessageAlloc());
                jmsQueueMessage.setReference(psEvent.getEventType());
                jmsQueueMessage.setCorrelationId(String.valueOf(psEvent.getCollatId()));

                if ((psEvent.isMultiContractB() && psEvent.getTradeId() > 0) || psEvent.isReprocessB()) {

                    try {

                        if (psEvent.isMultiContractB()) {
                            Trade tradeAlloc = DSConnection.getDefault()
                                    .getRemoteTrade().getTrade(psEvent.getTradeId());

                            if (psEvent.getContractId() > 0
                                    && !String
                                    .valueOf(psEvent.getContractId())
                                    .equals(tradeAlloc
                                            .getKeywordValue(MC_CONTRACT_NUMBER_TRADE_KEYWORD))) {
                                tradeAlloc.addKeyword(MC_CONTRACT_NUMBER_TRADE_KEYWORD,
                                        psEvent.getContractId());
                                tradeAlloc.setAction(Action.AMEND);
                                DSConnection.getDefault().getRemoteTrade()
                                        .save(tradeAlloc);
                            }
                        }

                        JMSQueueAnswer answer = importMessage(jmsQueueMessage,
                                tasks);

                        // Put task as COMPLETED
                        Task task = DSConnection.getDefault().getRemoteBO()
                                .getTask(psEvent.getTaskId());
                        task.setStatus(2);
                        tasks.add(task);

                        listJMSQueueMessage.add(answer);

                        Log.system(SantPDVCollatEngine.class.getName(),
                                "Answering PSEventPDVAllocation received: "
                                        + answer.getCode());

                    } catch (Exception e) {
                        Log.error(SantPDVCollatEngine.class.getName(), e);
                    }
                }
            }
        } else if (event instanceof PSEventPDVAllocationFut) {

            PSEventPDVAllocationFut psEvent = (PSEventPDVAllocationFut) event;
            if (psEvent != null && !Util.isEmpty(psEvent.getAllocMessage())) {

                Log.system(
                        SantPDVCollatEngine.class.getName(),
                        "Receiving PSEventPDVAllocationFut message: "
                                + psEvent.getAllocMessage());

                JMSQueueMessage jmsQueueMessage = new JMSQueueMessage();
                jmsQueueMessage.setText(psEvent.getAllocMessage());
                jmsQueueMessage.setReference(psEvent.getEventType());
                jmsQueueMessage.setCorrelationId(String.valueOf(psEvent
                        .getCollatId()));

                try {
                    // complete tasks for CANCEL and ACCOUNT_CLOSING events
                    PDVUtil.completeAllocFutureTasks(psEvent.getAction(),
                            psEvent.getCollatId(), psEvent.getValDate(),
                            psEvent.getTaskId(), tasks);

                    JMSQueueAnswer answer = importMessage(jmsQueueMessage,
                            tasks);

                    // Put task as COMPLETED if handling is OK
                    Task task = DSConnection.getDefault().getRemoteBO()
                            .getTask(psEvent.getTaskId());
                    if (JMSQueueAnswer.OK.equals(answer.getCode())) {
                        task.setStatus(2);
                    }
                    tasks.add(task);

                    listJMSQueueMessage.add(answer);

                    Log.system(SantPDVCollatEngine.class.getName(),
                            "Answering PSEventPDVAllocationFut received: "
                                    + answer.getCode());

                } catch (Exception e) {
                    Log.error(SantPDVCollatEngine.class.getName(), e);
                }
            }

        }

        return listJMSQueueMessage;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.engine.optimizer.SantOptimizerBaseEngine#importMessage(java.
     * lang.String, java.util.List)
     */
    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks)
            throws Exception {
        return null;
    }

    private File writeFile(String path, String fileName, String fileContent,
                           List<Task> tasks) {
        // create a file in the file system using the content retrieved from
        // the routing message
        FileOutputStream stream = null;
        PrintStream out = null;
        File file = new File(path + fileName);
        try {
            stream = new FileOutputStream(file);
            out = new PrintStream(stream);
            out.print(fileContent);

        } catch (Exception ex) {
            Log.error(this, ex);
            tasks.add(buildTask(
                    "Unable to read the message content as an allocation file",
                    0, PDVConstants.PDV_ALLOC_EXCEPTION_TYPE, "Collateral"));
            return null;
        } finally {
            try {
                if (stream != null)
                    stream.close();
                if (out != null)
                    out.close();
            } catch (Exception e) {
                Log.error(this, e);
                return null;
            }
        }
        return file;
    }
}
