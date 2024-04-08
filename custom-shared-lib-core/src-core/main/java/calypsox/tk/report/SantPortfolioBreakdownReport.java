package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.*;
import calypsox.tk.report.portbreakdown.*;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;
import com.isban.efs2.webservice.Entry;
import com.jidesoft.utils.StringUtils;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SantPortfolioBreakdownReport extends SantReport implements CheckRowsNumberReport {

    private static final int SLEEP_TIME = 5000;
    private static final String FX_ENV = "FX_ENV";

    private static final long serialVersionUID = -8085259677304890403L;

    private static String[] twoLegs = {"CASH_FLOW_MATCHING", "INTEREST_RATE_SWAP", "FX_SWAP_NON_DELIVERABLE",
            "FX_SWAP_DELIVERABLE", "FX_NON_DELIVERABLE_FORWARD", "FX_DELIVERABLE_SPOT", "FX_DELIVERABLE_FORWARD",
            "EQUITY_SWAP", "CURRENCY_SWAP", "BASIS_SWAP"};

    private static List<String> twoLegsUnderLyings = Arrays.asList(twoLegs);

    private static final boolean enableMultiThreading = true;

    private SantGenericQuotesLoader quotesLoader;

    private SantExcludedTradeLoader excludeLoader;

    private static final String PRICING_ENV = "PricingEnvName";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String DECIMAL_FORMAT = "#0.00";


    private PricingEnv pricingEnv;
    private String fxPricingEnv;

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @Override
    protected ReportOutput loadReport(Vector<String> errorMsgs) {

        try {
            this.fxPricingEnv = getFxPricingEnv(getReportTemplate());
            this.pricingEnv = getPriceEnv();
            return getReportOutput();


        } catch (RemoteException e) {
            String error = "Error loading PortfolioBreakdown Report\n";
            Log.error(this, error, e);
            errorMsgs.add(error + e.getMessage());
        } finally {
            System.out.println(new Date());
        }
        return null;
    }

    @Override
    protected JDate getValDate() {
        JDate valDate = getProcessStartDate();
        if (valDate == null) {
            return JDate.getNow();
        }
        return valDate;

    }

    private DefaultReportOutput getReportOutput() throws RemoteException {

        final DefaultReportOutput output = new DefaultReportOutput(this);

        Collection<SantTradeBrowserItem> items = loadTradeBrowserItems();

        final List<ReportRow> rows = new ArrayList<>();
        Iterator<SantTradeBrowserItem> it = items.iterator();
        while (it.hasNext()) {
            final SantTradeBrowserItem item = it.next();
            ReportRow row = new ReportRow(item.getTrade(), ReportRow.TRADE);
            row.setProperty(ReportRow.MARGIN_CALL_CONFIG, item.getMarginCall());
            row.setProperty(SantTradeBrowserReportTemplate.VAL_DATE, getValDate());
            row.setProperty(SantPortfolioBreakdownReportTemplate.TRADE_WRAPPER,
                    tradeWrapperFactory(item.getTrade(), item.getMarginCall()));
            row.setProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_VAL_DATE,
                    mtmWrapperFactory(item.getPLMark(getValDate()), item.getTrade()));
            row.setProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_PREVIOUS_DAY,
                    mtmWrapperFactory(item.getPLMark(getPreviousDayFromCurrentDate()), item.getTrade()));

            row = filterRowAfterLoad(row);
            if (row != null) {
                rows.add(row);
            }
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));

        //Generate a task exception if the number of rows is out of an umbral defined
        HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
        if (!value.isEmpty() && value.keySet().iterator().next().equals("ScheduledTask: ")){
            checkAndGenerateTaskReport(output, value);
        }
        return output;
    }

    private PortfolioBreakdownMTMWrapper mtmWrapperFactory(PLMark plMark, Trade trade) {
        PortfolioBreakdownMTMWrapper wrapper;
        if (!Util.isEmpty(trade.getKeywordValue("MurexRootContract"))) {
            wrapper = mxMtmWrapperFactory(plMark, trade);
        } else {
            wrapper = new PortfolioBreakdownMTMWrapper(plMark, trade, this.getValuationDatetime());
        }
        return wrapper;
    }


    private PortfolioBreakdownMTMWrapper mxMtmWrapperFactory(PLMark plMark, Trade trade) {
        PortfolioBreakdownMTMWrapper wrapper;
        if (trade.getProduct() instanceof Repo &&
                isRepoMurexWorkflowSubType(trade)) {
            wrapper = new RepoMxPortBreakdownWrapper(plMark, trade, this.getValuationDatetime());
        } else {
            wrapper = new MxPortBreakdownWrapper(plMark, trade, this.getValuationDatetime());
        }
        return wrapper;
    }

    private PortBreakdownTradeWrapper tradeWrapperFactory(Trade trade, CollateralConfig cc) {
        PortBreakdownTradeWrapper wrapper;
        if (Optional.ofNullable(trade).map(Trade::getProduct).map(p -> p instanceof Bond).orElse(false)) {
            wrapper = new BondPortBreakdownTradeWrapper(trade, cc, getValDate(), this.quotesLoader);
        } else {
            wrapper = new PortBreakdownTradeWrapper(trade, cc, getValDate(), this.quotesLoader);
        }
        return wrapper;
    }


    private boolean isRepoMurexWorkflowSubType(Trade trade) {
        return !Util.isEmpty(trade.getKeywordValue("WorkflowSubType"))
                && trade.getKeywordValue("WorkflowSubType").equalsIgnoreCase("RepoMurex");
    }

    protected JDate getPreviousDayFromCurrentDate() {
        JDate previousDay = JDate.getNow().addDays(-1);
        while (previousDay.isWeekEndDay()) {
            previousDay = previousDay.addDays(-1);
        }
        return previousDay;
    }

    /**
     * @return Returns null if the row must NOT be added to the result. Otherwise returns the row.
     */
    protected ReportRow filterRowAfterLoad(final ReportRow row) {


        //Filter trades for settle date when the contract is REAL_SETTLEMENT.
        // if (!filterTradeBySettleDate(row)) return null;

        // Filter from 'MTM Zero' checkbox
        final ReportTemplate template = getReportTemplate();
        Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
        final Boolean skipMtmZeroRows = (Boolean) template.get(SantGenericTradeReportTemplate.MTM_ZERO);    // if 'MTM Zero' checkbox is checked --> rows with field 'MTM Base Value'=0 (or empty) must NOT be added to the result
        if (skipMtmZeroRows != null && skipMtmZeroRows && !trade.getStatus().getStatus().equals(STATUS_VERIFIED)) {            // if checkbox state is unknown (skipMtmZeroRows == null) --> don't skip row
            final PortfolioBreakdownMTMWrapper mtmWrapper = row.getProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_VAL_DATE);
            if (mtmWrapper != null && mtmWrapper.getNpvBase() != null) {
                DisplayValue npvBase = mtmWrapper.getNpvBase();        // getNpvBase() returns the value used in field 'MTM Base Value' (see SantPortfolioBreakdownReportStyle.getColumnValue())
                NumberFormat formatter = new DecimalFormat(DECIMAL_FORMAT);
                Amount amount = new Amount(formatter.format(npvBase.get()));
                if (amount.get() == 0) {
                    return null;
                }
            } else {
                return null;
            }
            final PortfolioBreakdownMTMWrapper mtmWrapperPreviousDay = row.getProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_PREVIOUS_DAY);
            if (mtmWrapperPreviousDay != null && mtmWrapperPreviousDay.getNpvBase() != null) {
                DisplayValue npvBase = mtmWrapperPreviousDay.getNpvBase();
                NumberFormat formatter = new DecimalFormat(DECIMAL_FORMAT);
                Amount amount = new Amount(formatter.format(npvBase.get()));
                if (amount.get() == 0) {
                    return null;
                }
            } else {
                return null;
            }
        }

        if (null != this.excludeLoader) {
            // Filter from exclusions
            Map<Long, Boolean> excludedMap = this.excludeLoader.getDataAsMap();

            Boolean isExcluded = excludedMap.get(trade.getLongId());
            if ((isExcluded != null) && isExcluded) {
                return null;
            }
        }

        // Row passed all filters --> add to the result
        return row;
    }

    private String getFXQuotesSqlWhere() {
        String quoteSetName = "";
        if (!Util.isEmpty(this.fxPricingEnv)) {
            quoteSetName = this.fxPricingEnv;
        } else {
            quoteSetName = this.pricingEnv.getQuoteSetName();
        }
        // everis - SantPortfolioBreakdown Optimization.
        Calendar valDate = getValDate().getJDatetime().asCalendar();
        valDate.setTimeZone(Util.getReferenceTimeZone());
        valDate.set(Calendar.HOUR_OF_DAY, 0);
        valDate.set(Calendar.MINUTE, 0);
        valDate.set(Calendar.SECOND, 0);
        valDate.set(Calendar.MILLISECOND, 0);
        JDatetime valDateIni = new JDatetime(valDate.getTime());
        valDate.set(Calendar.HOUR_OF_DAY, 23);
        valDate.set(Calendar.MINUTE, 59);
        valDate.set(Calendar.SECOND, 59);
        valDate.set(Calendar.MILLISECOND, 999);
        JDatetime valDateEnd = new JDatetime(valDate.getTime());
        final StringBuilder sb = new StringBuilder(" quote_set_name= '" + quoteSetName + "'").append(
                " and quote_name like 'FX.%' and length(quote_name) = 10 and quote_date between ").append(
                Util.datetime2SQLString(valDateIni)).append(" and ").append(Util.datetime2SQLString(valDateEnd));
        // everis - SantPortfolioBreakdown Optimization.
        return sb.toString();
    }

    private Collection<SantTradeBrowserItem> loadTradeBrowserItems() throws RemoteException {
        final ReportTemplate template = getReportTemplate();
        final JDate valDate = getValDate();


        // load quotes
        List<String> quotesWhereList = new ArrayList<String>();
        quotesWhereList.add(getFXQuotesSqlWhere());
        this.quotesLoader = new SantGenericQuotesLoader(enableMultiThreading, quotesWhereList);
        this.quotesLoader.load();

        Collection<SantTradeBrowserItem> items = handleQuery(template, valDate);

        if (this.quotesLoader.isAlive()) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }

        return items;
    }

    // GSM: hotfix: child IA contracts must be shown too, with the data of the father //1158802 -Temporal Chapuza
    protected Collection<SantTradeBrowserItem> handleQuery(final ReportTemplate template, final JDate valDate)
            throws RemoteException {
        final HashMap<Long, SantTradeBrowserItem> tradeItemsMap = new HashMap<>();

        String agreementIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        // GSM: hotfix: child IA contracts must be shown too, with the data of the father //1158802
        // Load contracts
        SantGenericContractsLoader contractsLoader = new SantGenericContractsLoader(enableMultiThreading, template,
                agreementIds);
        contractsLoader.load();

        while (contractsLoader.isAlive()) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }

        // We have all the contracts with effective type different to REAL_SETTLEMENT
        final Map<Integer, CollateralConfig> contracts = contractsLoader.getContractsMap();
        final Map<Integer, CollateralConfig> fatherContracts = contractsLoader.getFatherContractsDataAsMap();

        // We have all the contracts with effective type equals to REAL_SETTLEMENT
        final Map<Integer, CollateralConfig> realSettleContracts = contractsLoader.getRealSettleContractsMap();
        final Map<Integer, CollateralConfig> fatherRealSettleContracts = contractsLoader.getFatherRealSettleContractsDataMap();


        if (contracts.isEmpty() && realSettleContracts.isEmpty()) {
            return tradeItemsMap.values(); // nothingN
        }

        // everis - SantPortfolioBreakdown Optimization.
        // BAU 6.1 - Filter only OPEN contracts.
        // Retrieve the list of parent contracts
        final String[] fatherAgreementIds = contractsLoader.getStringListAgreementIds();

//        final String[] fatherAgreementIds = {"18459757"};
        ArrayList<SantPortfolioTradesLoader> arrTradesLoaders = new ArrayList<SantPortfolioTradesLoader>();
        for (int i = 0; i < fatherAgreementIds.length; i++) {
            String fatherAgreeSubList = fatherAgreementIds[i];
            // Load relevant Trades
            SantPortfolioTradesLoader tradesLoader = new SantPortfolioTradesLoader(enableMultiThreading, template,
                    fatherAgreeSubList, valDate);

            tradesLoader.load();
            arrTradesLoaders.add(tradesLoader);
        }

        for (int i = 0; i < arrTradesLoaders.size(); i++) {
            SantPortfolioTradesLoader tradesLoader = arrTradesLoaders.get(i);
            while (tradesLoader.isAlive()) {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    Log.error(this, e);
                }
            }
        }

        final Set<Trade> trades = new HashSet<>();
        for (int i = 0; i < arrTradesLoaders.size(); i++) {
            SantPortfolioTradesLoader tradesLoader = arrTradesLoaders.get(i);
            trades.addAll(tradesLoader.getDataAsList());
        }

        //Loading trades from REAL_SETTLEMENT Contracts
        loadTradesFromRealSettlementMcc(contractsLoader, trades, valDate);


        // everis - SantPortfolioBreakdown Optimization.
        // at this point, we build all the trades associated to the father contract ID

        //Merge all contracts to last filter.
        final Map<Integer, CollateralConfig> mergedContracts = Stream.concat(contracts.entrySet().stream(), realSettleContracts.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, HashMap::new));
        final HashMap<Integer, CollateralConfig> mergedFatherContracts = Stream.concat(fatherContracts.entrySet().stream(), fatherRealSettleContracts.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, HashMap::new));


        final Map<Integer, List<Trade>> tradesPerMCCParent = getTradesPerContractParentAndChilds(mergedContracts, trades, mergedFatherContracts);

        for (Integer contract_id : tradesPerMCCParent.keySet()) {

            final List<Trade> tradesList = tradesPerMCCParent.get(contract_id);

            for (Trade trade : tradesList) {

                final SantTradeBrowserItem tradeItem = new SantTradeBrowserItem();
                tradeItem.setTrade(trade);

                final CollateralConfig contract = contracts.get(contract_id); // the key, child contracts too
                final CollateralConfig realSettleContract = realSettleContracts.get(contract_id);
                if (contract == null && realSettleContract == null) {
                    continue;
                }
                if (null != contract) tradeItem.setMarginCall(contract);
                if (null != realSettleContract) tradeItem.setMarginCall(realSettleContract);

                tradeItemsMap.put(trade.getLongId(), tradeItem);
            }
        }

        // 3. Load PLMarks
        // JAT y GSM 22/03/2015 - Fix to set PE from report selection

        //String pricingEnvName
        final Vector<String> holidays = new Vector<String>();
        holidays.add("SYSTEM");
        Set<JDate> valDateSet = new HashSet<JDate>();
        valDateSet.add(valDate);
        valDateSet.add(valDate.addBusinessDays(-1, holidays));

        // Load PL Marks
        SantGenericPLMarksLoader plMarksLoader = new SantGenericPLMarksLoader(true, tradeItemsMap, valDateSet,
                this.pricingEnv.getName());
        plMarksLoader.load();

        //No check if undelying have a isExcluded flag
        String isExcludeActive = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "ExcludeTradesPortfolio");
        if (Boolean.parseBoolean(isExcludeActive)) {
            this.excludeLoader = new SantExcludedTradeLoader(true, tradeItemsMap.keySet(), getValDate());
            this.excludeLoader.load();
        }
        while (plMarksLoader.isAlive() || (null != this.excludeLoader && this.excludeLoader.isAlive())) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }

        return tradeItemsMap.values();
    }


    private Map<Integer, List<Trade>> getTradesPerContractParentAndChilds(Map<Integer, CollateralConfig> contracts,
                                                                          Set<Trade> trades, Map<Integer, CollateralConfig> fatherContracts) {

        Map<Integer, List<Trade>> MasterMCContractTrades = new HashMap<Integer, List<Trade>>();
        Map<Integer, List<Trade>> result = new HashMap<Integer, List<Trade>>();
        Iterator iterator = trades.iterator();
        while (iterator.hasNext()) {
            Trade trade = (Trade) iterator.next();
            if ("CONTRACT_IA".equals(trade.getProductSubType())
                    || "DISPUTE_ADJUSTMENT".equals(trade.getProductSubType())) {
                continue;
            }
            CollateralConfig contract = null;
            int tradeContractID = 0;
            if (!Util.isEmpty(trade.getInternalReference())) {
                // everis - SantPortfolioBreakdown Optimization.
                tradeContractID = Integer.parseInt(trade.getInternalReference()); // 1
                contract = findFatherContract(tradeContractID, fatherContracts); // // 3
                // everis - SantPortfolioBreakdown Optimization.
            }
            if (contract == null) {
                continue;
            }

            if ("FICTICIO".equalsIgnoreCase(trade.getKeywordValue("SecLendingTrade")) || !isTransferSettled(trade, contract)) {
                continue;
            }

            if (MasterMCContractTrades.containsKey(tradeContractID)) {
                MasterMCContractTrades.get(tradeContractID).add(trade);

            } else {

                final List<Trade> tempTradesList = new ArrayList<Trade>();
                tempTradesList.add(trade);
                MasterMCContractTrades.put(tradeContractID, tempTradesList);
            }
        }

        for (Integer contract_id : contracts.keySet()) {

            final CollateralConfig contract = contracts.get(contract_id);
            final int MasterMCContractID = contract.getMasterAgreementId();

            if (MasterMCContractTrades.containsKey(MasterMCContractID)) {
                result.put(contract_id, MasterMCContractTrades.get(MasterMCContractID));
            }
        }

        return result;
    }

    private CollateralConfig findFatherContract(int tradeContractID, Map<Integer, CollateralConfig> fatherContracts) {
        // everis - SantPortfolioBreakdown Optimization.
//		for (Integer i : contracts.keySet()) {
//			if (contracts.get(i).getMasterAgreementId() == tradeContractID) {
//				return contracts.get(i).getMasterAgreement();
//			}
//		}
        CollateralConfig cc = fatherContracts.get(tradeContractID);
        return cc;
        // everis - SantPortfolioBreakdown Optimization.
    }

    public static boolean is2Legs(final String underlying) {
        return twoLegsUnderLyings.contains(underlying);

    }

    private String getFxPricingEnv(ReportTemplate reportTemplate) {
        if (reportTemplate.getAttributes().get(FX_ENV) != null) {
            return reportTemplate.getAttributes().get(FX_ENV).toString();
        }
        return "";
    }

    /**
     * @return Return PricingEnv for load PLMarks and FXRates (if FX_ENV is not set)
     */
    private PricingEnv getPriceEnv() {
        PricingEnv pricingEnv = new PricingEnv();
        ReportTemplate template = getReportTemplate();

        if (super.getPricingEnv() != null) {
            pricingEnv = super.getPricingEnv();
            return pricingEnv;

        } else if (template != null && template.getAttributes() != null && template.getAttributes().get(PRICING_ENV) != null) {

            String pricingEnvName = (String) template.getAttributes().get(PRICING_ENV);
            //load pricing env
            try {
                PricingEnv env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(pricingEnvName);
                if (env != null) {
                    pricingEnv = env;
                    return pricingEnv;
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Cannot load pricing env: " + pricingEnvName);
            }
        }

        pricingEnv.setName("DirtyPrice");
        pricingEnv.setQuoteSetName("DirtyPrice");
        Log.error(this, "PricingEnv not set up in the report. Using as default DirtyPrice");


        return pricingEnv;
    }

    private void loadTradesFromRealSettlementMcc(SantGenericContractsLoader contractsLoader, Set<Trade> trades, JDate valDate) {
        final String[] fatherRealSettleAgreementIds = contractsLoader.getStringListRealSettleOpenAgreementIds();
        ArrayList<SantPortfolioRealSettlementTradesLoader> arrTradesRealSettlementLoaders = new ArrayList<>();
        for (int i = 0; i < fatherRealSettleAgreementIds.length; i++) {
            String fatherAgreeSubList = fatherRealSettleAgreementIds[i];
            SantPortfolioRealSettlementTradesLoader tradesLoader = new SantPortfolioRealSettlementTradesLoader(enableMultiThreading, getReportTemplate(), fatherAgreeSubList, valDate);
            tradesLoader.load();
            arrTradesRealSettlementLoaders.add(tradesLoader);
        }

        for (int i = 0; i < arrTradesRealSettlementLoaders.size(); i++) {
            SantPortfolioRealSettlementTradesLoader tradesLoader = arrTradesRealSettlementLoaders.get(i);
            while (tradesLoader.isAlive()) {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    Log.error(this, e);
                }
            }
        }

        for (int i = 0; i < arrTradesRealSettlementLoaders.size(); i++) {
            SantPortfolioRealSettlementTradesLoader tradesLoader = arrTradesRealSettlementLoaders.get(i);
            trades.addAll(tradesLoader.getDataList());
        }
    }

    private boolean checkStartDateOfSecFinance(Trade trade) {
        return trade.getProduct() instanceof SecFinance && ((SecFinance) trade.getProduct()).getStartDate().lte(getValDate());
    }

    private boolean isTransferSettled(Trade trade, CollateralConfig config) {
        if (config.getEffDateType().equalsIgnoreCase("REAL_SETTLEMENT")) {
            if (checkStartDateOfSecFinance(trade)) {
                try {
                    TransferArray transfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId());
                    return transfers.stream()
                            .filter(s -> s.getTransferType().equalsIgnoreCase("SECURITY") && s.getStatus().equals(Status.SETTLED))
                            .min(Comparator.comparing(BOTransfer::getLongId))
                            .isPresent();
                } catch (CalypsoServiceException e) {
                    Log.error(this.getClass().getSimpleName(), e);
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
