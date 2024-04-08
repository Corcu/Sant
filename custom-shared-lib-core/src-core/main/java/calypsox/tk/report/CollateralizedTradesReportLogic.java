/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.PortfolioReconciliationReport.CollateralizedTradesDataWrapper;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;
import calypsox.util.collateral.SantCollateralConfigUtil;
import calypsox.util.SantReportingUtil;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.DFA_OUTPUT_FIELDS;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.RESPONSES;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationLogic;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteTrade;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static calypsox.tk.report.CollateralizedTradesReport.NO_VALUEDATE_ERROR_MESSAGE;
import static calypsox.util.TradeInterfaceUtils.*;

/**
 * Logic for the DFA batch file (collatearlized trades report) and the Portfolio reconciliation batch file (DFA and
 * EMIR). This class has the logic for both report as there are common calls between both batch files generations.
 *
 * @author Guillermo Solano
 * @version 2.6
 * <p>
 * 23/05/2013. Added new threshold measure. ------------------------------------------------------------
 * 18/06/2013. Cache the responses for contract. Improves the performance.
 */
public class CollateralizedTradesReportLogic {

    // public constant
    /* determines the minimum Threshold to consider in a MCContract for the FULL_PARTIAL_COLLATERALIZED answer */
    // private final static Double MINIMUM_THRESHOLD_TO_CONSIDER = 0.0;
    private final static String EMPTY = "";
    private final static String BUY = "Buy";
    private final static String SELL = "Sell";
    private final static String LOAN = "Loan";
    private final static String BORROWER = "Borrower";

    // class constants, most keywords read from calypsox.tk.report.CollateralizedTradesReport.
    private final static String BO_REFERENCE_KEYWORD = TRD_IMP_FIELD_BO_REFERENCE;
    private final static String BO_SYSTEM_KEYWORD = TRD_IMP_FIELD_BO_SYSTEM;
    private final static String STRUCTURE_ID = TRADE_KWD_STRUCTURE_ID;
    private final static String UNDERLYING = COL_CTX_PROP_UNDERLYING;
    private final static String UNDERLYING_1 = COL_CTX_PROP_UNDERLYING_1;
    private final static String NOMINAL_1 = COL_CTX_PROP_NOMINAL_1;
    private final static String NOMINAL_2 = COL_CTX_PROP_NOMINAL_2;
    private final static String CCY_1 = COL_CTX_PROP_CCY_1;
    private final static String CCY_2 = COL_CTX_PROP_CCY_2;
    private final static String DIRECTION_1 = COL_CTX_PROP_DIRECTION_1;
    private final static String DIRECTION_2 = COL_CTX_PROP_DIRECTION_2;
    private final static String USI_REFERENCE = TRADE_KWD_USI_REFERENCE;
    private final static String SD_MSP = TRADE_KWD_SD_MSP;
    private final static String US_PARTY = TRADE_KWD_US_PARTY;
    private final static String DFA_APPLICABLE = TRADE_KWD_DFA;
    private final static String FC_NFC = TRADE_KWD_FC_NFC;
    private final static String EMIR_APPLICABLE = TRADE_KWD_EMIR;
    private final static String UTI_APPLICABLE = TRADE_KWD_UTI;

    /* to format JDatetime when required */
    protected final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // definition of two legs products in Calypso
    private static String[] twoLegs = {"CASH_FLOW_MATCHING", "INTEREST_RATE_SWAP", "FX_SWAP_NON_DELIVERABLE",
            "FX_SWAP_DELIVERABLE", "FX_NON_DELIVERABLE_FORWARD", "FX_DELIVERABLE_SPOT", "FX_DELIVERABLE_FORWARD",
            "EQUITY_SWAP", "CURRENCY_SWAP", "BASIS_SWAP"};

    private static List<String> twoLegsUnderLyings = Arrays.asList(twoLegs);

    // just to match each response with an optional message when required
    public static class ResponseWrapper {

        public RESPONSES type;
        public String message;

        private ResponseWrapper(RESPONSES t, String m) { // only write mode from this class
            this.type = t;
            this.message = m;
        }

        @Override
        public String toString() {
            return this.type.getDescription() + this.message + ". Response code " + this.type.getResponseValue();
        }
    }

    // caches previous responses for a specific contract
    private static Map<Integer, List<ResponseWrapper>> cacheContractsResponse = new HashMap<Integer, List<ResponseWrapper>>();

    // //////////////////////////////////////////////////////////
    // // USE FOR DFA & PORTFOLIO RECONCILIATION BATCH FILES ///
    // ////////////////////////////////////////////////////////

    /**
     * Retrives all the MarginCallDetailEntryDTO of today (therefore, all Collaterals trade extracted from this method
     * are collateralized). This is used in the Batch mode process
     *
     * @param valuationDate
     * @return a list with all the MCDEntries that belong to the current system day when the call is done
     */
    public static List<MarginCallDetailEntryDTO> retriveMCAliveTradesList(final List<String> errors, JDate processDate) {

        List<MarginCallDetailEntryDTO> finalMCDEList = null;

        Log.debug("CollateralizedTradesReportLogic", "1. retriveTodayMCTradesList -> Start call");

        if (processDate == null) {

            Log.error(CollateralizedTradesReportLogic.class, NO_VALUEDATE_ERROR_MESSAGE);

        }

        // retrieve only derivative products as required for DFA
        String where = "PRODUCT_DESC.PRODUCT_SUB_TYPE NOT IN ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
                + "AND PRODUCT_DESC.PRODUCT_ID =  MARGIN_CALL_DETAIL_ENTRIES.PRODUCT_ID "
                + "AND (MARGIN_CALL_DETAIL_ENTRIES.PRODUCT_TYPE='PerformanceSwap' "
                + "OR  MARGIN_CALL_DETAIL_ENTRIES.PRODUCT_TYPE='CollateralExposure')"
                + "AND TRUNC(MARGIN_CALL_DETAIL_ENTRIES.PROCESS_DATETIME) = " + Util.date2SQLString(processDate);

        // get margin call detail entries for trades
        try {
            finalMCDEList = ServiceRegistry.getDefault().getDashBoardServer()
                    .loadMarginCallDetailEntries(where, Arrays.asList("MARGIN_CALL_DETAIL_ENTRIES", "PRODUCT_DESC"));

        } catch (RemoteException e) {
            errors.add("CollateralizedTradesReportLogic NOT POSSIBLE to generate the List of MarginCallDetailEntryDTO");
            Log.error("CollateralizedTradesReportLogic", e);
            // e.printStackTrace();
        }

        Log.debug("CollateralizedTradesReportLogic", "2. retriveTodayMCTradesList -> Return list MCDE. End");

        return finalMCDEList;
    }

    // //////////////////////////////
    // // USE FOR DFA BATCH FILE ///
    // ////////////////////////////

    /**
     * @param mcdet          the MarginCallDetailEntryDTO from where the response is has to be constructed
     * @param responses
     * @param responses      a List with the generated responses to be sent to the Source Systems (if applied)
     * @param reportTemplate
     * @return a map with all the output fields necessary to build a response
     */
    public static Map<DFA_OUTPUT_FIELDS, String> buildOutputMapFromMCDetailEntryDTO(MarginCallDetailEntryDTO mcdet,
                                                                                    List<ResponseWrapper> responses, JDate processDate, ReportTemplate reportTemplate) {

        RemoteTrade remoteTrade = null;
        Trade trade = null;

        if (responses == null) {
            responses = new ArrayList<ResponseWrapper>();
        }

        if (mcdet == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "No MarginCallDetailEntryDTO received as parameter in buildOutputMapFromMCDetailEntryDTO method. ");
            return null;
        }

        CollateralConfig marginCallConfig = null;
        List<MarginCallDetailEntryDTO> l = new ArrayList<MarginCallDetailEntryDTO>(1);
        l.add(mcdet);

        Map<Integer, CollateralConfig> loadMarginCallContracts = loadMarginCallContracts(l);

        if (loadMarginCallContracts.isEmpty()) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "No Contract Found for the MCDEntry parameter in buildOutputMapFromMCDetailEntryDTO method. ");
            return null;
        }

        marginCallConfig = loadMarginCallContracts.get(mcdet.getMarginCallConfigId());

        // GSM 15/07/15. SBNA Multi-PO filter
        if (CollateralUtilities.filterPoByTemplate(reportTemplate, marginCallConfig)) {
            return null;
        }

        remoteTrade = DSConnection.getDefault().getRemoteTrade();

        try {
            trade = remoteTrade.getTrade(mcdet.getTradeId());
        } catch (RemoteException e) {
            Log.error(CollateralizedTradesReportLogic.class.getName(), "Cannot load trade from RemoteTrade " + e);
            return null;
        }

        // generation of the response
        // GSM: 18/06/2013. Added cache to improve performance
        generateCollaterizedDegreeResponse(marginCallConfig, trade, responses, true, processDate);

        return buildOutputMapFromTradeAndContract(trade, mcdet, marginCallConfig, responses);

    }

    /**
     * @param trade            trade to be checked if it matches the MC contract as parameter
     * @param mcdet            Margin Call detail entry
     * @param marginCallConfig is the suitable MC contract obtained previous
     * @param responses        a List with the generated responses to be sent to the Source Systems (if applied)
     * @return a map with all the output fields necessary to build a response
     */
    public static Map<DFA_OUTPUT_FIELDS, String> buildOutputMapFromTradeAndContract(Trade trade,
                                                                                    MarginCallDetailEntryDTO mcdet, CollateralConfig marginCallConfig, List<ResponseWrapper> responses) {

        Map<DFA_OUTPUT_FIELDS, String> outMap = new HashMap<DFA_OUTPUT_FIELDS, String>(
                DFA_OUTPUT_FIELDS.values().length);

        if (!checkTradeAndMCContractNotNull(trade, marginCallConfig, mcdet)) {
            return null;
        }
        if ((responses == null) || responses.isEmpty()) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "No responses received as parameter in buildOutputMapFromTradeAndContract method. ");
            return null;
        }

        outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, responses.get(0).type.getResponseValue().toString());
        outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_NAME, marginCallConfig.getName());
        outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_TYPE, marginCallConfig.getContractType());
        outMap.put(DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE, trade.getKeywordValue(BO_REFERENCE_KEYWORD));
        outMap.put(DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM, trade.getKeywordValue(BO_SYSTEM_KEYWORD));
        outMap.put(DFA_OUTPUT_FIELDS.VALUE_DATE, dateFormat.format(trade.getTradeDate()));
        outMap.put(DFA_OUTPUT_FIELDS.CONTRACT_DIRECTION, SantCollateralConfigUtil.getContractDirectionV14Value(marginCallConfig));

        if (mcdet.getProcessDatetime() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_PROCESSING_DATE,
                    dateFormat.format(mcdet.getProcessDatetime().getTime()));
        }
        if (mcdet.getValueDatetime() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_VALUATION_DATE,
                    dateFormat.format(mcdet.getValueDatetime().getTime()));
        }
        outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_BASE_CURRENCY, marginCallConfig.getCurrency());
        if (marginCallConfig.getProcessingOrg() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_OWNER, marginCallConfig.getProcessingOrg().getName());
        }
        if (trade.getProduct() != null && trade.getProduct() instanceof CollateralExposure) {
            outMap.put(DFA_OUTPUT_FIELDS.PRODUCT_TYPE, trade.getProduct().getSubType());

        } else if (trade.getProduct() != null && trade.getProduct() instanceof PerformanceSwap) {
            outMap.put(DFA_OUTPUT_FIELDS.PRODUCT_TYPE, trade.getProduct().getType());
        }
        if (marginCallConfig.getStartingDate() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_START_DATE,
                    dateFormat.format(marginCallConfig.getStartingDate().getTime()));
        }
        if (marginCallConfig.getClosingDate() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_END_DATE,
                    dateFormat.format(marginCallConfig.getClosingDate().getTime()));
        }

        return outMap;
    }

    // ///////////////////////////////////////////////
    // // USE FOR PORT. RECONCILIATION BATCH FILE ///
    // /////////////////////////////////////////////

    /**
     * This class fills the missing required data for each mcde with the purpose of reading the contract and trade.
     *
     * @param mcdeList a list of the MarginCallDetailEntriesDTO as read from database.
     * @return map with the set {Key=trade id, item = SantTradeBrowserItem}, where for each SantTradeBrowserItem. Each
     * SantTradeBrowserItem includes de mcde, the contract and the trade.
     */
    public HashMap<Long, SantTradeBrowserItem> buildListOfTradeContractAndMCDetailEntryDTOFromList(
            List<MarginCallDetailEntryDTO> mcdeList) {

        Map<Integer, CollateralConfig> marginCallContractsMap = loadMarginCallContracts(mcdeList);
        HashMap<Long, SantTradeBrowserItem> ctwrapperMap = new HashMap<>(
                mcdeList.size());
        RemoteTrade remoteTrade = DSConnection.getDefault().getRemoteTrade();

        for (MarginCallDetailEntryDTO mcde : mcdeList) {

            final CollateralConfig contract = marginCallContractsMap.get(mcde.getMarginCallConfigId());
            Trade trade = null;

            try {
                trade = remoteTrade.getTrade(mcde.getTradeId());
            } catch (RemoteException excp) {
                Log.error(CollateralizedTradesReportLogic.class.getName(), "Cannot load trade from RemoteTrade " + excp);
                return null;
            }

            if ((contract == null) || (trade == null)) {
                Log.error(CollateralizedTradesReportLogic.class.getName(),
                        "Building item for Port.Reconciliation: either contract or trade is null. This should NOT happen");
            }

            SantMarginCallDetailEntry data = new SantMarginCallDetailEntry(mcde, null, contract);
            data.setTrade(trade);
            SantTradeBrowserItem item = new SantTradeBrowserItem(data);
            if (trade != null) {
                ctwrapperMap.put(trade.getLongId(), item);
            }
        }
        return ctwrapperMap;
    }

    /**
     * For the Portfolio reconciliation: this methods allows to gather the data which is independent of the two threads
     * that might be running (PLMark and FXQuotes threads). Calling first this method allows to continue the process
     * before stopping the main thread, giving time to the other process to gather the requested data.
     *
     * @param entry                         , including mcde, trade and thec contract
     * @param portfolioReconciliationReport instance of the report
     * @return a column map format including the thread independent data.
     */
    public Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> formatRowThreadIndependent(SantTradeBrowserItem entry,
                                                                                          PortfolioReconciliationReport portfolioReconciliationReport) {

        Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> outMap = null;
        String temp = EMPTY;
        CollateralExposure product = null;
        SantMarginCallConfigReportStyleHelper mccReportStyleHelper = null;

        if (!checkTradeAndMCContractNotNull(entry.getTrade(), entry.getMarginCall(), entry.getMarginCallDetailEntry())) {
            return null;
        }

        if ((entry.getTrade().getProduct() == null) || !(entry.getTrade().getProduct() instanceof CollateralExposure)
                || !(entry.getTrade().getProduct() instanceof PerformanceSwap)) {

            Log.error(CollateralizedTradesReportLogic.class,
                    "formatRow: the product of the trade is NOT CollateralExposure");
            return null;
        }

        product = (CollateralExposure) entry.getTrade().getProduct();
        mccReportStyleHelper = new SantMarginCallConfigReportStyleHelper();

        outMap = new HashMap<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String>(
                PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.values().length);

        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_AGREEMENT, entry.getMarginCall().getName()); // 1
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COLLATERAL_TYPE, entry.getMarginCall().getContractType());// 2
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BASE_CCY, entry.getMarginCall().getCurrency()); // 25

        temp = entry.getTrade().getCounterParty() != null ? entry.getTrade().getCounterParty().getName() : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.COUNTERPARTY, temp); // 3
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_ID,
                entry.getTrade().getKeywordValue(BO_REFERENCE_KEYWORD)); // 4
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EXTERNAL_REFERENCE, entry.getTrade().getExternalReference()); // 5
        // FO

        temp = (portfolioReconciliationReport != null) ? getPreviousWorkingDay(
                portfolioReconciliationReport.getValDate(), entry.getMarginCall()).toString() : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.CLOSE_OF_BUSINESS, temp); // 6 close of business

        temp = (entry.getTrade().getKeywordValue(STRUCTURE_ID) != null) ? entry.getTrade()
                .getKeywordValue(STRUCTURE_ID) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.STRUCTURE, temp); // 7

        temp = (entry.getTrade().getTradeDate() != null) ? dateFormat.format(entry.getTrade().getTradeDate()) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_DATE, temp); // 8 valuation

        temp = ((product != null) && (product.getStartDate() != null)) ? product.getStartDate().toString() : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_VALUE_DATE, temp); // 9

        temp = ((product != null) && (product.getMaturityDate() != null)) ? product.getMaturityDate().toString()
                : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.TRADE_MATURITY_DATE, temp); // 10
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.VALUATION_AGENT,
                mccReportStyleHelper.getValuationAgent(entry.getMarginCall())); // 11

        temp = (entry.getTrade().getBook() != null) ? entry.getTrade().getBook().getName() : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PORTFOLIO, temp); // 12
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.OWNER, entry.getMarginCall().getProcessingOrg().getName()); // 13

        temp = ((entry.getTrade().getBook() != null) && (entry.getTrade().getBook().getLegalEntity() != null)) ? entry
                .getTrade().getBook().getLegalEntity().getName() : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DEAL_OWNER, temp); // 14
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.INSTRUMENT, entry.getTrade().getProductSubType()); // 15

        if (is2Legs(product.getUnderlyingType())) {
            temp = (String) product.getAttribute(UNDERLYING_1);
        } else {
            temp = (String) product.getAttribute(UNDERLYING);
        }
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.UNDERLYING, temp); // 16

        buildLegsRowData(entry.getTrade(), outMap); // 17-20
        buildBuySellField(entry.getTrade(), outMap); // 24

        temp = (entry.getMarginCall() != null) ? entry.getMarginCall().getCurrency() : EMPTY;

        temp = (entry.getTrade().getKeywordValue(USI_REFERENCE) != null) ? entry.getTrade().getKeywordValue(
                USI_REFERENCE) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.USI, temp); // 27

        temp = (entry.getTrade().getKeywordValue(SD_MSP) != null) ? entry.getTrade().getKeywordValue(SD_MSP) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.SD_MSP, temp); // 28
        temp = (entry.getTrade().getKeywordValue(US_PARTY) != null) ? entry.getTrade().getKeywordValue(US_PARTY)
                : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.US_PERSON, temp); // 29
        temp = (entry.getTrade().getKeywordValue(DFA_APPLICABLE) != null) ? entry.getTrade().getKeywordValue(
                DFA_APPLICABLE) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.DFA, temp); // 30
        temp = (entry.getTrade().getKeywordValue(FC_NFC) != null) ? entry.getTrade().getKeywordValue(FC_NFC) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.FC_NFC_NFCPLUS, temp); // 31
        temp = (entry.getTrade().getKeywordValue(EMIR_APPLICABLE) != null) ? entry.getTrade().getKeywordValue(
                EMIR_APPLICABLE) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.EMIR, temp); // 32
        // GSM: 22/08/13. Added the field for Portf. Reconciliation
        temp = (entry.getTrade().getKeywordValue(UTI_APPLICABLE) != null) ? entry.getTrade().getKeywordValue(
                UTI_APPLICABLE) : EMPTY;
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.UTI_APPLICABLE, temp); // 33

        return outMap;
    }

    /**
     * For the Portfolio reconciliation: this method gathers the data which is DEPENDENT of the two threads that might
     * be running (PLMark and FXQuotes threads).
     *
     * @param rowWrapper                    including teh entry, including mcde, trade and thec contract AND the current column data
     * @param portfolioReconciliationReport instance of the report
     * @return a column map format including the thread dependent data.
     */
    public void formatRowThreadsDependancy(CollateralizedTradesDataWrapper rowWrapper,
                                           PortfolioReconciliationReport report) {

        String indAmount, npvBaseCcy;
        indAmount = npvBaseCcy = EMPTY;

        JDate processDate = report.getValDate();
        SantTradeBrowserItem item = rowWrapper.getItemData();
        Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> rowMapData = rowWrapper.getCurrentMapData();

        if ((item == null) || (rowMapData == null)) {
            return;
        }
        if ((item.getTrade() != null) && (item.getPLMark(processDate) != null)) {

            MtmWrapper mtmData = new MtmWrapper(item.getPLMark(report.getValDate()), item.getTrade());
            indAmount = mtmData.getIndepAmount().toString();
            npvBaseCcy = mtmData.getNpvBaseCcy();
        }

        rowMapData.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.IND_AMOUNT, indAmount); // 21
        rowMapData.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.MTM_BASE_CCY, npvBaseCcy); // 26

        buildFXRate(rowWrapper, report); // 22 y 23

    }

    /**
     * takes the buy/sell in a proper manner.
     *
     * @param trade  to check
     * @param outMap resulting map columns
     */
    private void buildBuySellField(Trade trade, Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> outMap) {

        String buySell = "";
        CollateralExposure product = null;

        if ((trade == null) || (trade.getProduct() == null)) {
            return;
        }

        product = (CollateralExposure) trade.getProduct();

        buySell = product.getDirection(trade);

        if ((buySell == null) || buySell.equals("")) {

            if (trade.getProduct().getPrincipal() < 0) {
                buySell = "Loan";
            } else {
                buySell = "Borrower";
            }
        }
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.BUY_SELL, buySell); // 24
    }

    // ///////////////////////////////////////////////////////////////
    // ///////PRIVATE METHODS PORT. RECONCILIATION //////////////////
    // /////////////////////////////////////////////////////////////

    /**
     * returns previous working day.
     */
    private static JDate getPreviousWorkingDay(JDate from, CollateralConfig marginCallConfig) {

        return TradeCollateralizationLogic.getPreviousWorkingDay(from, marginCallConfig);

    }

    /**
     * returns true if the product underlying is a two legs instrument
     */
    private static boolean is2Legs(final String underlying) {
        return twoLegsUnderLyings.contains(underlying);

    }

    /**
     * Builds the two legs of the trade, putting the data of the principal leg (nominal and ccy) on the left side and
     * the secondary data on the right.
     *
     * @param trade
     * @param outMap
     */
    private void buildLegsRowData(Trade trade, Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> outMap) {

        CollateralExposure collatExpo = null;
        String tradeDirection = "";
        String principal1, principal2, ccy1, ccy2;
        principal1 = principal2 = ccy1 = ccy2 = EMPTY;

        if ((trade == null) || (trade.getProduct() == null) || !(trade.getProduct() instanceof CollateralExposure)) {
            return;
        }

        collatExpo = (CollateralExposure) trade.getProduct();

        if ((collatExpo != null)) {
            tradeDirection = collatExpo.getDirection(trade);
        }

        if (!is2Legs(collatExpo.getUnderlyingType())) // one leg
        {
            outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL, collatExpo.getPrincipal() + "");
            outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_CCY, collatExpo.getCurrency());
            return;
        }

        // two legs
        String leg1Direction = (String) collatExpo.getAttribute(DIRECTION_1);
        String leg2Direction = (String) collatExpo.getAttribute(DIRECTION_2);

        // should not happen, but sometimes it does in DEV
        if ((leg1Direction != null) || (leg2Direction != null)) {

            // leg one with loan is principal
            if (BUY.equalsIgnoreCase(tradeDirection) || LOAN.equalsIgnoreCase(tradeDirection)) {

                if (LOAN.equalsIgnoreCase(leg1Direction) || BUY.equalsIgnoreCase(leg1Direction)) { // principal

                    principal1 = getValueAsString(collatExpo.getAttribute(NOMINAL_1)); // 18
                    ccy1 = getValueAsString(collatExpo.getAttribute(CCY_1)); // 17
                    principal2 = getValueAsString(collatExpo.getAttribute(NOMINAL_2)); // 20
                    ccy2 = getValueAsString(collatExpo.getAttribute(CCY_2));
                } else { // sell, leg1 is borrower

                    principal2 = getValueAsString(collatExpo.getAttribute(NOMINAL_1)); // 20
                    ccy2 = getValueAsString(collatExpo.getAttribute(CCY_1));
                    principal1 = getValueAsString(collatExpo.getAttribute(NOMINAL_2)); // 18
                    ccy1 = getValueAsString(collatExpo.getAttribute(CCY_2));
                }

            } else { // leg2 is principal

                if (BORROWER.equalsIgnoreCase(leg2Direction) || SELL.equalsIgnoreCase(leg2Direction)) { // principal

                    principal2 = getValueAsString(collatExpo.getAttribute(NOMINAL_1));
                    ccy2 = getValueAsString(collatExpo.getAttribute(CCY_1));
                    principal1 = getValueAsString(collatExpo.getAttribute(NOMINAL_2));
                    ccy1 = getValueAsString(collatExpo.getAttribute(CCY_2));

                } else { // sell, leg1 is borrower

                    principal1 = getValueAsString(collatExpo.getAttribute(NOMINAL_1)); // 18
                    ccy1 = getValueAsString(collatExpo.getAttribute(CCY_1)); // 17
                    principal2 = getValueAsString(collatExpo.getAttribute(NOMINAL_2)); // 20
                    ccy2 = getValueAsString(collatExpo.getAttribute(CCY_2));
                }
            }
        }

        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL, principal1);
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_CCY, ccy1);
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2, principal2);
        outMap.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2_CCY, ccy2);
    }

    /**
     * Based on the type of params, it will generated the appropiate String.
     *
     * @param in , object that can be a String or Amount (for others it will return "").
     * @return the corresponding String.
     */
    private String getValueAsString(Object in) {

        String display = "";

        if (in instanceof String) {
            display = (String) in;
        } else if (in instanceof Amount) {
            display = ((Amount) in).toString();
        } else {
            return display;
        }

        if ((display == null) || display.isEmpty()) {
            return EMPTY;
        }

        return display;

    }

    /**
     * Builds de MTM_BASE_CCY, PRINCIPAL_CCY & PRINCIPAL_2_CCY; making the correct conversion from the quotes fetched by
     * the thread in charge of feeding this data.
     */
    private void buildFXRate(CollateralizedTradesDataWrapper item, PortfolioReconciliationReport report) {

        Map<PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS, String> rowWrapper = item.getCurrentMapData();

        if (rowWrapper == null) {
            return;
        }

        String temp = rowWrapper.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.MTM_BASE_CCY);

        if ((temp == null) || temp.isEmpty()) {
            Trade d = item.getItemData().getTrade();

            if ((d == null) || (d.getProduct() == null)) {
                return;
            }

            temp = d.getProduct().getCurrency();
        }

        final String mccCcy = temp;
        final String principalCcy = rowWrapper.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_CCY);
        final String principal2Ccy = rowWrapper.get(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.PRINCIPAL_2_CCY);
        Double fxRate1 = 0.0;
        Double fxRate2 = 0.0;
        QuoteValue qv = null;

        if ((mccCcy == null) || (principalCcy == null) || (principal2Ccy == null)) {
            return;
        }

        if (mccCcy.equals(principalCcy)) {
            fxRate1 = 1.0d;
        } else {
            qv = report.getQuotesLoader().fetchFXQuoteValue(mccCcy, principalCcy);
            if (qv != null) {
                fxRate1 = qv.getClose();
            }
        }

        if (!Util.isEmpty(principal2Ccy)) {

            qv = null;
            if (mccCcy.equals(principal2Ccy)) {
                fxRate2 = 1.0d;
            } else {
                qv = report.getQuotesLoader().fetchFXQuoteValue(mccCcy, principal2Ccy);
                if (qv != null) {
                    fxRate2 = qv.getClose();
                }
            }
        }
        // fx rates obtained.
        rowWrapper.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE, fxRate1.toString()); // 22
        rowWrapper.put(PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS.RATE_2, fxRate2.toString()); // 23
    }

    // ///////////////////////////////////////////////////////////////
    // /////////////////// PRIVATE METHODS DFA //////////////////////
    // /////////////////////////////////////////////////////////////

    /**
     * @param contractIds a list of MarginCallDetailEntryDTO
     * @return a map collection of MCContracts with the MC ID as key
     */
    private static Map<Integer, CollateralConfig> loadMarginCallContracts(
            final List<MarginCallDetailEntryDTO> mcdentries) {

        final Set<Integer> contractIds = new HashSet<>();

        for (MarginCallDetailEntryDTO entry : mcdentries) {
            contractIds.add(entry.getMarginCallConfigId());
        }

        try {
            return SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigByIds(
                    new ArrayList<Integer>(contractIds));

        } catch (final PersistenceException e) {
            Log.error(CollateralizedTradesReportLogic.class.getName(), "Cannot load Contract with Id=" + contractIds.toString(), e);
        }
        return null;
    }

    /**
     * When a trade has matched a MCContract and is considered collateralized, this methods adds in the responses
     * parameter the appropiate category type.
     *
     * @param contract         of the Collaterized Trade
     * @param trade            that matches the marginCallConfig
     * @param responses
     * @param responses        to be generated
     * @param isCollateralized
     * @param processDate
     * @param mCCvalDatetime
     * @param mCCprocessDate
     * @see CollateralizedTradesReportLogic.RESPONSES enum for more information of the type of responses
     */
    private static void generateCollaterizedDegreeResponse(CollateralConfig contract, Trade trade,
                                                           List<ResponseWrapper> responses, boolean isCollateralized, JDate processDate) {

        if (!checkTradeAndMCContractNotNull(trade, contract, new MarginCallDetailEntryDTO())) {
            return;
        }

        // GSM: 18/06/2013. Cache to improve response
        if (cacheContractsResponse.containsKey(contract.getId())) {
            responses.addAll(cacheContractsResponse.get(contract.getId()));
            return;
        }

        if (responses == null) {
            responses = new ArrayList<ResponseWrapper>(1);
        }

        // Trade is not collateralized as it does NOT match the contract. We generate the response
        if (!isCollateralized) {
            responses.add(new ResponseWrapper(RESPONSES.UNCOLLATERALIZED, " Trade BO: "
                    + trade.getKeywordValue(BO_REFERENCE_KEYWORD))); // response
        }
        if (contract.getContractDirection().equals(CollateralConfig.NET_UNILATERAL)) {
            responses.add(new ResponseWrapper(RESPONSES.ONE_WAY_COLLATERALIZED, " Trade BO: "
                    + trade.getKeywordValue(BO_REFERENCE_KEYWORD)));

        } else { // must be BILATERAL

            if (checkBilateralHasThreshold(contract, processDate)) {
                responses.add(new ResponseWrapper(RESPONSES.PARTIAL_COLLATERALIZED, " Trade BO: "
                        + trade.getKeywordValue(BO_REFERENCE_KEYWORD)));
            } else {
                responses.add(new ResponseWrapper(RESPONSES.FULL_COLLATERALIZED, " Trade BO: "
                        + trade.getKeywordValue(BO_REFERENCE_KEYWORD)));
            }
        }

        // GSM: 18/06/2013. Cache to improve response
        cacheContractsResponse.put(contract.getId(), responses);
    }

    /**
     * Implementation of the threshold logic implemented by GBO COL when the value 1 is returned.
     */
    public static boolean checkBilateralHasThreshold(CollateralConfig contract, JDate processDate) {

        final String poThresholdType = contract.getPoNewThresholdType();
        final String leThresholdType = contract.getLeNewThresholdType();

        // never means maximum threshold, so we return true
        if (poThresholdType.contains(CollateralConfig.NEVER) || leThresholdType.contains(CollateralConfig.NEVER)) {
            return true;
        }

        // check now credit rating:
        // if we have both option, we must check credit and after amount/percentage
        if (leHasThreshold(poThresholdType) || leHasThreshold(leThresholdType)) {

            final boolean hasRatingThreshold = checkRatingThresholdAndDifferentZero(contract, processDate);

            if (hasRatingThreshold) {
                return true;
                // otherwise, continue checking!
            }
        }

        // finally we check if amount or percentage is, at least one, different than zero
        final double poPer = contract.getPoNewThresholdPercentage();
        final double lePer = contract.getLeNewThresholdPercentage();
        final double poAmo = contract.getPoNewThresholdAmount();
        final double leAmo = contract.getLeNewThresholdAmount();

        return ((poPer != 0) || (lePer != 0) || (poAmo != 0) || (leAmo != 0));
    }

    /**
     * @param leThresholdType
     * @return if threshold type if CREDIT_RATING or GLOBAL_RATING
     */
    private static boolean leHasThreshold(String leThresholdType) {

        if ((leThresholdType == null) || leThresholdType.isEmpty()) {
            return false;
        }
        //MIG V16 CONST
        //return (leThresholdType.contains(CollateralConfig.CREDIT_RATING)
        return (leThresholdType.contains("CREDIT RATING")
                || leThresholdType.contains(CollateralConfig.GLOBAL_RATING) || leThresholdType
                .contains(CollateralConfig.BOTH));
    }

    /*
     * returns true if the rating of the Colateral contract is different than zero in the matrix
     */
    private static boolean checkRatingThresholdAndDifferentZero(final CollateralConfig contract, JDate processDate) {

        final String poRating = contract.getPoThresholdRatingDirection();// contract.getPoMTARatingDirection();
        final String leRating = contract.getLeThresholdRatingDirection();
        boolean rating = false;

        final Set<String> ratingOptions = new TreeSet<>(Arrays.asList(new String[]{CollateralConfig.HIGHER,
                CollateralConfig.LOWER}));

        if (poRating != null) {
            rating = ratingOptions.contains(poRating);
        }
        if (!rating && (leRating != null)) {
            rating = ratingOptions.contains(leRating);
        }
        // we have the ratting option, but the contract has not a stored direction. This Should not happen
        if (!rating) {
            return rating;
        }

        // now we access the rating matrix and check it's different from zero
        rating = checkRatingMatrixMCCDifferentZero(contract, processDate);

        return rating;
    }

    /*
     * access the rating matrix and check it's different from zero
     */
    private static boolean checkRatingMatrixMCCDifferentZero(final CollateralConfig contract, JDate processDate) {

        final int idRatingMatrixPo = contract.getPoRatingsConfigId();
        final int idRatingMatrixLe = contract.getLeRatingsConfigId();
        final String ratingDirectionPo = contract.getPoThresholdRatingDirection();
        final String ratingDirectionLe = contract.getLeThresholdRatingDirection();
        final String poThresholdType = contract.getPoNewThresholdType();
        final String leThresholdType = contract.getLeNewThresholdType();
        final int ctpyId = contract.getLegalEntity().getEntityId();
        final int poId = contract.getPoId();
        String amountPo = null;
        String amountLe = null;

        // take the amount for the PO
        if (leHasThreshold(poThresholdType) || poThresholdType.contains(CollateralConfig.BOTH)) {
            amountPo = getRatingThresholdAmount(contract, processDate, idRatingMatrixPo, ratingDirectionPo, poId);
        }

        if (amountPo != null) {
            try {
                Amount amount = new Amount(amountPo);
                if (amount.get() > 0.0) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Log.error(CollateralizedTradesReportLogic.class, e); //sonar
            }
        }

        if (leHasThreshold(leThresholdType) || leThresholdType.contains(CollateralConfig.BOTH)) {
            amountLe = getRatingThresholdAmount(contract, processDate, idRatingMatrixLe, ratingDirectionLe, ctpyId);
        }

        if ((amountLe == null) || amountLe.isEmpty()) {
            return false;
        }

        try {
            Amount amount = new Amount(amountLe);
            if (amount.get() > 0.0) {
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        // both are empty or equal zero
        return false;
    }

    /*
     * returns
     */

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static String getRatingThresholdAmount(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                                   String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return null;
        }

        if (idRatingMatrix <= 0) {
            return null;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return null;
        }

        if (idLe < 0) {
            return null;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return null;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class, "Contract rating configuration error. Contract Name: "
                    + contract.getName());
            return null;
        }

        return ratingToday.getThreshold();
    }

    /**
     * Just checks that trade and MCContract have been passed as parameters
     *
     * @param trade
     * @param marginCallConfig
     */
    private static boolean checkTradeAndMCContractNotNull(Trade trade, CollateralConfig marginCallConfig,
                                                          MarginCallDetailEntryDTO mcdet) {

        if (mcdet == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "No MarginCallDetailEntry received as parameter in checkTradeAndMCContractNotNull method.");
            return false;
        }
        if (trade == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "No trade received as parameter in checkTradeAndMCContractNotNull method.");
            return false;
        }
        if (marginCallConfig == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "No Collateral Contract received as parameter in checkTradeAndMCContractNotNull method.");
            return false;
        }
        return true;
    }

    /* inner class: Mtm class wrapper to store and process this data when MTM data is required */
    class MtmWrapper {

        private Double npv;

        private Double npvBase;

        private Double marginCall;

        private Double indepAmount;

        private Double indepAmountBase;

        private String npvCcy;

        private String npvBaseCcy;

        private String indepAmountCcy;

        private String indepAmountBaseCcy;

        private Double closingPrice;

        public MtmWrapper(PLMark plMark, Trade trade) {
            if (plMark != null) {
                PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV);
                if (plValue != null) {
                    this.npv = plValue.getMarkValue();
                    this.npvCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV_BASE);
                if (plValue != null) {
                    this.npvBase = plValue.getMarkValue();
                    this.npvBaseCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_INDEPENDENT_AMOUNT);
                if (plValue != null) {
                    this.indepAmount = plValue.getMarkValue();
                    this.indepAmountCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE);
                if (plValue != null) {
                    this.indepAmountBase = plValue.getMarkValue();
                    this.indepAmountBaseCcy = plValue.getCurrency();
                }
                // For ISMA Repo
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CLOSING_PRICE);
                if (plValue != null) {
                    this.closingPrice = plValue.getMarkValue();
                }
            }
        }

        public DisplayValue getMarginCall() {
            return this.marginCall == null ? null : CollateralUtilities.formatAmount(this.marginCall, this.npvCcy);
        }

        public DisplayValue getNpv() {
            return this.npv == null ? null : CollateralUtilities.formatAmount(this.npv, this.npvCcy);
        }

        public DisplayValue getNpvBase() {
            return this.npvBase == null ? null : CollateralUtilities.formatAmount(this.npvBase, this.npvBaseCcy);
        }

        public DisplayValue getIndepAmount() {
            return this.indepAmount == null ? null : CollateralUtilities.formatAmount(this.indepAmount,
                    this.indepAmountCcy);
        }

        public DisplayValue getIndepAmountBase() {
            return this.indepAmountBase == null ? null : CollateralUtilities.formatAmount(this.indepAmountBase,
                    this.indepAmountBaseCcy);
        }

        public String getClosingPrice() {
            // No rounding for closing price, displayed like it comes from SUSI
            return this.closingPrice == null ? null : (new Amount(this.closingPrice)).toString();
        }

        public String getNpvCcy() {
            return this.npvCcy;
        }

        public String getNpvBaseCcy() {
            return this.npvBaseCcy;
        }

        public String getIndepAmountCcy() {
            return this.indepAmountCcy;
        }

        public String getIndepAmountBaseCcy() {
            return this.indepAmountBaseCcy;
        }
    }

}
