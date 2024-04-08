package calypsox.tk.report;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.*;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;

import java.rmi.RemoteException;
import java.util.*;

public class SantIntragroupPortfolioBreakdownReport extends SantReport {

    private static final long serialVersionUID = 123L;

    private static String[] twoLegs = {"CASH_FLOW_MATCHING", "INTEREST_RATE_SWAP", "FX_SWAP_NON_DELIVERABLE",
            "FX_SWAP_DELIVERABLE", "FX_NON_DELIVERABLE_FORWARD", "FX_DELIVERABLE_SPOT", "FX_DELIVERABLE_FORWARD",
            "EQUITY_SWAP", "CURRENCY_SWAP", "BASIS_SWAP"};

    private static List<String> twoLegsUnderLyings = Arrays.asList(twoLegs);

    private static final boolean enableMultiThreading = true;

    private SantGenericQuotesLoader quotesLoader;

    private SantExcludedTradeLoader excludeLoader;

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @Override
    protected ReportOutput loadReport(Vector<String> errorMsgs) {

        try {
            return getReportOutput();

        } catch (RemoteException e) {
            String error = "Error loading IntragroupPortfolioBreakdown Report\n";
            Log.error(this, error, e);
            errorMsgs.add(error + e.getMessage());
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

        Collection<SantTradeBrowserItem> items = null;

        items = loadTradeBrowserItems();

        final List<ReportRow> rows = new ArrayList<ReportRow>();

        for (final SantTradeBrowserItem item : items) {

            ReportRow row = new ReportRow(item.getTrade(), ReportRow.TRADE);
            row.setProperty(ReportRow.MARGIN_CALL_CONFIG, item.getMarginCall());
            row.setProperty(SantTradeBrowserReportTemplate.VAL_DATE, getValDate());

            row.setProperty(SantIntragroupPortfolioBreakdownReportTemplate.TRADE_WRAPPER,
                    new TradeWrapper(item.getTrade(), item.getMarginCall()));

            row.setProperty(SantIntragroupPortfolioBreakdownReportTemplate.MTM_WRAPPER_VAL_DATE,
                    new MtmWrapper(item.getPLMark(getValDate()), item.getTrade()));

            // row.setProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_PREVIOUS_DAY,
            // new MtmWrapper(item.getPLMark(getPreviousDayFromValDate()), item.getTrade()));

            row = filterRowAfterLoad(row);
            if (row != null) {
                rows.add(row);
            }
        }

        output.setRows(rows.toArray(new ReportRow[rows.size()]));

        return output;
    }

    // // TODO to improve - take into account bank holidays
    // protected JDate getPreviousDayFromValDate() {
    // JDate valDate = getValDate();
    // JDate previousDay = valDate.addDays(-1);
    // while (previousDay.isWeekEndDay()) {
    // previousDay = previousDay.addDays(-1);
    // }
    // return previousDay;
    // }

    protected JDate getPreviousDayFromCurrentDate() {
        JDate previousDay = JDate.getNow().addDays(-1);
        while (previousDay.isWeekEndDay()) {
            previousDay = previousDay.addDays(-1);
        }
        return previousDay;
    }

    protected ReportRow filterRowAfterLoad(final ReportRow row) {
        final ReportTemplate template = getReportTemplate();
        final Boolean mtmZero = (Boolean) template.get(SantGenericTradeReportTemplate.MTM_ZERO);

        if (mtmZero == null) {
            return row;
        }

        if (!mtmZero) {
            final MtmWrapper mtmWrapper = (MtmWrapper) row
                    .getProperty(SantIntragroupPortfolioBreakdownReportTemplate.MTM_WRAPPER_VAL_DATE);
            if (mtmWrapper == null) {
                return null;
            }
            if (mtmWrapper.getNpv() == null) {
                return null;
            }
        }

        Map<Long, Boolean> excludedMap = this.excludeLoader.getDataAsMap();

        Trade trade = (Trade) row.getProperty(ReportRow.TRADE);

        Boolean isExcluded = excludedMap.get(trade.getLongId());
        if ((isExcluded != null) && isExcluded) {
            return null;
        }

        return row;
    }

    private String getFXQuotesSqlWhere() {
        final StringBuilder sb = new StringBuilder(" quote_set_name= 'OFFICIAL'").append(
                " and quote_name like 'FX.%' and length(quote_name) = 10 and TRUNC(quote_date) = ").append(
                Util.date2SQLString(getValDate()));

        return sb.toString();
    }

    @SuppressWarnings("unused")
    private String getBondQuotesSqlWhere() {
        final StringBuilder sb = new StringBuilder(" quote_set_name= 'DirtyPrice'").append(
                " and quote_name like 'Bond%'  and TRUNC(quote_date) = ").append(Util.date2SQLString(getValDate()));

        return sb.toString();
    }

    @SuppressWarnings("unused")
    private String getEquityQuotesSqlWhere() {
        final StringBuilder sb = new StringBuilder(" quote_set_name= 'OFFICIAL'").append(
                " and quote_name like 'Equity%'  and TRUNC(quote_date) = ").append(Util.date2SQLString(getValDate()));

        return sb.toString();
    }

    private Collection<SantTradeBrowserItem> loadTradeBrowserItems() throws RemoteException {

        final ReportTemplate template = getReportTemplate();
        final JDate valDate = getValDate();

        // load quotes
        List<String> quotesWhereList = new ArrayList<String>();
        quotesWhereList.add(getFXQuotesSqlWhere());

        // quotesWhereList.add(getBondQuotesSqlWhere());
        // quotesWhereList.add(getEquityQuotesSqlWhere());

        this.quotesLoader = new SantGenericQuotesLoader(enableMultiThreading, quotesWhereList);
        this.quotesLoader.load();

        Collection<SantTradeBrowserItem> items = handleQuery(template, valDate);

        if (this.quotesLoader.isAlive()) {
            try {
                Thread.sleep(1000);
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

        final String agreementIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        // GSM: hotfix: child IA contracts must be shown too, with the data of the father //1158802
        // Load contracts based on po and intragroup le fields
        SantIntragroupContractsLoader contractsLoader = new SantIntragroupContractsLoader(enableMultiThreading,
                template, agreementIds);
        contractsLoader.load();

        while (contractsLoader.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }
        // we have all the contracts
        final Map<Integer, CollateralConfig> contracts = contractsLoader.getDataAsMap();

        if (contracts.isEmpty()) {
            return tradeItemsMap.values(); // nothing
        }

        // Retrieve the list of parent contracts
        final String fatherAgreementIds = getFatherIdsStringList(contracts);

        // Load intragroup trades (trade's cpty must be intragroup le)
        SantIntragroupTradesLoader tradesLoader = new SantIntragroupTradesLoader(enableMultiThreading, template,
                fatherAgreementIds, valDate);

        tradesLoader.load();

        while (tradesLoader.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }

        final List<Trade> trades = tradesLoader.getDataAsList();
        // at this point, we build all the trades associated to the father contract ID

        final Map<Integer, List<Trade>> tradesPerMCCParent = getTradesPerContractParentAndChilds(contracts, trades);

        for (Integer contract_id : tradesPerMCCParent.keySet()) {

            final List<Trade> tradesList = tradesPerMCCParent.get(contract_id);

            for (Trade trade : tradesList) {

                final SantTradeBrowserItem tradeItem = new SantTradeBrowserItem();
                tradeItem.setTrade(trade);

                final CollateralConfig contract = contracts.get(contract_id); // the key, child contracts too

                if (contract == null) {
                    continue;
                }
                tradeItem.setMarginCall(contract);
                tradeItemsMap.put(trade.getLongId(), tradeItem);
            }
        }

        // 3. Load PLMarks
        String pricingEnvName = "DirtyPrice";
        Set<JDate> valDateSet = new HashSet<>();
        valDateSet.add(getValDate());

        // Load PL Marks
        SantGenericPLMarksLoader plMarksLoader = new SantGenericPLMarksLoader(true, tradeItemsMap, valDateSet,
                pricingEnvName);
        plMarksLoader.load();
        this.excludeLoader = new SantExcludedTradeLoader(true, tradeItemsMap.keySet(), getValDate());
        this.excludeLoader.load();
        while (plMarksLoader.isAlive() || this.excludeLoader.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }

        return tradeItemsMap.values();
    }

    private String getFatherIdsStringList(Map<Integer, CollateralConfig> contracts) {

        StringBuffer sb = new StringBuffer();
        ArrayList<Integer> ids = new ArrayList<Integer>();

        for (Integer id : contracts.keySet()) {
            final Integer fatherId = contracts.get(id).getMasterAgreement().getId();

            if (!ids.contains(fatherId)) {
                ids.add(fatherId);
            }
        }

        for (Integer i : ids) {
            sb.append(i).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private Map<Integer, List<Trade>> getTradesPerContractParentAndChilds(Map<Integer, CollateralConfig> contracts,
                                                                          List<Trade> trades) {

        Map<Integer, List<Trade>> MasterMCContractTrades = new HashMap<Integer, List<Trade>>();
        Map<Integer, List<Trade>> result = new HashMap<Integer, List<Trade>>();

        for (Trade trade : trades) {

            if ("CONTRACT_IA".equals(trade.getProductSubType())
                    || "DISPUTE_ADJUSTMENT".equals(trade.getProductSubType())) {
                continue;
            }

            final int tradeContractID = Integer.valueOf(trade.getInternalReference()); // 1
            final CollateralConfig contract = findFatherContract(tradeContractID, contracts);// contracts.get(tradeContractID).getMasterAgreement();
            // // 3
            if (contract == null) {
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
            final int MasterMCContractID = contract.getMasterAgreement().getId();

            if (MasterMCContractTrades.containsKey(MasterMCContractID)) {
                result.put(contract_id, MasterMCContractTrades.get(MasterMCContractID));
            }
        }

        return result;
    }

    private CollateralConfig findFatherContract(int tradeContractID, Map<Integer, CollateralConfig> contracts) {

        for (Integer i : contracts.keySet()) {
            final CollateralConfig daddy = contracts.get(i).getMasterAgreement();
            if (daddy.getId() == tradeContractID) {
                return daddy;
            }
        }
        return null;
    }

    class TradeWrapper {

        private Double principal;

        private Double principal2;

        private String principalCcy;

        private String principal2Ccy;

        private String mccCcy;

        private Double fxRate1;

        private Double fxRate2;

        private String underlying1;

        private String underlying2;

        private String instrument;

        private String direction;

        private Double initialCollateral;

        private JDate tradeDate;

        // for repo enhacements
        private double pool_factor;

        public TradeWrapper(Trade trade, CollateralConfig mcc) {
            this.mccCcy = mcc.getCurrency();
            buildTradeDate(trade);

            try {
                // GSM: build-underlying changes
                buildPrincipals(trade);
            } catch (Exception e) {
                Log.error(this, "Build principal issue for trade id = " + trade.getLongId(), e);
            }
            buildFXRate();
            buildUnderlyings(trade.getProduct());
            buildInstrument(trade);
            buildInitialCollateral(trade, mcc);
            buildFactors(trade); // for repo enhacements
        }

        private void buildTradeDate(Trade trade) {
            if (trade.getProductType().equals(Product.SEC_LENDING)) {
                String kw = trade.getKeywordValue("REAL_TRADE_DATE");
                if (!Util.isEmpty(kw)) {
                    this.tradeDate = JDate.valueOf(kw);
                }
            } else {
                this.tradeDate = trade.getTradeDate().getJDate(TimeZone.getDefault());
            }
        }

        // for testing purpose should not be used
        TradeWrapper(Trade trade) {
            buildInitialCollateral(trade, null);
        }

        private void buildInitialCollateral(Trade trade, CollateralConfig mcc) {
            Collateral collat = null;
            if (Product.REPO.equals(trade.getProductType())) {
                Repo repo = (Repo) trade.getProduct();
                if ((repo.getCollaterals() != null) && (repo.getCollaterals().size() > -1)) {
                    collat = (Collateral) repo.getCollaterals().get(0);
                }

            } else if (Product.SEC_LENDING.equals(trade.getProductType())) {
                SecLending secLending = (SecLending) trade.getProduct();
                if ((secLending.getCollaterals() != null) && (secLending.getCollaterals().size() > -1)) {
                    // System.out.println(trade.getLongId());
                    // GSM: error in CERT - OSLA
                    if ((secLending.getCollaterals() != null) && (secLending.getCollaterals().size() >= 1)) {
                        collat = (Collateral) secLending.getCollaterals().get(0);
                    }
                }
            }

            if (collat == null) {
                this.initialCollateral = null;
            } else {
                collat.setInitialFXRate(1.0d);
                double allInPrice = collat.computeAllInPrice();
                if (Product.BOND.equals(collat.getSecurityType())) {
                    allInPrice = allInPrice / 100;
                }
                this.initialCollateral = allInPrice * collat.getQuantity();
                if (Product.SEC_LENDING.equals(trade.getProductType())) {
                    double oslaFactor = 1;
                    try {
                        if (!Util.isEmpty(trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT))) {
                            oslaFactor = Double.valueOf(trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT)) / 100;
                        } else {
                            oslaFactor = Double.valueOf(mcc.getAdditionalField(CollateralStaticAttributes.MCC_HAIRCUT)) / 100;
                        }
                    } catch (Exception e) {
                        Log.error(this, e); //sonar
                        oslaFactor = 1;
                    }
                    // initialCollateral represents the value in term of CASH and not Security
                    // Therefore for a Loan of Security this value should be positive and negative for a Borrow of
                    // Security
                    this.initialCollateral = this.initialCollateral * oslaFactor * -1;
                }
            }
        }

        private void buildFactors(Trade trade) {
            Collateral collat = null;
            if (Product.REPO.equals(trade.getProductType())) {
                Repo repo = (Repo) trade.getProduct();
                if ((repo.getCollaterals() != null) && (repo.getCollaterals().size() > -1)) {
                    collat = (Collateral) repo.getCollaterals().get(0);
                }
                if (Product.BOND.equals(collat.getSecurityType())) {
                    Bond bond = (Bond) collat.getSecurity();
                    this.pool_factor = bond.getPoolFactor(getValDate());
                }
            }
        }

        private void buildInstrument(Trade trade) {
            if ("CollateralExposure".equals(trade.getProductType())) {
                this.instrument = trade.getProductSubType();
            } else {
                this.instrument = trade.getProductType();
            }

        }

        private void buildPrincipals(Trade input) {

            Product product = input.getProduct();

            if ((input == null) || (product == null)) {
                Log.error(this, "Cannot retrieve producto of the trade int. ref: " + input.getInternalReference());
            }

            this.principal = product.getPrincipal();
            this.principalCcy = product.getCurrency();

            // derivative
            if ((product instanceof CollateralExposure)) {

                // take the type
                buildTwoLegs(input);

                // if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
                //
                // CollateralExposure collatExpo = (CollateralExposure) product;
                // this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                // this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
                // // GSM: fix currency
                // this.principalCcy = (String) collatExpo.getAttribute("CCY_1");
                // }

            } else if (product instanceof SecLending) {
                this.principal = ((SecLending) product).getSecuritiesNominalValue(getValDate());
                this.direction = ((SecLending) product).getDirection();
            }
        }

        private void buildFXRate() {
            QuoteValue qv = null;
            if (this.mccCcy.equals(this.principalCcy)) {
                this.fxRate1 = 1.0d;
            } else {
                qv = SantIntragroupPortfolioBreakdownReport.this.quotesLoader.fetchFXQuoteValue(this.mccCcy,
                        this.principalCcy);
                if (qv != null) {
                    this.fxRate1 = qv.getClose();
                }
            }

            if (Util.isEmpty(this.principal2Ccy)) {
                return;
            }
            qv = null;
            if (this.mccCcy.equals(this.principal2Ccy)) {
                this.fxRate2 = 1.0d;
            } else {
                qv = SantIntragroupPortfolioBreakdownReport.this.quotesLoader.fetchFXQuoteValue(this.mccCcy,
                        this.principal2Ccy);
                if (qv != null) {
                    this.fxRate2 = qv.getClose();
                }
            }
        }

        /*
         * GSM: fix building underlyings -> take in two-legs trades, each trade based on the leg attribute. Also take
         * into consideration the trade direction to take the principal and the secondary leg data.
         */
        @SuppressWarnings("unchecked")
        private void buildUnderlyings(Product product) {

            // derivative
            if ((product instanceof CollateralExposure)) {
                CollateralExposure collatExpo = (CollateralExposure) product;
                if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
                    this.underlying1 = (String) collatExpo.getAttribute("UNDERLYING_1");
                } else {
                    this.underlying1 = (String) collatExpo.getAttribute("UNDERLYING");
                }
                this.underlying2 = (String) collatExpo.getAttribute("UNDERLYING_2");

            } else if (product instanceof Repo) {
                try {
                    Product secUnderlying = DSConnection.getDefault().getRemoteProduct()
                            .getProduct(((Repo) product).getUnderlyingSecurityId());
                    this.underlying1 = secUnderlying.getDescription();
                } catch (Exception e) {
                    Log.error(this, "Cannot retrieve security", e);
                }
            } else if (product instanceof SecLending) {
                SecLending secLending = (SecLending) product;
                final Vector<Collateral> leftCollaterals = secLending.getLeftCollaterals();
                if (leftCollaterals.size() > 0) {
                    this.underlying1 = leftCollaterals.get(0).getDescription();
                }
            }
        }

        /**
         * Builds the two legs of the trade, putting the data of the principal leg (nominal and ccy) on the left side
         * and the secondary data on the right.
         *
         * @param tradeBeanLeg1
         * @param tradeBeanLeg2
         * @return
         */
        private void buildTwoLegs(Trade trade) {

            CollateralExposure collatExpo = null;
            String tradeDirection = "";

            if ((trade == null) || (trade.getProduct() == null)) {
                return;
            }

            if (!(trade.getProduct() instanceof CollateralExposure)) {
                return;
            }

            collatExpo = (CollateralExposure) trade.getProduct();

            if ((collatExpo != null)) {
                tradeDirection = collatExpo.getDirection(trade);
                // small fix, lets show the direction of the product on the report
                this.direction = tradeDirection;
            }

            String leg1Direction = (String) collatExpo.getAttribute("DIRECTION_1");
            String leg2Direction = (String) collatExpo.getAttribute("DIRECTION_2");

            if ((leg1Direction == null) || (leg2Direction == null) || leg1Direction.isEmpty()
                    || leg2Direction.isEmpty()) {
                return;
            }

            // leg one with loan is principal
            if ("Buy".equalsIgnoreCase(tradeDirection) || "Loan".equalsIgnoreCase(tradeDirection)) {

                if (leg1Direction.equalsIgnoreCase("Loan") || leg1Direction.equalsIgnoreCase("Buy")) { // principal

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
                } else { // sell, leg1 is borrower

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_2");
                }

            } else { // leg2 is principal

                if (leg2Direction.equalsIgnoreCase("Borrower") || leg2Direction.equalsIgnoreCase("Sell")) { // principal

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_2");
                } else { // sell, leg1 is borrower

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
                }

            }
        }

        public JDate getTradeDate() {
            return this.tradeDate;
        }

        public String getUndelying1() {
            return this.underlying1;
        }

        public String getUndelying2() {
            return this.underlying2;
        }

        public DisplayValue getPrincipal() {
            return ((this.principal == null) || Double.isNaN(this.principal)) ? null : CollateralUtilities
                    .formatAmount(this.principal, this.principalCcy);
        }

        public DisplayValue getPrincipal2() {
            return ((this.principal2 == null) || Double.isNaN(this.principal2)) ? null : CollateralUtilities
                    .formatAmount(this.principal2, this.principal2Ccy);
        }

        public String getPrincipalCcy() {
            return this.principalCcy;
        }

        public String getPrincipal2Ccy() {
            return this.principal2Ccy;
        }

        public DisplayValue getFXRate1() {
            return ((this.fxRate1 == null) || Double.isNaN(this.fxRate1)) ? null : CollateralUtilities.formatFXQuote(
                    this.fxRate1, this.principalCcy, this.mccCcy);
        }

        public DisplayValue getFXRate2() {
            return ((this.fxRate2 == null) || Double.isNaN(this.fxRate2)) ? null : CollateralUtilities.formatFXQuote(
                    this.fxRate2, this.principal2Ccy, this.mccCcy);
        }

        public DisplayValue getInitialCollateral() {
            return ((this.initialCollateral == null) || Double.isNaN(this.initialCollateral)) ? null
                    : CollateralUtilities.formatAmount(this.initialCollateral, this.principalCcy);
        }

        public String getInstrument() {
            return this.instrument;
        }

        /**
         * @return the direction
         */
        public String getDirection() {
            return this.direction;
        }

        // for repo enhacements
        public double getPoolFactor() {
            return this.pool_factor;
        }

    }

    /* Mtm class wrapper */
    class MtmWrapper {

        private Double npv;

        private Double npvBase;

        private Double marginCall;

        @SuppressWarnings("unused")
        private Double marginCallBase;

        private Double indepAmount;

        private Double indepAmountBase;

        private String npvCcy;

        private String npvBaseCcy;

        private String indepAmountCcy;

        private String indepAmountBaseCcy;

        private Double closingPrice;

        // for RepoEnhacements
        private Double repoAccruedInterest;
        private Double bondAccruedInterest;
        private Double cleanPrice;
        private Double capitalFactor;
        private String repoAccruedInterestCcy;
        private String bondAccruedInterestCcy;
        private String cleanPriceCcy;
        private String capitalFactorCcy;

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
                // closingPrice = null if CSA

                // To be changed but for a quick fix:
                // Use of PLMARK MARGIN_CALL for SecLending because haircut is taken into account
                if (trade.getProduct() instanceof SecLending) {
                    plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_MARGIN_CALL);
                    if (plValue != null) {
                        // Margin Call PLMark represents the value in term of CASH and not Security
                        // Therefore for a Loan of Security this value should be positive and negative for a Borrow of
                        // Security
                        this.marginCall = plValue.getMarkValue();
                    }
                }

                // for RepoEnhacements
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_REPO_ACCRUED_INTEREST);
                if (plValue != null) {
                    this.repoAccruedInterest = plValue.getMarkValue();
                    this.repoAccruedInterestCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_BOND_ACCRUED_INTEREST);
                if (plValue != null) {
                    this.bondAccruedInterest = plValue.getMarkValue();
                    this.bondAccruedInterestCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CLEAN_PRICE);
                if (plValue != null) {
                    this.cleanPrice = plValue.getMarkValue();
                    this.cleanPriceCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CAPITAL_FACTOR);
                if (plValue != null) {
                    this.capitalFactor = plValue.getMarkValue();
                    this.capitalFactorCcy = plValue.getCurrency();
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

        // for RepoEnhacements
        public DisplayValue getRepoAccruedInterest() {
            return this.repoAccruedInterest == null ? null : CollateralUtilities.formatAmount(this.repoAccruedInterest,
                    this.repoAccruedInterestCcy);
        }

        public DisplayValue getBondAccruedInterest() {
            return this.bondAccruedInterest == null ? null : CollateralUtilities.formatAmount(this.bondAccruedInterest,
                    this.bondAccruedInterestCcy);
        }

        public DisplayValue getCleanPrice() {
            return this.cleanPrice == null ? null : CollateralUtilities.formatAmount(this.cleanPrice,
                    this.cleanPriceCcy);
        }

        public DisplayValue getCapitalFactor() {
            return this.capitalFactor == null ? null : CollateralUtilities.formatAmount(this.capitalFactor,
                    this.capitalFactorCcy);
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

    private static boolean is2Legs(final String underlying) {
        return twoLegsUnderLyings.contains(underlying);

    }

    /**
     * For testing purpose
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        DSConnection ds = ConnectionUtil.connect(args, "MainEntry");
        DSConnection.setDefault(ds);
        Integer[] tradeIds = {109942, 109948, 109949, 109950, 109953, 109952, 109951, 109954, 109933, 109935, 109938,
                109939, 109936, 109937, 109940, 109941, 109957, 109958, 12443, 335510, 335512};

        for (long tradeId : tradeIds) {
            Trade trade = ds.getRemoteTrade().getTrade(tradeId);
            if (trade == null) {
                continue;
            }
            SantIntragroupPortfolioBreakdownReport report = new SantIntragroupPortfolioBreakdownReport();
            TradeWrapper wrapper = report.new TradeWrapper(trade);

            System.out.println("Trade id = " + tradeId + " type = " + trade.getProductType() + " Initial Collateral = "
                    + wrapper.getInitialCollateral());
        }
    }

}
