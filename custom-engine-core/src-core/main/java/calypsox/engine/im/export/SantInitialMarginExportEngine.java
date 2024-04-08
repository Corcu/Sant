package calypsox.engine.im.export;

import calypsox.engine.im.SantInitialMarginBaseEngine;
import calypsox.engine.im.TaskErrorUtil;
import calypsox.engine.im.errorcodes.SantInitialMarginCalypsoErrorCodeEnum;
import calypsox.engine.im.errorcodes.SantInitialMarginQefErrorCodeEnum;
import calypsox.engine.im.export.input.SantInitialMarginExportInput;
import calypsox.engine.im.export.output.SantInitialMarginExportOutput;
import calypsox.engine.im.export.output.SantInitialMarginExportOutputFactory;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.event.PSEventSantInitialMarginExport;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.util.SantCalypsoUtilities;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class SantInitialMarginExportEngine extends SantInitialMarginBaseEngine {

    /**
     * type of exception to be published on TS
     */
    private static final String EX_TYPE = "IM_INFO_TO_QEF";

    private static final String IM_QEF_EXPORT_FILTER = "IM_QEF_EXPORT_FILTER";

    private static final String IM_EXPORT_QEF_DATE = "IM_EXPORT_QEF_DATE";

    private static final String IM_SUB_CONTRACTS_CC_ATTR = "IM_SUB_CONTRACTS";

    private static final String END_OF_MESSAGE_STR = "END OF MESSAGE";

    private static final String IM_CSD_TYPE_STR = "IM_CSD_TYPE";

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_InitialMargin_ExportToQef";

    private TradeMarkerRunnable marker = null;
    private TradeArray trades;
    private boolean activateMarker = false;

    /*
     *
     *
     */
    @Override
    protected synchronized void init(EngineContext engineContext) {
        super.init(engineContext);
        setEngineName(ENGINE_NAME);
    }

    public SantInitialMarginExportEngine(
            final DSConnection dsCon, final String hostName, final int esPort) {
        super(dsCon, hostName, esPort);
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
        return false;
    }

    @Override
    protected boolean isAcceptedEvent(PSEvent psEvent) {
        return (psEvent instanceof PSEventSantInitialMarginExport);
    }

    @Override
    public List<QEFJMSMessageWrapper> handleOutgoingJMSMessage(PSEvent event, List<Task> tasks)
            throws Exception {
        if (isAcceptedEvent(event)) {
            return processMessagesEvent(event);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<QEFJMSMessageWrapper> processMessagesEvent(PSEvent event) {
        List<QEFJMSMessageWrapper> listJMSQueueMessage = new ArrayList<>();

        PSEventSantInitialMarginExport mcQefEvent = (PSEventSantInitialMarginExport) event;
        JDate processDate = null;

        try {
            // check if the marker its running or not
            checkMarker();
            QEFMessageContent messageContent;
            if (mcQefEvent != null) {
                List<MarginCallDetailEntryDTO> entries = new ArrayList<>();
                processDate = mcQefEvent.getProcessDate();
                int contractid = mcQefEvent.getContractid();

                //Load the FacadeContract
                CollateralConfig facadecontract =
                        CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractid);

                //Load VM and IM contracts from the Facade.
                messageContent = processFacadeContract(facadecontract, processDate, mcQefEvent.getFacadeEntryId());

                if (!messageContent.isEmpty()) {

                    JMSQueueMessage jmsQEFContentMessage = new JMSQueueMessage();
                    JMSQueueMessage jmsEOMMessage = new JMSQueueMessage();
                    // get correlation id from PSEvent
                    String correlationID = QEFCorrelationIdGenerator.generateNewCorrelationId();
                    // create jmsMessage
                    jmsQEFContentMessage.setCorrelationId(correlationID);
                    jmsQEFContentMessage.setText(messageContent.getText());
                    //EOM Message
                    jmsEOMMessage.setCorrelationId(correlationID);
                    jmsEOMMessage.setText(END_OF_MESSAGE_STR);

                    writeLog(correlationID, jmsQEFContentMessage);
                    writeLog(correlationID, jmsEOMMessage);

                    listJMSQueueMessage.add(new QEFJMSMessageWrapper(jmsQEFContentMessage, messageContent.entryId, false));
                    listJMSQueueMessage.add(new QEFJMSMessageWrapper(jmsEOMMessage, messageContent.entryId, true));

                    //Mark affected trades
                    if (activateMarker) {
                        marker.setDate(correlationID);
                        marker.addTrades(this.trades);
                    }
                } else {
                    getDS().getRemoteTrade().eventProcessed(event.getLongId(), getEngineName());
                    String contractError = "0";
                    if (mcQefEvent.getContractid() != 0) {
                        CollateralConfig contract =
                                CacheCollateralClient.getCollateralConfig(
                                        DSConnection.getDefault(), mcQefEvent.getContractid());
                        contractError = contract.getName();
                    }
                    final List<Task> tasks = new ArrayList<>();
                    if (Util.isEmpty(entries)) {
                        Log.system(
                                SantInitialMarginExportEngine.class.getName(),
                                "No entries found for the contract: " + contractError);
                        tasks.add(
                                TaskErrorUtil.buildTask(
                                        EX_TYPE, "No entries found for the contract: " + contractError));
                    } else {
                        Log.system(
                                SantInitialMarginExportEngine.class.getName(),
                                "Contract " + contractError + " is not configured correctly");
                        tasks.add(
                                TaskErrorUtil.buildTask(
                                        EX_TYPE, "Contract " + contractError + " is not configured correctly"));
                    }

                    publishTask(tasks);
                    return listJMSQueueMessage;
                }
            }
        } catch (RemoteException e) {
            setTerminateMarker();
            StringBuilder msg = new StringBuilder("Couldn't process the event: ");
            msg.append(event.getLongId()).append(" - ");
            msg.append(e.getMessage());

            Log.error(this, msg.toString());
        }
        return listJMSQueueMessage;
    }

    public QEFMessageContent processFacadeContract(CollateralConfig facadeContract, JDate processDate, int facadeEntryId) throws CollateralServiceException {
        List<CollateralConfig> listVMContracts = new ArrayList<>();

        CollateralConfig imPoContract = new CollateralConfig();
        CollateralConfig imCptyContract = new CollateralConfig();

        QEFMessageContent messageContent = new QEFMessageContent(facadeEntryId);

        //Load VM and IM contracts from the Facade.
        if (facadeContract != null && !Util.isEmpty(facadeContract.getAdditionalField(IM_SUB_CONTRACTS_CC_ATTR))) {

            Vector<Integer> childIds = getSubContractIds(facadeContract);

            for (final Integer id : childIds) {
                final CollateralConfig mcc =
                        CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), id);
                if (mcc != null) {
                    if (Util.isEmpty(mcc.getAdditionalField(IM_CSD_TYPE_STR)) && ("CSA").equalsIgnoreCase(mcc.getContractType())) {
                        listVMContracts.add(mcc);
                    } else {
                        if (isImPoContract(mcc)) {
                            imPoContract = mcc;
                        }
                        if (isImCptyContract(mcc)) {
                            imCptyContract = mcc;
                        }
                    }
                }
            }
            listVMContracts = listVMContracts.stream().filter(cc -> isProcessDateAfterAF(cc, processDate)).collect(Collectors.toList());
            messageContent.setText(buildMessageInfo(listVMContracts, processDate, imPoContract, imCptyContract));
        }
        return messageContent;
    }

    /**
     * @param listVMContracts
     * @param processDate
     * @param imPoContract
     * @param imCptyContract
     * @return
     * @throws CollateralServiceException
     */
    private StringBuilder buildMessageInfo(List<CollateralConfig> listVMContracts, JDate processDate, CollateralConfig imPoContract, CollateralConfig imCptyContract) throws CollateralServiceException {
        StringBuilder messageContent = new StringBuilder();
        for (CollateralConfig vmContract : listVMContracts) {
            List<MarginCallDetailEntryDTO> detailEntries;
            String where = buildQuery(vmContract, processDate);
            List<String> from = buildFrom(where);
            detailEntries = ServiceRegistry.getDefault()
                    .getDashBoardServer()
                    .loadMarginCallDetailEntries(where, from);
            // fill output info for each detail entry
            if (!Util.isEmpty(detailEntries)) {
                String pricingEnvName = vmContract.getPricingEnvName();
                // Fill info
                messageContent = messageContent.append(fillInfoNew(detailEntries, pricingEnvName, vmContract, imPoContract, imCptyContract));
            }
        }
        return messageContent;
    }

    /**
     * @param vmContract
     * @param processDate
     * @return
     */
    private boolean isProcessDateAfterAF(CollateralConfig vmContract, JDate processDate) {
        boolean res = true;
        if (!Util.isEmpty(vmContract.getAdditionalField(IM_EXPORT_QEF_DATE)) && JDate.valueOf(vmContract.getAdditionalField(IM_EXPORT_QEF_DATE))
                .after(processDate)) {
            res = false;
            Log.info(
                    SantInitialMarginExportEngine.class.getName(),
                    "ProcessDate can't be earlier than Additional Field 'IM_EXPORT_QEF_DATE'");
        }
        return res;
    }

    /**
     * @param facadeContract
     * @return
     */
    private Vector<Integer> getSubContractIds(CollateralConfig facadeContract) {
        Vector<Integer> childIds = new Vector<>();
        StringTokenizer st = new StringTokenizer(facadeContract.getAdditionalField(IM_SUB_CONTRACTS_CC_ATTR), ",");
        while (st.hasMoreTokens()) {
            childIds.add(Integer.valueOf(st.nextToken()));
        }
        return childIds;
    }

    /**
     * @param cc
     * @return
     */
    private boolean isImPoContract(CollateralConfig cc) {
        return checkContractAdditionalField(cc, IM_CSD_TYPE_STR, "PO");
    }

    /**
     * @param cc
     * @return
     */
    private boolean isImCptyContract(CollateralConfig cc) {
        return checkContractAdditionalField(cc, IM_CSD_TYPE_STR, "CPTY");
    }

    /**
     * @param cc
     * @param additionalField
     * @param expectedValue
     * @return
     */
    private boolean checkContractAdditionalField(CollateralConfig cc, String additionalField, String expectedValue) {
        boolean res = false;
        if (expectedValue.equals(cc.getAdditionalField(additionalField))) {
            res = true;
        }
        return res;
    }

    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks) throws Exception {
        return null;
    }

    @Override
    protected JMSQueueAnswer importMessage(JMSQueueMessage jmsMessage, List<Task> tasks)
            throws Exception {
        return null;
    }

    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
        final List<Task> tasks = new ArrayList<>();

        if (externalMessage == null) {
            return true;
        }

        // Display the message
        Log.info(this, externalMessage.getText());

        // Try to import the object
        if (externalMessage instanceof JMSQueueMessage) {
            // ACK/NACK treatment
            SantInitialMarginExportInput inputReceived = new SantInitialMarginExportInput();
            SantInitialMarginCalypsoErrorCodeEnum formatterResult =
                    inputReceived.parseInfo(externalMessage);

            int errorCode = formatterResult.getCode();

            // if parsed correctly and code with some error (means different to
            // 'NotError')
            if ((errorCode == 0)
                    && (inputReceived.getErrorCode()
                    != SantInitialMarginQefErrorCodeEnum.NoError.getCode())) {
                // publish TS Exception for users to resend
                Task task = TaskErrorUtil.buildTask(EX_TYPE, inputReceived, externalMessage);
                tasks.add(task);
                publishTask(tasks);

                StringBuilder msg = new StringBuilder("Task published for contractName: ");
                msg.append(inputReceived.getContractName()).append(" - ErrorCode: ");
                msg.append(inputReceived.getErrorCode());
                Log.info(this, msg.toString());
            }
        }

        return true;
    }

    protected String buildQuery(CollateralConfig contract, JDate processDate) {
        StringBuffer where = new StringBuffer();

        // filter by process date = today
        String dateString = " margin_call_entries.process_datetime ";

        if (processDate != null) {
            JDatetime startOfDay = new JDatetime(processDate, TimeZone.getDefault());
            startOfDay = startOfDay.add(-1, 0, 1, 0, 0);
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.datetime2SQLString(startOfDay));
        }

        if (processDate != null) {
            JDatetime endOfDay = new JDatetime(processDate, TimeZone.getDefault());
            endOfDay = endOfDay.add(0, 0, 0, 59, 999);
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.datetime2SQLString(endOfDay));
        }

        // filter by mcc id
        if ((contract != null) && (contract.getId() != 0)) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append("margin_call_entries");
            where.append(".mcc_id ");
            where.append("=");
            where.append("'");
            where.append(String.valueOf(contract.getId()));
            where.append("'");
        }

        if (where.length() > 0) {
            where.append(" AND margin_call_detail_entries.mc_entry_id = margin_call_entries.id");
        }

        return where.toString();
    }

    protected List<String> buildFrom(String where) {
        List<String> result = new ArrayList<>();

        if (!(Util.isEmpty(where))) {
            if (where.contains("margin_call_entries")) {
                result.add("margin_call_entries");
            }

            if (where.contains("margin_call_detail_entries")) {
                result.add("margin_call_detail_entries");
            }

            if (where.contains("mrgcall_config")) {
                result.add("mrgcall_config");
            }

            if (where.contains("clearing_member_configuration")) {
                result.add("clearing_member_configuration");
            }

            if (where.contains("clearing_member_service")) {
                result.add("clearing_member_service");
            }

            if (where.contains("clearing_service")) {
                result.add("clearing_service");
            }
        }

        return result;
    }

    /**
     * @author epalaobe
     * <p>New fill info to SantInitialMarginExportOutput
     */
    private StringBuilder fillInfoNew(
            final List<MarginCallDetailEntryDTO> entries,
            final String pricingEnvName,
            final CollateralConfig vmcontract,
            final CollateralConfig impocontract,
            final CollateralConfig imcptycontract) {

        StringBuilder outputText = new StringBuilder("");
        StaticDataFilter sdf = getSDF(vmcontract, impocontract);

        long[] idsTrades = new long[entries.size()];
        HashMap<Long, MarginCallDetailEntryDTO> hashMapMCDDTO = new HashMap<>();

        for (int i = 0; i < entries.size(); i++) {
            MarginCallDetailEntryDTO entry = entries.get(i);
            long tradeId = entry.getTradeId();
            idsTrades[i] = tradeId;
            hashMapMCDDTO.put(tradeId, entry);
        }

        TradeArray tradeArray = new TradeArray();
        try {
            tradeArray = SantCalypsoUtilities.getInstance().getTradesWithTradeFilter(idsTrades);
            this.trades = tradeArray;
        } catch (CalypsoServiceException e) {
            Log.error(SantInitialMarginExportEngine.class.getName(), e.getMessage());
        }

        if (null != tradeArray) {
            for (int i = 0; i < tradeArray.size(); i++) {
                Trade trade = tradeArray.get(i);
                SantInitialMarginExportOutput currentEntry =
                        SantInitialMarginExportOutputFactory.getSantInitialMarginExportOutput(
                                sdf,
                                trade,
                                hashMapMCDDTO.get(trade.getLongId()),
                                vmcontract,
                                impocontract,
                                imcptycontract,
                                pricingEnvName);

                if (currentEntry != null && currentEntry.isFilterAccepted()) {
                    currentEntry.fillInfoByProduct();
                    outputText.append(currentEntry.generateOutput());
                }
            }
        }
        return outputText;
    }

    private void setTerminateMarker() {
        if (this.marker != null && this.marker.isAlive()) {
            marker.setTerminateMarker(true);
        }
    }

    private void checkMarker() {
        try {
            List<String> activate =
                    DSConnection.getDefault().getRemoteReferenceData().getDomainValues("ActivateQEFMarker");
            if (!Util.isEmpty(activate)
                    && !"false".equals(activate.get(0))
                    && !Util.isEmpty(activate.get(0))
                    && "true".equals(activate.get(0))) {
                activateMarker = true;
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Cannot found ActivateQEFMarker " + e);
        }
        if (activateMarker) {
            if (this.marker == null) {
                this.marker = new TradeMarkerRunnable();
                this.marker.start();
            }
            if (this.marker != null && !this.marker.isAlive()) {
                this.marker = new TradeMarkerRunnable();
                this.marker.start();
            }
        }
    }

    private StaticDataFilter getSDF(
            final CollateralConfig vmcontract, final CollateralConfig impocontract) {
        String sdfName = vmcontract.getAdditionalField(IM_QEF_EXPORT_FILTER);
        if (Util.isEmpty(sdfName)) {
            sdfName = impocontract.getAdditionalField(IM_QEF_EXPORT_FILTER);
        }
        return BOCache.getStaticDataFilter(DSConnection.getDefault(), sdfName);
    }

    public class QEFMessageContent {
        StringBuilder msgContents;
        int entryId;

        public QEFMessageContent(int entryId) {
            this.entryId = entryId;
        }

        public void setText(StringBuilder msgContents) {
            this.msgContents = msgContents;
        }

        public String getText() {
            return msgContents.toString();
        }

        public boolean isEmpty() {
            return msgContents == null || msgContents.length() == 0;
        }

    }

    private void writeLog(String correlationID, JMSQueueMessage jmsMessage) {
        Log.system(
                SantInitialMarginExportEngine.class.getName(),
                "Sending msg [correlation Id]: "
                        + correlationID
                        + " message in columns: "
                        + jmsMessage.getText());
    }
}
