package calypsox.engine.im.importim;

import calypsox.engine.im.SantInitialMarginBaseEngine;
import calypsox.engine.im.TaskErrorUtil;
import calypsox.engine.im.errorcodes.SantInitialMarginCalypsoErrorCodeEnum;
import calypsox.engine.im.export.QEFJMSMessageWrapper;
import calypsox.engine.im.importim.input.SantInitialMarginImportImInput;
import calypsox.engine.im.importim.output.SantInitialMarginImportImOutput;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import calypsox.util.SantPLMarkBuilder;
import calypsox.util.SantReportingUtil;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.*;

public class SantInitialMarginImportIMEngine extends SantInitialMarginBaseEngine {

    private static final String INITIAL_MARGIN_TYPE_AF = "IM_CSD_TYPE";

    private static final String CPTY = "CPTY";

    private static final String PO = "PO";

    private static final String IM_SUB_CONTRACTS_AF = "IM_SUB_CONTRACTS";

    private static final String UNKNOWN_ERROR = "Error. Unknown error code while processing IM.";

    /**
     * type of exception to be published on TS
     */
    private static final String EX_TYPE = "IM_IMPORT_IM";

    /*
     *
     *
     */
    @Override
    protected synchronized void init(EngineContext engineContext) {
        super.init(engineContext);
        setEngineName(ENGINE_NAME);
    }

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_InitialMargin_ImportIM";

    // private static SimpleDateFormat sdf_HHmm = new SimpleDateFormat("HHmm");

    public SantInitialMarginImportIMEngine(final DSConnection dsCon, final String hostName,
                                           final int esPort) {
        super(dsCon, hostName, esPort);
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    protected boolean isAcceptedEvent(PSEvent psEvent) {
        return false;
    }

    @Override
    public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
        return false;
    }

    @Override
    public List<QEFJMSMessageWrapper> handleOutgoingJMSMessage(PSEvent event, List<Task> tasks) throws Exception {
        return null;
    }

    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks) throws Exception {
        return null;
    }

    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {

        JMSQueueAnswer answer = null;
        final List<Task> tasks = new ArrayList<Task>();

        if (externalMessage == null) {
            return true;
        }

        Log.system(SantInitialMarginImportIMEngine.class.getName(), "El external Message es --------------> " + externalMessage.getText());

        SantInitialMarginImportImInput inputReceived = new SantInitialMarginImportImInput();
        SantInitialMarginCalypsoErrorCodeEnum formatterResult = inputReceived.parseInfo(externalMessage);

        Log.info(SantInitialMarginImportIMEngine.class.getName(), " Despues del parseo:  ProcessDate --> " + inputReceived.getProcessDate() +
                " Value Date --> " + inputReceived.getValueDate() + " Contract Name--> " + inputReceived.getContractName() +
                " IM PO --> " + inputReceived.getImPo() + " IM CPTY --> " + inputReceived.getImCpty() +
                " CCY PO --> " + inputReceived.getContractCcyPo() + " CCY CPTY --> " + inputReceived.getContractCcyCpty());

        formatterResult.getCode();
        formatterResult.getMessage();

        // if parsed correctly and code with some error (means different to
        // 'NotError')
        if (formatterResult.getCode() != 0) {
            // publish TS Exception for users to resend

            Log.info(SantInitialMarginImportIMEngine.class.getName(), "Error de formateo --------------> " + formatterResult.getCode() + " - " + formatterResult.getMessage());
            StringBuilder msg = new StringBuilder("Task published for contractName: ");
            msg.append(inputReceived.getContractName()).append(" - ErrorCode: ");
            msg.append(inputReceived.getErrorCode()).append(" - ErrorMessage: ");
            if (!Util.isEmpty(inputReceived.getErrorMessage())) {
                msg.append(inputReceived.getErrorMessage());
            } else {
                msg.append("Error not defined");
            }

            Log.info(this, msg.toString());
            tasks.add(TaskErrorUtil.buildTask(EX_TYPE, msg.toString()));
        }

        // if msg could be parsed correctly
        if (formatterResult.getCode() == 0) {
            Log.info(SantInitialMarginImportIMEngine.class.getName(), "--> No hay error de formateo <--");
            int errorCode = processIm(inputReceived);

            // generate answer and send it to QEF
            try {
                SantInitialMarginImportImOutput output = new SantInitialMarginImportImOutput();
                output.setContractName(inputReceived.getContractName());
                output.setErrorCode(errorCode);

                String description = UNKNOWN_ERROR;
                SantInitialMarginCalypsoErrorCodeEnum code = SantInitialMarginCalypsoErrorCodeEnum.isValid(errorCode);
                if (code != null) {
                    description = code.getMessage();
                }
                output.setErrorDescription(description);

                final JMSQueueMessage jmsMessage = (JMSQueueMessage) externalMessage;

                answer = new JMSQueueAnswer();
                answer.setText(output.generateOutput());
                answer.setCorrelationId(jmsMessage.getCorrelationId());
                answer.setReference(jmsMessage.getReference());

                sendAnswer(answer);

                // publish task in TS in case output is an error
                if (output.getErrorCode() != SantInitialMarginCalypsoErrorCodeEnum.NoError.getCode()) {
                    tasks.add(TaskErrorUtil.buildTask(EX_TYPE, output.getErrorDescription()));
                }


            } catch (Exception ex) {
                StringBuilder msg = new StringBuilder("Contract: " + inputReceived.getContractName() + " - Couldn't send the msg to Qef: ");
                msg.append(answer.getText()).append(" - ");
                msg.append(ex.getMessage());
                Log.error(this, msg.toString());
                tasks.add(TaskErrorUtil.buildTask(EX_TYPE, msg.toString()));
            }
        }
        // TODO need to publish Task when not parsed correctly??
        if (!Util.isEmpty(tasks)) {
            publishTask(tasks);
        }

        return true;
    }

    /**
     * save IM process:
     * <li>1. Look for CSD + Ccy IM contracts
     * <li>2. Save MARGIN_CALL PLMark on them
     * <li>3. Apply action Price on them.
     * <li>4. Apply action Price on facade contract.
     *
     * @param inputReceived input message
     * @return 0 if everything was OK
     */
    private int processIm(SantInitialMarginImportImInput inputReceived) {

        Log.info(SantInitialMarginImportIMEngine.class.getName(), inputReceived.toString());

        int result = 0;

        String poIMName = inputReceived.getContractName();
        String ccy = inputReceived.getContractCcyPo();

        if (!Util.isEmpty(poIMName) && !Util.isEmpty(ccy)) {
            // get facade contract
            CollateralConfig poIMContract = new CollateralConfig();
            try {
                poIMContract = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByCode(null, poIMName);
            } catch (CollateralServiceException e1) {
                result = 2;
                Log.error(SantInitialMarginImportIMEngine.class.getName(), e1);
            }

            if (poIMContract != null) {
                String facadeId = poIMContract.getAdditionalField("IM_GLOBAL_ID");

                try {
                    if (!Util.isEmpty(facadeId)) {

                        final CollateralConfig facadeContract = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                                Integer.valueOf(facadeId));
                        List<MarginCallEntryDTO> entries = getEntriesDTO(facadeContract, inputReceived);

                        MarginCallEntryDTO entryFacade = getRelevantEntry(entries, Integer.valueOf(facadeId), true);
                        MarginCallEntryDTO entryPo = getRelevantEntry(entries, 0, true);
                        MarginCallEntryDTO entryCpty = getRelevantEntry(entries, 0, false);


                        if (entryPo == null || entryCpty == null || entryFacade == null) {

                            Map<String, Integer> csdContracts = getContractsID(facadeContract);

                            final List<Integer> mccIDs = new ArrayList<>();


                            if (entryFacade == null) {
                                mccIDs.add(facadeContract.getId());
                            }
                            if (entryPo == null) {
                                mccIDs.add(csdContracts.get(PO));
                            }
                            if (entryCpty == null) {
                                mccIDs.add(csdContracts.get(CPTY));
                            }

                            List<MarginCallEntry> resultEntries = priceWorker(mccIDs, inputReceived.getProcessDate());
                            MarginCallEntry resultEntry;
                            List<MarginCallEntryDTO> entriesDTO = new ArrayList<>();

                            if (!Util.isEmpty(resultEntries)) {
                                for (int i = 0; resultEntries.size() > i; i++) {
                                    resultEntry = resultEntries.get(i);
                                    entriesDTO.add(resultEntry.toDTO());
                                }
                            }

                            for (MarginCallEntryDTO entryDef : entriesDTO) {

                                String imType = "";
                                CollateralConfig currentMcc = CacheCollateralClient
                                        .getCollateralConfig(DSConnection
                                                .getDefault(), entryDef
                                                .getCollateralConfigId());
                                if (!Util
                                        .isEmpty(currentMcc
                                                .getAdditionalField(INITIAL_MARGIN_TYPE_AF))) {
                                    imType = currentMcc
                                            .getAdditionalField(INITIAL_MARGIN_TYPE_AF);
                                }

                                if (currentMcc == facadeContract) {
                                    entryFacade = entryDef;
                                } else if (PO.equalsIgnoreCase(imType)) {
                                    entryPo = entryDef;
                                } else if (CPTY.equalsIgnoreCase(imType)) {
                                    entryCpty = entryDef;
                                }
                            }

                        }

                        // create PLMark
                        Vector<PLMark> plmarks = new Vector<>();
                        plmarks.add(createPLMark(inputReceived, entryPo, true));
                        plmarks.add(createPLMark(inputReceived, entryCpty,
                                false));

                        // save PLMark
                        DSConnection.getDefault().getRemoteMark()
                                .saveMarksWithAudit(plmarks, true);

                        // action Price on children entries
                        applyPrice(entryPo);
                        applyPrice(entryCpty);

                        // action Price on Facade
                        applyPrice(entryFacade);


                    }
                } catch (PersistenceException e) {
                    result = 1;
                    Log.error(this, "Couldn't save PLMarks: " + e.getMessage());
                }
            }
        }

        return result;
    }

    private List<MarginCallEntryDTO> getEntriesDTO(CollateralConfig facadeContract,
                                                   SantInitialMarginImportImInput inputReceived) {
        List<MarginCallEntryDTO> childrenEntries = null;

        String subContractIds = facadeContract.getAdditionalField(IM_SUB_CONTRACTS_AF);
        String[] children = subContractIds.trim().split(",");

        final List<String> from = new ArrayList<String>();
        from.add(" margin_call_entries ");
        from.add(" mrgcall_config ");

        final StringBuilder sqlWhere = new StringBuilder();
        sqlWhere.append(" mrgcall_config.mrg_call_def = margin_call_entries.mcc_id ");

        // children entries + facade entry
        sqlWhere.append(" AND margin_call_entries.mcc_id IN (");
        sqlWhere.append(Util.collectionToString(Arrays.asList(children), ","));
        sqlWhere.append(",").append(facadeContract.getId());
        sqlWhere.append(")");

        JDate today = inputReceived.getProcessDate();
        // process start date
        sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) = ");
        sqlWhere.append(Util.date2SQLString(today));

        // Base Ccy
        sqlWhere.append(" AND mrgcall_config.currency_code = '");
        sqlWhere.append(inputReceived.getContractCcyPo()).append("'");

        try {
            childrenEntries = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getMarginCallEntriesDTO(sqlWhere.toString(), from, true);
        } catch (PersistenceException ex) {
            Log.error(this, "Couldn't get SantReportingService: " + ex.getMessage());
        } catch (RemoteException e) {
            Log.error(this, "Couldn't get MarginCallEntriesDTO: " + e.getMessage());
        }

        return childrenEntries;
    }

    private MarginCallEntryDTO getRelevantEntry(List<MarginCallEntryDTO> entries, int contractId, boolean isPo) {
        MarginCallEntryDTO entry = null;

        for (MarginCallEntryDTO currentEntry : entries) {
            // return related entry if contractId specified
            if (contractId != 0 && contractId == currentEntry.getCollateralConfigId()) {
                entry = currentEntry;
            } else if (contractId == 0) {
                // if no contractId specified, look for child
                CollateralConfig currentMcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        currentEntry.getCollateralConfigId());
                String imType = currentMcc.getAdditionalField(INITIAL_MARGIN_TYPE_AF);

                if (!Util.isEmpty(imType) && isPo && PO.equalsIgnoreCase(imType)) {
                    entry = currentEntry;
                } else if (!Util.isEmpty(imType) && !isPo && CPTY.equalsIgnoreCase(imType)) {
                    entry = currentEntry;
                }
            }
        }

        return entry;
    }

    private HashMap<String, Integer> getContractsID(CollateralConfig collateral) {

        String additionalField;
        HashMap<String, Integer> csdContracts = new HashMap<String, Integer>();

        additionalField = collateral.getAdditionalField(IM_SUB_CONTRACTS_AF);

        String[] children = additionalField.trim().split(",");

        for (String mccID : children) {

            CollateralConfig currentMcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                    Integer.parseInt(mccID));
            String imType = currentMcc.getAdditionalField(INITIAL_MARGIN_TYPE_AF);

            if (PO.equalsIgnoreCase(imType)) {
                csdContracts.put(PO, currentMcc.getId());
            } else if (CPTY.equalsIgnoreCase(imType)) {
                csdContracts.put(CPTY, currentMcc.getId());
            }
        }

        return csdContracts;
    }

    private PLMark createPLMark(
            SantInitialMarginImportImInput inputReceived,
            MarginCallEntryDTO entry, boolean isPo) {
        PLMark plMark = new SantPLMarkBuilder().build();

        List<MarginCallDetailEntryDTO> detailEntries = entry.getDetailEntries();

        if (!Util.isEmpty(detailEntries)) {
            MarginCallDetailEntryDTO detailEntry = detailEntries.get(0);

            if (detailEntry != null) {
                try {
                    Trade trade = DSConnection.getDefault().getRemoteTrade()
                            .getTrade(detailEntry.getTradeId());

                    if (trade != null) {

                        Log.info(SantInitialMarginImportIMEngine.class.getName(), "La trade no es null --------------> " + trade.getLongId());

                        final CollateralConfig contract = CacheCollateralClient
                                .getCollateralConfig(DSConnection.getDefault(),
                                        Integer.valueOf(entry.getCollateralConfigId()));

                        String pricingEnvName = contract.getPricingEnvName();

                        if (Util.isEmpty(pricingEnvName)) {
                            pricingEnvName = "DirtyPrice";
                        }

                        JDate valueDate = entry.getValueDatetime().getJDate(
                                TimeZone.getDefault());

                        // get plmark to put our MARGIN_CALL value inside
                        plMark = CollateralUtilities.createPLMarkIfNotExists(
                                trade, DSConnection.getDefault(),
                                pricingEnvName, valueDate);

                        PLMarkValue plMarkValue = new PLMarkValue();
                        plMarkValue.setMarkName(PricerMeasure.S_MARGIN_CALL);

                        double value = 0.0;
                        String ccy = "";
                        if (isPo) {
                            value = inputReceived.getImPo();
                            ccy = inputReceived.getContractCcyPo();
                        } else {
                            value = inputReceived.getImCpty();
                            ccy = inputReceived.getContractCcyCpty();
                        }

                        plMarkValue.setCurrency(ccy);
                        plMarkValue.setOriginalCurrency(ccy);

                        plMarkValue.setMarkValue(value);

                        plMark.addPLMarkValue(plMarkValue);
                        plMark.setType("PL");
                    } else {
                        Log.info(SantInitialMarginImportIMEngine.class.getName(), "La trade del detailEntry es null  --> " + detailEntry.toString());
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(SantInitialMarginImportIMEngine.class.getName(), "Couldn't get trade: " + e.getMessage());
                } catch (RemoteException e) {
                    Log.error(SantInitialMarginImportIMEngine.class.getName(), "Couldn't get PLMark: " + e.getMessage());
                }
            } else {
                Log.info(SantInitialMarginImportIMEngine.class.getName(), "El detailEntry es null. La entry es --> " + entry.getId());
            }
        }

        return plMark;
    }


    private static void applyPrice(MarginCallEntryDTO entry) {
        try {
            final CollateralConfig contract = CacheCollateralClient
                    .getCollateralConfig(DSConnection.getDefault(),
                            Integer.valueOf(entry.getCollateralConfigId()));

            final List<Integer> mccIDs = new ArrayList<Integer>();
            mccIDs.add(contract.getId());

            List<MarginCallEntry> resultEntries = priceWorker(mccIDs, entry.getProcessDate());

            if (Util.isEmpty(resultEntries)) {
                StringBuffer msg = new StringBuffer("Action ");
                msg.append("Price").append(
                        " could not be applied for Entry with id ");
                msg.append(entry.getId());
                Log.error(SantInitialMarginImportIMEngine.class.getName(), msg.toString());
            }

            Log.info(SantInitialMarginImportIMEngine.class.getName(), "Price successfully applied on Entry with id " + entry.getId());
        } catch (Exception e) {
            StringBuffer msg = new StringBuffer("Action ");
            msg.append("Price could not be applied for Entry with id ");
            msg.append(entry.getId()).append(". Error: ").append(e.getMessage());
            Log.error(SantInitialMarginImportIMEngine.class.getName(), msg.toString());
        }
    }

    @SuppressWarnings("deprecation")
    private static List<MarginCallEntry> priceWorker(List<Integer> mccIDs, JDate processDate) {
        MarginCallReportTemplate template = new MarginCallReportTemplate();

        template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES,
                Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION,
                Boolean.FALSE);
        template.put(MarginCallReportTemplate.CONTRACT_TYPES, "CSD,CSA_FACADE");

        ExecutionContext context = ExecutionContext.getInstance(
                ServiceRegistry.getDefaultContext(),
                ServiceRegistry.getDefaultExposureContext(), template);

        MarginCallConfigFilter mccFilter = SantMCConfigFilteringUtil.getInstance().buildMCConfigFilter(processDate, mccIDs);
        context.setFilter(mccFilter);

        final CollateralManager marginCallManager = CollateralManager.getInstance(context);
        List<MarginCallEntry> entries = marginCallManager.createEntries(context.getFilter(), new ArrayList<>());

        CollateralTaskWorker rePriceTaskWorker = CollateralTaskWorker.getInstance(CollateralTaskWorker.TASK_REPRICE, context, entries);
        rePriceTaskWorker.process();

        return entries;
    }


}
