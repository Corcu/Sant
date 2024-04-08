package calypsox.tk.util;

import calypsox.repoccp.MTSPlatformReferenceHandler;
import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import calypsox.repoccp.model.lch.LCHNetPositions;
import calypsox.repoccp.model.lch.LCHObligations;
import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TransferArray;

import java.io.FileNotFoundException;
import java.util.*;

import static calypsox.repoccp.ReconCCPConstants.*;
import static calypsox.repoccp.ReconCCPUtil.*;

/**
 * ScheduledTaskRECONCCP_NETTING_GROSS
 * @author x854118
 */
public class ScheduledTaskRECONCCP_NETTING_GROSS extends ScheduledTask {

    private static final long serialVersionUID = -5013503570521828445L;

    /**
     * ST Parameter, name of the XML file that contains external trades to recon
     */
    public static final String FILE_NAME = "File Name";
    /**
     * ST Parameter, path of the XML file that contains external trades to recon
     */
    public static final String FILE_PATH = "File Path";
    /**
     * ST Parameter, path of the XLM file that contains file order
     */
    public static final String ORDER_FILES_BY = "Order Files By";
    /**
     * ST Parameter, true if execute action to move next status
     */
    public static final String MOVE_NEXT_STATUS = "Move Next Status";
    /**
     * ST Parameter, workflow bond action
     */
    public static final String WF_BOND_ACTION = "WF Bond Action";
    /**
     * ST Parameter, workflow repo action
     */
    public static final String WF_REPO_ACTION = "WF Repo Action";
    /**
     * ST Parameter, workflow repo action, if product family is none
     */
    public static final String WF_ALL_ACTION = "WF ALL Action";
    /**
     * ST Parameter, filter transfer by SettleDate equals ValDate
     */
    public static final String FILTER_TRANSFERS_BY_VALUE_DATE = "Filter xfers by settleDate";

    /**
     * ST Parameter, filter transfer by SRI not empty
     */
    public static final String FILTER_TRANSFERS_BY_SRI_NOT_EMPTY = "Filter xfers by SRI not empty";

    /**
     * ST Parameter, filter transfer by SDF
     */
    public static final String FILTER_TRANSFERS_BY_SDF = "Filter xfers by SDF";

    private final MTSPlatformReferenceHandler mtsReferenceHandler = new MTSPlatformReferenceHandler();
    /**
     * Save split transfers for move to the next status
     */
    private final List<BOTransfer> splitTransfers = new ArrayList<>();
    /**
     * Filter by transfer SettleDate equals ValDate
     */
    private boolean filterBySettleDate;

    /**
     * Filter by SRI not empty
     */
    private boolean filterBySRI;

    /**
     * Filter by SDF
     */
    private StaticDataFilter sdf;

    /**
     * Reactive processing is not yet even fully designed, so for now, imperative calls are enough
     */
    protected boolean process(DSConnection ds, PSConnection ps) {
        String fileName = getAttribute(FILE_NAME);
        String filePath = getAttribute(FILE_PATH);
        String orderBy = getAttribute(ORDER_FILES_BY);
        this.filterBySRI = getBooleanAttribute(FILTER_TRANSFERS_BY_SRI_NOT_EMPTY, true);
        this.filterBySettleDate = getBooleanAttribute(FILTER_TRANSFERS_BY_VALUE_DATE, true);
        String sdfName = getAttribute(FILTER_TRANSFERS_BY_SDF);
        if (!Util.isEmpty(sdfName)) {
            this.sdf = BOCache.getStaticDataFilter(ds, sdfName);
        }

        try {
            //Read file and store results in list of ReconCCP interface objects
            List<ReconCCP> fileObjects = readAndParseFile(fileName, filePath, orderBy);

            //Load trades from calypso with trade filter
            List<Trade> calypsoTradesFromFilter = ReconCCPUtil.loadAndFilterTrades(ds, this.getValuationDatetime(), this._timeZone, this._tradeFilter, this._filterSet);

            calypsoTradesFromFilter = filterTradeBySettlementXfers(this.getDSConnection(), calypsoTradesFromFilter,
                    this.filterBySRI, this.filterBySettleDate,
                    this.sdf, JDate.valueOf(this.getValuationDatetime()));

            if(!Util.isEmpty(fileObjects) && !Util.isEmpty(calypsoTradesFromFilter)) {

                List<LCHTrade> trades = groupByTrade(groupNetPositionsByISIN(fileObjects), fileObjects);

                if(!Util.isEmpty(trades)) {
                    //Try to match trades and then their transfers with the file transfers
                    List<ReconCCPMatchingResult> matchingResults = matchTransfers(trades, calypsoTradesFromFilter);

                    //Post process, create tasks and assign keywords to transfers and save them
                    postProcessResult(matchingResults);
                }
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.error(this, e.getCause());
        }

        return false;
    }


    @Override
    public String getTaskInformation() {
        return "Runs the CCP RECON process taking the file from attributes.";
    }

    /**
     * Try to match the external file objects to calypso objects and generate all the matched or unmatched results
     * Split transfer if LCHMarketCode not equals IT, tradeSourceName not contains MTS and has more than one obligation
     */
    private List<ReconCCPMatchingResult> matchTransfers(List<LCHTrade> fileTrades, List<Trade> calypsoTrades) {
        List<ReconCCPMatchingResult> matchingResults = new ArrayList<>();
        JDate stValueDate = JDate.valueOf(this.getValuationDatetime());
        //This may be different between clearings
        //Creating a simple class hierarchy to easily add functionality in the future is not a bad idea
        for (LCHTrade trade : fileTrades) {
            boolean matchedTrade = false;
            newTradeMatching:
            for (int i = 0; i < calypsoTrades.size(); i++) {
                Trade calypsoTrade = calypsoTrades.get(i);
                //Execute simple match
                if (trade.getBuyerSellerReference().equalsIgnoreCase(calypsoTrade.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF))) {
                    matchedTrade = true;
                    calypsoTrades.remove(calypsoTrade);
                    i--;
                    //Find calypso transfer
                    TransferArray tfArray = getSettlementTransfers(this.getDSConnection(), calypsoTrade.getLongId());

                    if (!Util.isEmpty(tfArray)) {
                        for (BOTransfer bo : tfArray.getTransfers()) {
                            if (filterXfer(this.filterBySRI, this.filterBySettleDate, this.sdf, bo, calypsoTrade, stValueDate)) {

                                List<LCHObligations> obligations = new ArrayList<>();
                                LCHNetPositions netPositions = trade.getNetPositionObligationSet();
                                if(netPositions != null){
                                    obligations = netPositions.getObligations();
                                }
                                if (!Util.isEmpty(obligations)) {
                                    if (!"IT".equalsIgnoreCase(trade.getIdentifier().getLchMarketCode()) &&
                                            !trade.getTradeSourceName().contains("MTS") && obligations.size() > 1) {
                                        //INC56 Split transfers
                                        String tradeSourceName = bo.getAttribute(XFER_ATTR_TRADE_SOURCE);
                                        if (Util.isEmpty(tradeSourceName)) {
                                            bo.setAttribute(XFER_ATTR_TRADE_SOURCE, trade.getTradeSourceName());
                                            saveTransferAttribute(this.getDSConnection(), bo.getLongId(),
                                                    XFER_ATTR_TRADE_SOURCE, trade.getTradeSourceName());
                                        }

                                        ReconCCPMatchingResult netPositionsResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
                                        if (netPositions != null && netPositions.matchReference(bo)) {
                                            netPositionsResult = netPositions.matchFields(bo);
                                            if (!netPositionsResult.hasErrors()) {
                                                if (netPositionsResult.hasWarnings()) {
                                                    netPositionsResult.setTrade(calypsoTrade);
                                                    netPositionsResult.setTransfer(bo);
                                                    matchingResults.add(netPositionsResult);
                                                }
                                                splitTransfers.addAll(splitTransfer(this.getDSConnection(), bo,
                                                        obligations));
                                            } else {
                                                netPositionsResult.setTrade(calypsoTrade);
                                                netPositionsResult.setTransfer(bo);
                                                netPositionsResult.setCashAmount("LCH".equals(netPositions.getCashReceiver())?-netPositions.getCashAmount():netPositions.getCashAmount());
                                                matchingResults.add(netPositionsResult);
                                            }
                                        } else {
                                            netPositionsResult.addError("Net Position Nominal Amount NOT " +
                                                    "matched, Calypso Transfer ID  " + bo.getLongId() + ", " +
                                                    "Nominal Amount [" + bo.getNominalAmount() + "]");
                                            netPositionsResult.setTrade(calypsoTrade);
                                            netPositionsResult.setTransfer(bo);
                                            matchingResults.add(netPositionsResult);
                                        }
                                        break newTradeMatching;
                                    } else {
                                        //Operation according to initial specification
                                        for (int j = 0; j < obligations.size(); j++) {
                                            LCHObligations obligation = obligations.get(j);
                                            if (obligation.matchReference(bo)) {
                                                obligations.remove(obligation);
                                                ReconCCPMatchingResult matchingResult = obligation.match(bo);
                                                matchingResult.setTrade(calypsoTrade);
                                                matchingResult.setCashAmount("LCH".equals(obligation.getCashReceiver())?-obligation.getCashAmountInstructed():obligation.getCashAmountInstructed());
                                                matchingResults.add(matchingResult);
                                                break newTradeMatching;
                                            }
                                        }
                                    }
                                }
                                //If reached this point, no single obligation was matched with the specific transfer from calypso
                                ReconCCPMatchingResult matchingResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
                                matchingResult.setTransfer(bo);
                                matchingResult.addNoTransfersFound(bo);
                                matchingResults.add(matchingResult);
                            }
                        }
                    }
                }
            }

            //Error for each unmatched external trade to calypso trades
            if (!matchedTrade) {
                ReconCCPMatchingResult matchingResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
                matchingResult.addTradeNotFound(trade);
                matchingResults.add(matchingResult);
            }
        }

        //Error for each unmatched calypso trade but loaded in trade filter
        for (Trade trade : calypsoTrades) {
            ReconCCPMatchingResult matchingResult = new ReconCCPMatchingResult(false, trade, new ArrayList<>(), new ArrayList<>());
            matchingResult.addCalypsoTradeNotMatchError(trade);
            matchingResults.add(matchingResult);
        }

        return matchingResults;
    }

    /**
     * Create tasks for each:
     * - Unmatched trade from file
     * - Unmatched trade from calypso
     * - Unmatched transfer from file
     * - Unmatched transfer from Calypso
     * - Matched transfer but any of the recon fields not matched
     * <p>
     * Update keywords:
     * - BuyerSellerReference keyword with its value from file for all matched trades
     * - SettlementReferenceInstructed keyword with value OK for matched calypso trades and KO for unmatched calypso trades
     */
    private void postProcessResult(List<ReconCCPMatchingResult> matchingResults) {
        TaskArray taskArray = new TaskArray();
        List<BOTransfer> transferArray = new ArrayList<>();
        JDatetime jdt = new JDatetime();
        //Trade KWD updating, error task creation etc...
        for (ReconCCPMatchingResult result : matchingResults) {
            Trade trade = result.getTrade();
            BOTransfer transfer = result.getTransfer();
            if (result.isMatched()) {
                if (result.hasErrors()) {
                    Task matchingErrorsTask = new Task(trade);
                    matchingErrorsTask.setComment(result.getUnmatchedErrors());
                    matchingErrorsTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING);
                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setDatetime(jdt);
                    matchingErrorsTask.setNewDatetime(jdt);
                    matchingErrorsTask.setTradeLongId(trade.getLongId());
                    matchingErrorsTask.setPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingErrorsTask);
                }

                if (result.hasWarnings()) {
                    Task matchingWarnTask = new Task(trade);
                    matchingWarnTask.setComment(result.getUnmatchedWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(jdt);
                    matchingWarnTask.setNewDatetime(jdt);
                    matchingWarnTask.setTradeLongId(trade.getLongId());
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingWarnTask);
                }

                if (!result.hasErrors()) {
                    if(transfer != null) {
                        transfer.setAttribute(TRADE_KEYWORD_RECON, RECON_OK);
                        double ourCashAmount = "SECURITY".equals(transfer.getTransferType())
                                ?"PAY".equals(transfer.getPayReceive())?Math.abs(transfer.getRealCashAmount()):-Math.abs(transfer.getRealCashAmount())
                                :"PAY".equals(transfer.getPayReceive())?-Math.abs(transfer.getRealSettlementAmount()):Math.abs(transfer.getRealSettlementAmount());

                        if (Math.abs(ourCashAmount - result.getCashAmount()) > Math.pow(10 , -CurrencyUtil.getCcyDecimals(transfer.getSettlementCurrency(), 2))) {
                            transfer.setAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED, Util.numberToString( result.getCashAmount(), Locale.UK, false));
                        }

                        if (trade != null && !transferArray.contains(transfer) && mtsReferenceHandler.isMTSPlatformTrade(trade)) {
                            String originalSettRef = transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
                            String buyerSellerReference = mtsReferenceHandler.getMTSSettleReference(trade);
                            transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, buyerSellerReference);
                            transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST_2, originalSettRef);
                        }
                    }
                } else if (result.hasErrors()) {
                    transfer.setAttribute(TRADE_KEYWORD_RECON, RECON_KO);
                }
                transfer.setAction(Action.valueOf("UPDATE_XFER_ATTR"));
                transferArray.add(transfer);
            } else {
                if (result.hasWarnings()) {
                    Task matchingWarnTask = new Task(trade);
                    matchingWarnTask.setComment(result.getUnmatchedWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(jdt);
                    matchingWarnTask.setNewDatetime(jdt);
                    matchingWarnTask.setTradeLongId(trade.getLongId());
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingWarnTask);
                }
                if (result.hasErrors()) {
                    Task matchingErrorsTask = new Task();
                    matchingErrorsTask.setComment(result.getUnmatchedErrors());
                    if (trade == null && transfer == null) {
                        //if (matchingErrorsTask.getComment().contains(XFER_ATTR_SETTLEMENT_REF_INST)) {
                        //    matchingErrorsTask.setEventType(EXCEPTION_MISSING_TRANSFER_RECON_CCP);
                        //} else {
                        matchingErrorsTask.setEventType(EXCEPTION_MISSING_TRADE_RECON_CCP);
                        //}
                    } else if (transfer != null) {
                        matchingErrorsTask.setEventType(EXCEPTION_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP);
                        matchingErrorsTask.setObjectLongId(transfer.getLongId());
                        transfer.setAttribute(TRADE_KEYWORD_RECON, RECON_KO);
                        transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, "");
                        transferArray.add(transfer);
                    } else {
                        matchingErrorsTask.setEventType(EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP);
                        matchingErrorsTask.setTradeLongId(result.getTrade().getLongId());
                    }
                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setDatetime(jdt);
                    matchingErrorsTask.setNewDatetime(jdt);
                    taskArray.add(matchingErrorsTask);
                }
            }
        }
        try {
            this.getDSConnection().getRemoteBackOffice().saveAndPublishTasks(taskArray, 0L, null);

            for (BOTransfer transfer : transferArray) {
                String recon = transfer.getAttribute(TRADE_KEYWORD_RECON);
             //   String settlementReferenceInstructed = transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
            //    String settlementReferenceInstructed2 = transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST_2);
                if (!Util.isEmpty(recon)) {
                    if (RECON_OK.equalsIgnoreCase(recon)) {
                        transfer.setAction(Action.UPDATE);
                        this.getDSConnection().getRemoteBackOffice().save(transfer, 0L, null);
                    /*
                    this.getDSConnection().getRemoteBackOffice().saveTransferAttribute(transfer.getLongId(),
                            TRADE_KEYWORD_RECON, recon);
                    if (RECON_OK.equalsIgnoreCase(recon)) {
                        if (!Util.isEmpty(settlementReferenceInstructed)) {
                            this.getDSConnection().getRemoteBackOffice().saveTransferAttribute(transfer.getLongId(),
                                    XFER_ATTR_SETTLEMENT_REF_INST, settlementReferenceInstructed);
                        }
                        if(!Util.isEmpty(settlementReferenceInstructed2)){
                            this.getDSConnection().getRemoteBackOffice().saveTransferAttribute(transfer.getLongId(),
                                    XFER_ATTR_SETTLEMENT_REF_INST_2, settlementReferenceInstructed2);
                        } */
                    } else {
                        this.getDSConnection().getRemoteBackOffice().saveTransferAttribute(transfer.getLongId(),
                                TRADE_KEYWORD_RECON, recon);
                    }
                }
            }
        } catch (CalypsoServiceException  e) {
            Log.error(this, e.getCause());
        }

        moveNextStatus(transferArray);
    }

    /**
     * Moves transfers whose reconciliation is OK from PENDING status to VERIFIED
     *
     * @param transferArray the transfer list to move
     */
    private void moveNextStatus(List<BOTransfer> transferArray) {
        if (!Util.isEmpty(getAttribute(MOVE_NEXT_STATUS)) && getBooleanAttribute(MOVE_NEXT_STATUS)) {
            transferArray.addAll(splitTransfers);
            transferArray = filterTransfersByAttribute(this.getDSConnection(), transferArray, TRADE_KEYWORD_RECON, RECON_OK);
            transferArray = filterTransfersByStatus(this.getDSConnection(), transferArray, Status.S_PENDING);
            moveTransfersNextStatus(this.getDSConnection(), transferArray, getAttribute(WF_BOND_ACTION),
                    getAttribute(WF_REPO_ACTION), getAttribute(WF_ALL_ACTION));
        }
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(FILE_NAME));
        attributeList.add(attribute(FILE_PATH));
        attributeList.add(attribute(ORDER_FILES_BY));
        attributeList.add(attribute(MOVE_NEXT_STATUS));
        attributeList.add(attribute(WF_BOND_ACTION));
        attributeList.add(attribute(WF_REPO_ACTION));
        attributeList.add(attribute(WF_ALL_ACTION));
        attributeList.add(attribute(FILTER_TRANSFERS_BY_VALUE_DATE));
        attributeList.add(attribute(FILTER_TRANSFERS_BY_SRI_NOT_EMPTY));
        attributeList.add(attribute(FILTER_TRANSFERS_BY_SDF));
        return attributeList;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean isValidInput(Vector messages) {
        if (Util.isEmpty(getAttribute(FILE_NAME))) {
            messages.add("File name should be filled");
        }

        if (Util.isEmpty(getAttribute(FILE_PATH))) {
            messages.add("File path should be filled");
        }

        if(Util.isEmpty(getAttribute(ORDER_FILES_BY))){
            messages.add(ORDER_FILES_BY + " should be filled");
        }

        if (Util.isEmpty(getAttribute(MOVE_NEXT_STATUS))) {
            messages.add(MOVE_NEXT_STATUS + " cannot be empty");
        } else {
            boolean moveNextStatus = getBooleanAttribute(MOVE_NEXT_STATUS);
            if (moveNextStatus) {
                if (Util.isEmpty(getAttribute(WF_BOND_ACTION))) {
                    messages.add(WF_BOND_ACTION + " cannot be empty");
                }
                if (Util.isEmpty(getAttribute(WF_REPO_ACTION))) {
                    messages.add(WF_REPO_ACTION + " cannot be empty");
                }
                if (Util.isEmpty(getAttribute(WF_ALL_ACTION))) {
                    messages.add(WF_ALL_ACTION + " cannot be empty");
                }
            }
        }

        if (Util.isEmpty(this._tradeFilter)) {
            messages.add("Please, fill the trade filter");
        }

        return Util.isEmpty(messages);
    }

    @Override
    public Vector<String> getAttributeDomain(String attr, Hashtable<String, String> currentAttr) {
        Vector<String> v = new Vector<>();
        if (!Util.isEmpty(attr)) {
            if (MOVE_NEXT_STATUS.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            } else if (WF_REPO_ACTION.equals(attr) || WF_BOND_ACTION.equals(attr) || WF_ALL_ACTION.equals(attr)) {
                return LocalCache.getDomainValues(this.getDSConnection(), "transferAction");
            }else if(ORDER_FILES_BY.equals(attr)){
                v.add(ORDER_BY_NAME);
                v.add(ORDER_BY_DATE);
                return v;
            } else if (FILTER_TRANSFERS_BY_SDF.equals(attr)) {
                v.addAll(getSDFNames(this.getDSConnection()));
                return v;
            } else if (FILTER_TRANSFERS_BY_SRI_NOT_EMPTY.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            } else if (FILTER_TRANSFERS_BY_VALUE_DATE.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            }
        }
        return super.getAttributeDomain(attr, currentAttr);
    }


}
