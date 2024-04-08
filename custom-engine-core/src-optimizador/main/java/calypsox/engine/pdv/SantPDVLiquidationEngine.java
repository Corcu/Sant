package calypsox.engine.pdv;

import calypsox.engine.optimizer.SantOptimizerBaseEngine;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.collateral.allocation.importer.jms.PDVJMSQueueAnswer;
import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.pdv.importer.PDVUtil.EnumMessageType;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

public class SantPDVLiquidationEngine extends SantOptimizerBaseEngine {

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_ImportMessageEngine_PDVLiquidation";

    public SantPDVLiquidationEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    protected JMSQueueAnswer importMessage(JMSQueueMessage jmsMessage,
                                           List<Task> tasks) throws Exception {
        String fileContent = jmsMessage.getText();
        String fileName = (Util.isEmpty(jmsMessage.getCorrelationId()) ? "PDVLiquidation.txt"
                : jmsMessage.getCorrelationId());

        //MIG 16 mini change
        PDVJMSQueueAnswer importAnswer = new PDVJMSQueueAnswer(jmsMessage.getText());

        StringBuffer returnCode = new StringBuffer();
        returnCode.append(fileContent);
        returnCode.append("|");
        returnCode.append(importPDVLiquidationFile(fileContent, fileName,
                new JDatetime(), tasks, importAnswer) ? JMSQueueAnswer.OK
                : JMSQueueAnswer.KO);
        returnCode.append("|");
        returnCode.append(importAnswer.getDescription());
        returnCode.append("|");
        importAnswer.setCode(returnCode.toString());
        return importAnswer;
    }

    @SuppressWarnings("unused")
    private boolean importPDVLiquidationFile(String fileContent,
                                             String fileName, JDatetime jDatetime, List<Task> tasks,
                                             PDVJMSQueueAnswer importAnswer) {
        if (!Util.isEmpty(fileContent)) {

            Log.system(SantPDVLiquidationEngine.class.getName(),
                    "Processing message: " + fileContent);

            List<String> fields = Arrays.asList(PDVUtil.LIQUIDATION_ID_MESSAGE,
                    PDVUtil.LIQUIDATION_FO_SYSTEM,
                    PDVUtil.LIQUIDATION_ID_MUREX,
                    PDVUtil.LIQUIDATION_SETTLEMENT_DATE,
                    PDVUtil.LIQUIDATION_SETTLEMENT_STATUS,
                    PDVUtil.LIQUIDATION_COMMENT);

            HashMap<String, String> values = (HashMap<String, String>) PDVUtil
                    .getFieldValues(EnumMessageType.LIQUIDATION_MESSAGE,
                            fileContent, fields);

            if (!Util.isEmpty(values.keySet())) {
                String idMessage = values.get(PDVUtil.LIQUIDATION_ID_MESSAGE);
                String foSystem = values.get(PDVUtil.LIQUIDATION_FO_SYSTEM);
                String idTrade = values.get(PDVUtil.LIQUIDATION_ID_MUREX);
                String settlementDate = values
                        .get(PDVUtil.LIQUIDATION_SETTLEMENT_DATE);
                String status = values
                        .get(PDVUtil.LIQUIDATION_SETTLEMENT_STATUS);
                String comment = values.get(PDVUtil.LIQUIDATION_COMMENT);
                Trade trade = null;
                if (!Util.isEmpty(idTrade) && !Util.isEmpty(foSystem)) {
                    idTrade = idTrade.trim();
                    foSystem = foSystem.trim();
                    TradeArray tradeArray = TradeInterfaceUtils
                            .getTradeByBORefAndBOSystem(foSystem, idTrade);
                    if (tradeArray != null && tradeArray.size() > 0) {
                        trade = tradeArray.get(0);
                    } else {
                        String description = "No Trade found with 'BO_REFERENCE'="
                                + idTrade + " and 'BO_SYSTEM'=" + foSystem;
                        handleExceptionInfo(tasks, importAnswer, description);
                        return false;
                    }
                } else {
                    String description = "No Trade Id or FO reference found";
                    handleExceptionInfo(tasks, importAnswer, description);
                    return false;
                }
                if (trade != null) {
                    Vector<String> statuses = LocalCache.getDomainValues(
                            DSConnection.getDefault(),
                            "MarginCallAllocation.SETTLEMENT_STATUS");
                    if (!Util.isEmpty(statuses) && statuses.contains(status)) {
                        // update Trade keyword
                        trade.addKeyword(
                                PDVUtil.SETTLEMENT_STATUS_TRADE_KEYWORD, status);
                        trade.setAction(Action.AMEND);

                        try {
                            DSConnection.getDefault().getRemoteTrade().save(trade);

                            Log.info(SantPDVLiquidationEngine.class.getName(), "Trade id :" + trade.getLongId() + " successfuly updated");
                        } catch (RemoteException e1) {
                            String descritption = "Unable to update trade: "
                                    + trade.getLongId();
                            Log.error(SantPDVLiquidationEngine.class
                                    .getName(), descritption);
                            handleExceptionInfo(tasks, importAnswer,
                                    descritption);
                            return false;
                        }

                        // update MC entries
                        if (!Util
                                .isEmpty(trade
                                        .getKeywordValue(PDVConstants.MC_CONTRACT_NUMBER_TRADE_KEYWORD))) {
                            int collateralConfigId = trade
                                    .getKeywordAsInt(PDVConstants.MC_CONTRACT_NUMBER_TRADE_KEYWORD);
                            final List<Integer> contractIds = new ArrayList<Integer>();
                            contractIds.add(collateralConfigId);

                            final int calculationOffSet = ServiceRegistry
                                    .getDefaultContext().getValueDateDays()
                                    * -1;
                            final JDate processingDate = JDate.getNow();
                            final JDate valuationDate = Holiday.getCurrent()
                                    .addBusinessDays(
                                            processingDate,
                                            DSConnection.getDefault()
                                                    .getUserDefaults()
                                                    .getHolidays(),
                                            calculationOffSet);
                            ExecutionContext executionContext = initExecutionContext(
                                    processingDate.getJDatetime(TimeZone
                                            .getDefault()),
                                    valuationDate.getJDatetime(TimeZone
                                            .getDefault()));

                            List<String> errors = new ArrayList<>();
                            List<MarginCallEntry> entries = null;
                            entries = CollateralManagerUtil.loadEntries(
                                    contractIds, executionContext, errors);
                            if (!Util.isEmpty(entries)) {
                                for (MarginCallEntry marginCallEntry : entries) {
                                    for (MarginCallAllocation alloc : marginCallEntry
                                            .getAllocations()) {
                                        if (idTrade
                                                .equals(alloc
                                                        .getAttribute(PDVConstants.COLLAT_NUM_FRONT_ID_FIELD))
                                                && foSystem
                                                .equals(alloc
                                                        .getAttribute(PDVConstants.COLLAT_FO_SYSTEM_FIELD))) {
                                            alloc.addAttribute(
                                                    PDVConstants.SETTLEMENT_STATUS_ALLOC_ATTR,
                                                    status);
                                        }
                                    }
                                    MarginCallEntryDTO mcEntryDTO = marginCallEntry
                                            .toDTO();
                                    // TODO: delete with upgrade 1.6.3
                                    if (Util.isEmpty(mcEntryDTO
                                            .getCashPositions())) {
                                        mcEntryDTO
                                                .setCashPosition(new PreviousPositionDTO<CashPositionDTO>());
                                    }
                                    if (mcEntryDTO != null) {
                                        try {
                                            ServiceRegistry
                                                    .getDefault(
                                                            DSConnection
                                                                    .getDefault())
                                                    .getCollateralServer()
                                                    .save(mcEntryDTO,
                                                            "UPDATE",
                                                            TimeZone.getDefault());
                                        } catch (RemoteException e) {
                                            String descritption = "Unable to update entry with settlement status: "
                                                    + collateralConfigId;
                                            Log.error(
                                                    SantPDVLiquidationEngine.class
                                                            .getName(),
                                                    descritption);
                                            handleExceptionInfo(tasks,
                                                    importAnswer, descritption);
                                            return false;
                                        }
                                        Log.info(
                                                SantPDVLiquidationEngine.class
                                                        .getName(),
                                                "Entry with id "
                                                        + marginCallEntry
                                                        .getId()
                                                        + " successfully saved for the contract "
                                                        + collateralConfigId);
                                    }
                                }
                            }
                        }
                    } else {
                        String description = "Invalid settlement status:"
                                + status;
                        handleExceptionInfo(tasks, importAnswer, description);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void handleExceptionInfo(List<Task> tasks,
                                     PDVJMSQueueAnswer importAnswer, String description) {
        tasks.add(buildTask(description, 0,
                PDVConstants.PDV_LIQUIDATION_EXCEPTION_TYPE, "Collateral"));
        importAnswer.setDescription(description);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.engine.optimizer.SantOptimizerBaseEngine#isAcceptedEvent(com
     * .calypso.tk.event.PSEvent)
     */
    protected boolean isAcceptedEvent(PSEvent psevent) {
        return false;
    }

    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks)
            throws Exception {
        return null;
    }

    private ExecutionContext initExecutionContext(
            final JDatetime processingDate, final JDatetime valuationDate) {

        final ExecutionContext ec = CollateralManagerUtil.getDefaultExecutionContext();
        ec.setProcessDate(processingDate.getJDate(TimeZone.getDefault()));
        // set the default user action to use
        ec.setUserAction("Price");
        return ec;
    }
}
