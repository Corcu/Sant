package calypsox.tk.util;

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
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TransferArray;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;

import static calypsox.repoccp.ReconCCPConstants.*;
import static calypsox.repoccp.ReconCCPUtil.*;

/**
 * ScheduledTaskRECONCCP_NETTING_MTINEXTDAY
 *
 * @author x854118
 */
public class ScheduledTaskRECONCCP_NETTING_MTINEXTDAY extends ScheduledTask {

    private static final long serialVersionUID = -4091296144762316950L;

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
     * Map to save trade maturity date - transfer id to filter autosettle
     */
    private final HashMap<Long, JDate> tradeMatDateByTransferId = new HashMap<>();
    /**
     * ST Parameter, true if execute action to move next status
     */
    public static final String AUTO_SETTLE = "Auto Settle";
    /**
     * ST Parameter, workflow bond action
     */
    public static final String WF_BOND_ACTION_AUTOSETTLE = "WF Bond Action Auto Settle";
    /**
     * ST Parameter, workflow repo action
     */
    public static final String WF_REPO_ACTION_AUTOSETTLE = "WF Repo Action Auto Settle";
    /**
     * ST Parameter, workflow repo action, if product family is none
     */
    public static final String WF_ALL_ACTION_AUTOSETTLE = "WF ALL Action Auto Settle";

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
            List<Trade> calypsoTradesFromFilter = ReconCCPUtil.loadAndFilterTrades(ds, this.getValuationDatetime(),
                    this._timeZone, this._tradeFilter, this._filterSet);

            calypsoTradesFromFilter = filterTradeBySettlementXfers(this.getDSConnection(), calypsoTradesFromFilter,
                    this.filterBySRI, this.filterBySettleDate,
                    this.sdf, JDate.valueOf(this.getValuationDatetime()));

            if (!Util.isEmpty(fileObjects) && !Util.isEmpty(calypsoTradesFromFilter)) {
                List<LCHTrade> trades = groupByTrade(groupNetPositionsByISIN(fileObjects), fileObjects);

                if (!Util.isEmpty(trades)) {
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
     *
     * This is conceptually incorrect - should not to match trades with obligations,
     * for netted transfers with multiple trade transfers this logic will generate a matching result for
     * each trade included into the netting and subsequently  update the same transfer many-many times and generate multiple warning tasks
     * needs to be redesigned 
     */
    private List<ReconCCPMatchingResult> matchTransfers(List<LCHTrade> fileTrades, List<Trade> calypsoTrades) {
        List<ReconCCPMatchingResult> matchingResults = new ArrayList<>();
        List<Long> processedTransfers = new ArrayList<>();
        JDate stValueDate = JDate.valueOf(this.getValuationDatetime());
        //This may be different between clearing
        //Creating a simple class hierarchy to easily add functionality in the future is not a bad idea
        for (LCHTrade trade : fileTrades) {
            boolean matchedTrade = false;
            for (int i = 0; i < calypsoTrades.size(); i++) {
                Trade calypsoTrade = calypsoTrades.get(i);
                //Execute simple match
                if (trade.getBuyerSellerReference().equalsIgnoreCase(calypsoTrade.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF))) {
                    matchedTrade = true;
                    calypsoTrades.remove(calypsoTrade);

                    TransferArray tfArray = getSettlementTransfers(this.getDSConnection(), calypsoTrade.getLongId());

                    if (!Util.isEmpty(tfArray)) {
                        for (BOTransfer bo : tfArray.getTransfers()) {
                            saveTradeMaturityDate(bo, calypsoTrade);
                            if (filterXfer(this.filterBySRI, this.filterBySettleDate, this.sdf, bo, calypsoTrade, stValueDate)
                                    && !processedTransfers.contains(bo.getLongId())) {

                                List<LCHObligations> obligations = new ArrayList<>();
                                LCHNetPositions netPositions = trade.getNetPositionObligationSet();
                                if (netPositions != null) {
                                    obligations = netPositions.getObligations();
                                }

                                processedTransfers.add(bo.getLongId());

                                if (!Util.isEmpty(obligations)) {
                                    String tradeSourceName = bo.getAttribute(XFER_ATTR_TRADE_SOURCE);
                                    if (Util.isEmpty(tradeSourceName)) {
                                        bo.setAttribute(XFER_ATTR_TRADE_SOURCE, trade.getTradeSourceName());
                                        saveTransferAttribute(this.getDSConnection(), bo.getLongId(), XFER_ATTR_TRADE_SOURCE, trade.getTradeSourceName());
                                    }

                                    ReconCCPMatchingResult netPositionsResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
                                    if (netPositions != null && netPositions.matchReference(bo)) {
                                        netPositionsResult = netPositions.matchFields(bo);
                                        if (!netPositionsResult.hasErrors()) {
                                            if (netPositionsResult.hasWarnings()) {
                                                netPositionsResult.setTrade(calypsoTrade);
                                                netPositionsResult.setTransfer(bo);
                                                netPositionsResult.setCashAmount("LCH".equals(netPositions.getCashReceiver())?-netPositions.getCashAmount():netPositions.getCashAmount());
                                                matchingResults.add(netPositionsResult);
                                            }
                                            if (obligations.size() <= 1) {
                                                LCHObligations obligation = obligations.get(0);
                                                if (obligation.matchReference(bo)) {
                                                    ReconCCPMatchingResult matchingResult = obligation.match(bo);
                                                    matchingResult.setTrade(calypsoTrade);
                                                    matchingResult.setTransfer(bo);
                                                    matchingResult.setCashAmount("LCH".equals(obligation.getCashReceiver())?-obligation.getCashAmountInstructed():obligation.getCashAmountInstructed());
                                                    matchingResults.add(matchingResult);
                                                }
                                            } else if (!"IT".equalsIgnoreCase(trade.getIdentifier().getLchMarketCode()) && !trade.getTradeSourceName().contains("MTS") && obligations.size() > 1) {
                                                splitTransfers.addAll(splitTransfer(this.getDSConnection(), bo, obligations));
                                            } else {
                                                ReconCCPMatchingResult matchingResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
                                                matchingResult.setTrade(calypsoTrade);
                                                matchingResult.setTransfer(bo);
                                                matchingResult.addNoTransfersFound(bo);
                                                matchingResults.add(matchingResult);
                                            }
                                        } else {
                                            netPositionsResult.setTrade(calypsoTrade);
                                            netPositionsResult.setTransfer(bo);
                                            netPositionsResult.setCashAmount("LCH".equals(netPositions.getCashReceiver())?-netPositions.getCashAmount():netPositions.getCashAmount());
                                            matchingResults.add(netPositionsResult);
                                        }
                                    } else {
                                        netPositionsResult.addError("Net Position Nominal Amount NOT matched, Calypso Transfer ID  " + bo.getLongId() + ", Nominal Amount [" + bo.getNominalAmount() + "]");
                                        netPositionsResult.setTrade(calypsoTrade);
                                        netPositionsResult.setTransfer(bo);
                                        matchingResults.add(netPositionsResult);
                                    }
                                }
                            }
                        }
                    }
                    break;
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

            if (transfer != null && !transferArray.contains(transfer)) {
                updateMTSSettlementRefInstructed2(transfer, trade);
            }

            if (result.isMatched()) {
                if (result.hasWarnings()) {
                    Task matchingWarnTask;
                    if (trade != null) {
                        matchingWarnTask = new Task(trade);
                        matchingWarnTask.setTradeLongId(trade.getLongId());
                    } else {
                        matchingWarnTask = new Task();
                    }
                    matchingWarnTask.setComment(result.getUnmatchedWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(jdt);
                    matchingWarnTask.setNewDatetime(jdt);
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingWarnTask);
                }
                if (result.hasErrors()) {
                    Task matchingErrorsTask;
                    if (trade != null) {
                        matchingErrorsTask = new Task(trade);
                        matchingErrorsTask.setTradeLongId(trade.getLongId());
                    } else {
                        matchingErrorsTask = new Task();
                    }
                    matchingErrorsTask.setComment(result.getUnmatchedErrors());
                    matchingErrorsTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING);
                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setDatetime(jdt);
                    matchingErrorsTask.setNewDatetime(jdt);
                    matchingErrorsTask.setPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingErrorsTask);
                    if (transfer != null) {
                        transfer.setAttribute(TRADE_KEYWORD_RECON, RECON_KO);
                    }
                } else if (transfer != null) {
                    transfer.setAttribute(TRADE_KEYWORD_RECON, RECON_OK);
                }

                if (transfer != null) {
                    double ourCashAmount = "SECURITY".equals(transfer.getTransferType())
                            ?"PAY".equals(transfer.getPayReceive())?Math.abs(transfer.getRealCashAmount()):-Math.abs(transfer.getRealCashAmount())
                            :"PAY".equals(transfer.getPayReceive())?-Math.abs(transfer.getRealSettlementAmount()):Math.abs(transfer.getRealSettlementAmount());

                    if (Math.abs(ourCashAmount - result.getCashAmount()) > Math.pow(10 , -CurrencyUtil.getCcyDecimals(transfer.getSettlementCurrency(), 2))) {
                        transfer.setAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED, Util.numberToString( result.getCashAmount(), Locale.UK, false));
                    }

                 //   transfer.setAction(Action.valueOf("UPDATE_XFER_ATTR"));
                    transferArray.add(transfer);
                }
            } else {
                if (result.hasWarnings()) {
                    Task matchingWarnTask;
                    if (trade != null) {
                        matchingWarnTask = new Task(trade);
                        matchingWarnTask.setTradeLongId(trade.getLongId());
                    } else {
                        matchingWarnTask = new Task();
                    }
                    matchingWarnTask.setComment(result.getUnmatchedWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_NETTING);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(jdt);
                    matchingWarnTask.setNewDatetime(jdt);
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingWarnTask);
                }
                if (result.hasErrors()) {
                    Task matchingErrorsTask;
                    if (trade != null) {
                        matchingErrorsTask = new Task(trade);
                    } else {
                        matchingErrorsTask = new Task();
                    }
                    matchingErrorsTask.setComment(result.getUnmatchedErrors());
                    if (trade == null && transfer == null) {
                        matchingErrorsTask.setEventType(EXCEPTION_MISSING_TRADE_RECON_CCP);
                    } else if (transfer != null) {
                        if (result.getUnmatchedErrors().contains("Coupon Identifier")) {
                            matchingErrorsTask.setEventType(EXCEPTION_COUPON_IDENTIFIER_Y);
                        } else {
                            matchingErrorsTask.setEventType(EXCEPTION_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP);
                        }
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
                if (!Util.isEmpty(recon)) {
                    saveTransferAttribute(this.getDSConnection(), transfer.getLongId(), TRADE_KEYWORD_RECON, recon);
                    if (RECON_OK.equalsIgnoreCase(recon)) {

                        String settlementReferenceInstructed = transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
                        if (!Util.isEmpty(settlementReferenceInstructed)) {
                            saveTransferAttribute(this.getDSConnection(), transfer.getLongId(), XFER_ATTR_SETTLEMENT_REF_INST,
                                    settlementReferenceInstructed);
                        }
                        String settlementReferenceInstructed2 = transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST_2);
                        if (!Util.isEmpty(settlementReferenceInstructed2)) {
                            saveTransferAttribute(this.getDSConnection(), transfer.getLongId(), XFER_ATTR_SETTLEMENT_REF_INST_2,
                                    settlementReferenceInstructed2);
                        }

                        String cashAmountInstructed = transfer.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED);
                        if (!Util.isEmpty(cashAmountInstructed)) {
                            saveTransferAttribute(this.getDSConnection(), transfer.getLongId(), XFER_ATTR_CASH_AMOUNT_INSTRUCTED,
                                    cashAmountInstructed);
                        }
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, e.getCause());
        }

        moveNextStatus(transferArray);
    }

    private void updateMTSSettlementRefInstructed2(BOTransfer transfer, Trade trade) {
        if (transfer != null && trade != null) {
            String originalSettRef = transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
            if (isBOTransferMTS(this.getDSConnection(), transfer) && transfer.getSettleDate() != null) {
                JDate priorDate = transfer.getSettleDate().addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMdd");
                String monthDayMinusOne = dateFormat.format(priorDate.getDate());
                String isin;
                if (trade.getProduct() instanceof Repo) {
                    isin = ((Repo) trade.getProduct()).getSecurity().getSecCode("ISIN");
                } else {
                    isin = trade.getProduct().getSecCode("ISIN");
                }
                transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, monthDayMinusOne + isin);
                transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST_2, originalSettRef);
            }
        }
    }

    /**
     * Move the transfer to the next status
     *
     * @param transferArray the transfers array
     */
    private void moveNextStatus(List<BOTransfer> transferArray) {
        //Move transfer nextstatus
        if (!Util.isEmpty(getAttribute(MOVE_NEXT_STATUS)) && getBooleanAttribute(MOVE_NEXT_STATUS)) {
            //Add split transfers to move next status
            transferArray.addAll(splitTransfers);
            List<BOTransfer> filterTransfers = filterTransfersByAttribute(this.getDSConnection(),
                    transferArray, TRADE_KEYWORD_RECON, RECON_OK);
            //Filter PENDING transfers, remove SPLIT parent
            filterTransfers = filterTransfersByStatus(this.getDSConnection(), filterTransfers, Status.S_PENDING);
            moveTransfersNextStatus(this.getDSConnection(), filterTransfers,
                    getAttribute(WF_BOND_ACTION), getAttribute(WF_REPO_ACTION), getAttribute(WF_ALL_ACTION));
            if (!Util.isEmpty(getAttribute(AUTO_SETTLE)) && getBooleanAttribute(AUTO_SETTLE)) {
                TransferArray lastTransferVersion = getLastTransfersVersion(this.getDSConnection(), filterTransfers);
                if (lastTransferVersion != null) {
                    List<BOTransfer> transfersToSettled = filterTransfersToSettled(lastTransferVersion.toVector());
                    moveTransfersNextStatus(this.getDSConnection(), transfersToSettled, getAttribute(WF_BOND_ACTION_AUTOSETTLE),
                            getAttribute(WF_REPO_ACTION_AUTOSETTLE), getAttribute(WF_ALL_ACTION_AUTOSETTLE));
                }

            }
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
        attributeList.add(attribute(AUTO_SETTLE));
        attributeList.add(attribute(WF_BOND_ACTION_AUTOSETTLE));
        attributeList.add(attribute(WF_REPO_ACTION_AUTOSETTLE));
        attributeList.add(attribute(WF_ALL_ACTION_AUTOSETTLE));
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

        if (Util.isEmpty(getAttribute(ORDER_FILES_BY))) {
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
        if (Util.isEmpty(getAttribute(AUTO_SETTLE))) {
            messages.add(AUTO_SETTLE + " cannot be empty");
        } else {
            boolean autoSettle = getBooleanAttribute(AUTO_SETTLE);
            if (autoSettle) {
                if (Util.isEmpty(getAttribute(MOVE_NEXT_STATUS))) {
                    messages.add(MOVE_NEXT_STATUS + " cannot be empty");
                } else if (!getBooleanAttribute(MOVE_NEXT_STATUS)) {
                    messages.add(MOVE_NEXT_STATUS + " cannot be false");
                }
                if (Util.isEmpty(getAttribute(WF_BOND_ACTION_AUTOSETTLE))) {
                    messages.add(WF_BOND_ACTION_AUTOSETTLE + " cannot be empty");
                }
                if (Util.isEmpty(getAttribute(WF_REPO_ACTION_AUTOSETTLE))) {
                    messages.add(WF_REPO_ACTION_AUTOSETTLE + " cannot be empty");
                }
                if (Util.isEmpty(getAttribute(WF_ALL_ACTION_AUTOSETTLE))) {
                    messages.add(WF_ALL_ACTION_AUTOSETTLE + " cannot be empty");
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
            } else if (WF_REPO_ACTION.equals(attr) || WF_BOND_ACTION.equals(attr) || WF_ALL_ACTION.equals(attr) ||
                    WF_REPO_ACTION_AUTOSETTLE.equals(attr) || WF_BOND_ACTION_AUTOSETTLE.equals(attr) ||
                    WF_ALL_ACTION_AUTOSETTLE.equals(attr)) {
                return LocalCache.getDomainValues(this.getDSConnection(), "transferAction");
            } else if (AUTO_SETTLE.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            } else if (ORDER_FILES_BY.equals(attr)) {
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

    /**
     * Filter to check if the transfer has to go to SETTLED
     * -	Agent = MGBE
     * -	Xfer Security Amount = 0
     * -    Trade maturity date = Security maturity date.
     *
     * @param transfersToFilter the list of transfers
     * @return the filter list
     */
    private List<BOTransfer> filterTransfersToSettled(List<BOTransfer> transfersToFilter) {
        List<BOTransfer> transfersAccept = new ArrayList<>();
        if (!Util.isEmpty(transfersToFilter)) {
            for (BOTransfer transfer : transfersToFilter) {
                if (transfer.getStatus().equals(Status.VERIFIED)) {
                    if (acceptMaturityDate(transfer)) {
                        transfersAccept.add(transfer);
                    } else if (acceptEuroclear(transfer)) {
                        transfersAccept.add(transfer);
                    }
                }
            }
        }
        return transfersAccept;
    }

    /**
     * Check if transfer is EUROCLEAR
     *
     * @param transfer the transfer
     * @return true if is EUROCLEAR
     */
    private boolean acceptEuroclear(BOTransfer transfer) {
        if (transfer != null && !Util.isEmpty(transfer.getTransferType()) && transfer.getTransferType().equals("SECURITY")) {
            LegalEntity entity = BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getInternalAgentId());
            return entity != null && !Util.isEmpty(entity.getCode()) && "MGBE".equals(entity.getCode()) &&
                    transfer.getSettlementAmount() == 0.0D; //SettlementAmount == Xfer Security Amount, PO Agent
        }
        return false;
    }

    /**
     * Check if Trade maturity date = Security maturity date.
     *
     * @param transfer the transfer
     * @return true if Trade maturity date = Security maturity date.
     */
    private boolean acceptMaturityDate(BOTransfer transfer) {
        if (transfer != null && !Util.isEmpty(transfer.getTransferType()) && transfer.getTransferType().equals("SECURITY")) {
            JDate tradeMaturityDate = tradeMatDateByTransferId.get(transfer.getLongId());
            if (tradeMaturityDate != null) {
                Product product = BOCache.getExchangedTradedProduct(this.getDSConnection(), transfer.getProductId());
                if (product == null) {
                    return false;
                }
                return product.getMaturityDate() != null &&
                        tradeMaturityDate.equals(product.getMaturityDate());
            }
        }
        return false;
    }

    /**
     * Save the trade maturity date
     * Bond: SettleDate
     * Repo: EndDate
     *
     * @param transfer the transfer
     * @param trade    the trade
     */
    private void saveTradeMaturityDate(BOTransfer transfer, Trade trade) {
        if (transfer != null && trade != null && trade.getProduct() != null) {
            if (trade.getProduct() instanceof SecFinance) {
                tradeMatDateByTransferId.put(transfer.getLongId(), ((SecFinance) trade.getProduct()).getEndDate());
            } else {
                tradeMatDateByTransferId.put(transfer.getLongId(), trade.getSettleDate());
            }
        }
    }
}
