package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.pledge.util.TripartyPledgeProrateCalculator;
import calypsox.tk.util.ScheduledTaskImportRepoMtM;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.product.flow.CashFlowIndemnity;
import com.calypso.tk.product.flow.CashFlowPrincipal;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMark;
import com.calypso.tk.util.CurrencyUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RepoInformesReport extends TradeReport {

    ConcurrentHashMap<Long, ReportRow> listOfRepos = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, ReportRow> listOfPledge = new ConcurrentHashMap<>();
    TripartyPledgeProrateCalculator calculator = null;

    private static final String MUREX_ROOT_CONTRACT = "MurexRootContract";
    public static final String FATHER_TRIPARTY_REPO = "fatherTripartyRepo";
    private static final String TRADE = "Trade";
    public static final String MTM_NET_MUREX = "MTM_NET_MUREX";
    public static final String REPO_INTEREST_RATE = "RepoInterestRate";
    public static final String REPO_BSB_FLOWS = "RepoBSBFlows";

    private static final String INTEREST = "INTEREST";
    private static final String COUPON = "COUPON";
    private static final String INDEMNITY = "INDEMNITY";
    private static final String PRINCIPAL = "PRINCIPAL";

    public ReportOutput load(Vector errorMsgs) {
        final DefaultReportOutput reportOutput = (DefaultReportOutput) super.load(errorMsgs);
        Optional.ofNullable(reportOutput).ifPresent(output ->{
            init();
            processRows(output.getRows());
            processPledges();
            processRepos();
        });
        return reportOutput;
    }

    /**
     * Process Pledges and calculate prorate for MTM, PRINCIPAL and ACCRUAL, getting cres from father triparty repo
     */
    private void processPledges(){
        listOfPledge.values().parallelStream().forEach( row -> {
            Trade trade = (Trade) row.getProperty(TRADE);
            String internalReference = trade.getInternalReference();
            if(!Util.isEmpty(internalReference)){
                Trade fatherTripartyRepo = getTripartyRepo(internalReference);
                if(null!=fatherTripartyRepo){
                    row.setProperty(FATHER_TRIPARTY_REPO,fatherTripartyRepo);
                    row.setProperty(REPO_INTEREST_RATE,getRepoInterestRate(fatherTripartyRepo));
                    String murexRootContract = fatherTripartyRepo.getKeywordValue(MUREX_ROOT_CONTRACT);
                    trade.addKeyword(MUREX_ROOT_CONTRACT,murexRootContract);
                    calculator.calculate(trade,fatherTripartyRepo,row);
                }
            }
        });
    }

    /**
     *
     */
    private void processRepos(){
        listOfRepos.values().parallelStream().forEach(row -> {
            final JDatetime valDateTime = getValuationDatetime();
            final JDate valDate = getValDate();
            final Trade trade = (Trade)row.getProperty(TRADE);
            if (trade != null && trade.getProduct() instanceof Repo) {
                //Calculate FLows just for Repos BSB
                if(Repo.SUBTYPE_BSB.equalsIgnoreCase(trade.getProductSubType())){
                    final CashFlowSet cashFlows = generateRepoBSBFlows(trade, valDate, _pricingEnv);
                    final JDate startDate = ((Repo) trade.getProduct()).getStartDate();
                    final JDate endDate = ((Repo) trade.getProduct()).getEndDate();
                    double pastCoupons = calculatePastCoupons(cashFlows, valDate);
                    final double coupons = calculateCoupons(cashFlows, valDate);
                    row.setProperty(RepoInformesReportStyle.CUPONES_INTERMEDIOS,new Amount(coupons));
                    row.setProperty(RepoInformesReportStyle.EFECTIVO_IDA,new Amount(calculateEffIda(cashFlows,pastCoupons,startDate)));
                    row.setProperty(RepoInformesReportStyle.EFECTIVO_VUELTA,new Amount(calculateEffVuelta(cashFlows,valDate,endDate)));
                    row.setProperty(REPO_BSB_FLOWS,cashFlows);
                }
                row.setProperty(REPO_INTEREST_RATE,getRepoInterestRate(trade));

                Repo productRepo = (Repo) trade.getProduct();
                Double value = getMtmNetMurex(row, trade, valDateTime, valDate);
                int dec = CurrencyUtil.getRoundingUnit(productRepo.getCurrency());
                if (value != null && value != 0.0d && !Double.isNaN(value)) {
                    row.setProperty(MTM_NET_MUREX,new Amount((Double) value, dec));
                }else {
                    JDate yesterdayValDate = valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                    value = getMtmNetMurex(row, trade, new JDatetime(yesterdayValDate, TimeZone.getDefault()), yesterdayValDate);
                    if (value != null && value != 0.0d && !Double.isNaN(value)) {
                        row.setProperty(MTM_NET_MUREX,new Amount((Double) value, dec));
                    }
                }
            }
        });
    }

    /**
     * Generate 2 list wuhit Repos and Pledges
     * @param rows
     */
    private void processRows(ReportRow[] rows){
        if(!Util.isEmpty(rows)){
            Arrays.stream(rows).parallel().forEach(row -> {
                if(row.getProperty(TRADE) instanceof Trade){
                    Trade trade = row.getProperty(TRADE);
                    Product product = trade.getProduct();
                    if(product instanceof Repo){
                        listOfRepos.put(trade.getLongId(),row);
                    }else if(product instanceof Pledge){
                        listOfPledge.put(trade.getLongId(),row);
                    }
                }
            });
        }
    }


    /**
     * Get Repo-Triparty from internal reference of Pledge
     * @param interalRef
     * @return
     */
    private Trade getTripartyRepo(String interalRef){
        try{
            final long fatherTripartyRepoID = Long.parseLong(interalRef);
            if(listOfRepos.containsKey(fatherTripartyRepoID)){
                final ReportRow row = listOfRepos.get(fatherTripartyRepoID);
                return  (Trade) row.getProperty(TRADE);
            }else {
                try {
                    return DSConnection.getDefault().getRemoteTrade().getTrade(fatherTripartyRepoID);
                } catch (CalypsoServiceException e) {
                    Log.error(this,"Error loading Trade: " + fatherTripartyRepoID + " " + e);
                }
            }
        }catch (Exception e){
            Log.error(this,"Error parsing internalReference " + interalRef + " " + e);
        }
        return null;
    }

    /**
     *
     * Calculate MTM NET just for Repo product
     * @param row
     * @param trade
     * @param valDateTime
     * @param valDate
     * @return
     */
    private Double getMtmNetMurex(ReportRow row, Trade trade, JDatetime valDateTime, JDate valDate) {
        PricingEnv pricingEnv = row.getProperty("PricingEnv");
        PricerMeasure pmAccrual = BOCreUtils.getInstance().calculatePM(valDateTime, trade, PricerMeasure.ACCRUAL_FIRST, "OFFICIAL_ACCOUNTING");
        RemoteMark remoteMark = DSConnection.getDefault().getRemoteMark();
        PLMark plMark = null;
        try {
            plMark = RemoteAPI.getMark(remoteMark, "PL", trade.getLongId(), (String)null, pricingEnv.getName(), valDate);
        } catch (PersistenceException e) {
            Log.error(this, "Error : " + e.toString());
        }
        if (plMark == null) {
            try {
                plMark = RemoteAPI.getMark(remoteMark, "NONE", trade.getLongId(), (String)null, pricingEnv.getName(), valDate);
            } catch (PersistenceException e) {
                Log.error(this, "Error : " + e.toString());
            }
            if (plMark == null) {
                return Double.NaN;
            }
        }

        // Using ScheduledTaskImportRepoMtM global vars because ST and PricerMeasure are directly connected
        Double plmMarketValueManValue = 0.0;
        PLMarkValue plmMarketValueMan = CollateralUtilities.retrievePLMarkValue(plMark, ScheduledTaskImportRepoMtM.MARKETVALUEMAN_PLMARKNAME);
        if (plmMarketValueMan == null) {
            Log.error(this, "No PLMark " + ScheduledTaskImportRepoMtM.MARKETVALUEMAN_PLMARKNAME + " found for Trade " + trade.getLongId() + " on date " + valDate);
        }
        else {
            plmMarketValueManValue = plmMarketValueMan.getMarkValue();
        }

        Double plmBuySellCashValue = 0.0;
        PLMarkValue plmBuySellCash = CollateralUtilities.retrievePLMarkValue(plMark, ScheduledTaskImportRepoMtM.BUYSELLCASH_PLMARKNAME);
        if (plmBuySellCash == null) {
            Log.error(this, "No PLMark " + ScheduledTaskImportRepoMtM.BUYSELLCASH_PLMARKNAME + " found for Trade " + trade.getLongId() + " on date " + valDate);
        }
        else {
            plmBuySellCashValue = plmBuySellCash.getMarkValue();
        }
        return plmMarketValueManValue + plmBuySellCashValue - pmAccrual.getValue();
    }

    /**
     * Calculate Repo Interest Rate
     *
     * @param trade
     * @return
     */
    private Amount getRepoInterestRate(Trade trade){
        Amount amount = new Amount();
        if(null!=trade){
            if (trade != null && trade.getProduct() instanceof Repo) {
                Repo productRepo = (Repo) trade.getProduct();
                Cash cash = productRepo.getCash();
                Double rate = null;
                JDate datecheked = null;
                if (cash != null) {
                    if (cash.getSpecificResets() != null) {
                        for (int i = 0; i < cash.getSpecificResets().size(); i++) {
                            if(checkValidation(i, datecheked, cash.getSpecificResets().get(i).getResetDate()))
                                if(!cash.getSpecificResets().get(i).getQuoteName().equalsIgnoreCase("CleanupOnly") && !cash.getSpecificResets().get(i).getResetDate().after(JDate.getNow()))
                                    rate = cash.getSpecificResets().get(i).getAmount() * 100.0D;
                            datecheked = cash.getSpecificResets().get(i).getResetDate();
                        }
                    }
                    if (cash.getFixedRateB() && rate == null) {
                        rate = cash.getFixedRate()  * 100.0D;
                    } else if (rate == null)
                        rate = cash.getSpread()  * 100.0D;
                }
                if(rate!=null) {
                    int dec = CurrencyUtil.getRoundingUnit(productRepo.getCurrency());
                    amount = new Amount((Double) rate, dec);
                }
            }
        }
        return amount;
    }

    private boolean checkValidation (int i, JDate dateChecked, JDate dateToCheck){
        return i != 0 ? dateToCheck.after(dateChecked) ?  dateToCheck.after(JDate.getNow()) ? false : true : false : true;
    }

    /**
     *
     * Calculate CashFlow for Repo BSB type.
     * @param trade
     * @param valDate
     * @param pricingEnv
     * @return
     */
    private CashFlowSet generateRepoBSBFlows(Trade trade, JDate valDate, PricingEnv pricingEnv){
        CashFlowSet flows = new CashFlowSet();
            SecFinance secFinance = (SecFinance)trade.getProduct();
            secFinance.setFlows((CashFlowSet)null);
            try {
                flows = secFinance.generateFlows(valDate);
                valDate = this.adjustValDateForBondDanishMortgage(secFinance, flows, valDate);
                secFinance.calculateAll(flows, pricingEnv, valDate);

            } catch (FlowGenerationException e) {
                Log.error(this,"Error: " + e);
            }
        return flows;
    }

    /**z
     *
     * SUM Cash Flows COUPON whit date after valDate and PRINCIPAL on repo start date
     *
     * @param cashFlowSet
     * @param coupons
     * @param repoStartDate
     * @return
     */
    private double calculateEffIda(CashFlowSet cashFlowSet,double coupons, JDate repoStartDate){
        double thesum = 0.0;
        thesum = Arrays.stream(cashFlowSet.getFlows())
                .filter(cash -> !(cash instanceof CashFlowPrincipal) && PRINCIPAL.equalsIgnoreCase(cash.getType()) && cash.getDate().equals(repoStartDate))
                .map(CashFlow::getAmount).reduce(thesum, Double::sum);
        thesum-=coupons;
        return thesum;
    }

    /**
     * SUM Cash Flows COUPON, whit date after report valDate, PRINCIPAL on repoStartDate, all INDEMNITY and all INTEREST
     *
     * @param cashFlowSet
     * @param valDate
     * @param repoEndDate
     * @return
     */
    private double calculateEffVuelta(CashFlowSet cashFlowSet,JDate valDate,JDate repoEndDate){
        double thesum = 0.0;
        thesum = Arrays.stream(cashFlowSet.getFlows())
                .filter(cash -> (cash instanceof CashFlowCoupon) ||
                        (!(cash instanceof CashFlowPrincipal) && PRINCIPAL.equalsIgnoreCase(cash.getType()) && cash.getDate().equals(repoEndDate))
                || (cash instanceof CashFlowIndemnity && INDEMNITY.equalsIgnoreCase(cash.getType()))
                || (cash instanceof CashFlowSimple && INTEREST.equalsIgnoreCase(cash.getType())))
                .map(CashFlow::getAmount).reduce(thesum, Double::sum);
        return thesum;
    }

    /**
     * Sum all Cash Flows COUPON type whit date after report valDate
     *
     * @param cashFlowSet
     * @param valDate
     * @return
     */
    private double calculateCoupons(CashFlowSet cashFlowSet,JDate valDate){
        double thesum = 0.0;
        for(CashFlow cash : cashFlowSet.getFlows()){
            if(cash instanceof CashFlowCoupon){
                if(cash.getCustomData()!=null && cash.getCustomData() instanceof JDate ){
                    if(((JDate)cash.getCustomData()).after(valDate)){
                        thesum += cash.getAmount();
                    }
                }else if(cash.getEndDate().after(valDate)){
                    thesum += cash.getAmount();
                }
            }
        }
        return thesum;
    }

    private double calculatePastCoupons(CashFlowSet cashFlowSet,JDate valDate){
        double thesum = 0.0;
        for(CashFlow cash : cashFlowSet.getFlows()){
            if(cash instanceof CashFlowCoupon){
                if(cash.getCustomData()!=null && cash.getCustomData() instanceof JDate ){
                    if(((JDate)cash.getCustomData()).lte(valDate)){
                        thesum += cash.getAmount();
                    }
                }else if(cash.getEndDate().lte(valDate)){
                    thesum += cash.getAmount();
                }
            }
        }
        return thesum;
    }

    private void init(){
        listOfRepos.clear();
        listOfPledge.clear();
        calculator = new TripartyPledgeProrateCalculator();
    }

    private JDate adjustValDateForBondDanishMortgage(SecFinance secFinance, CashFlowSet flows, JDate valDate) {
        JDate initialValDate = valDate;
        JDate temp = valDate;
        if (flows != null && secFinance.getSecurity() != null && "BondDanishMortgage".equals(secFinance.getSecurity().getType())) {
            Iterator var6 = flows.iterator();

            while(var6.hasNext()) {
                CashFlow flow = (CashFlow)var6.next();
                if (flow instanceof CashFlowCoupon && !"SECLENDING_FEE".equals(flow.getType()) && flow.getEndDate().lte(initialValDate) && ((CashFlowCoupon)flow).getPublicationDate() != null) {
                    temp = ((CashFlowCoupon)flow).getPublicationDate().addDays(1);
                }
            }
        }

        return DateUtil.max(new JDate[]{initialValDate, temp});
    }

}

