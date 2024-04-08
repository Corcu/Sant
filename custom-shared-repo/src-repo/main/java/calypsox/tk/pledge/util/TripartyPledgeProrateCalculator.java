package calypsox.tk.pledge.util;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.report.RepoTripartyPledgeReportTemplate;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.PricerMeasureUtility;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class TripartyPledgeProrateCalculator {
    private static final String PAY = "PAY";
    private static final String RECEIVE = "REC";
    private static final String MTM_NET = "MTM_NET";
    private static final String PRINCIPAL = "PRINCIPAL";
    private static final String ACCRUAL = "ACCRUAL";
    private static final String MTM_NET_MUREX = "MTM_NET_MUREX";
    private static final String MARKETVALUEMAN = "MARKETVALUEMAN";

    private static final String PRICING_ENV = "OFFICIAL_ACCOUNTING";

    public static final String PLEDGE_PRORATE = "PledgeProrate";
    public static final String PLEDGE_PRORATE_MTM = "PledgeProrateMTM";
    public static final String PLEDGE_PRORATE_PRINCIPAL = "PledgeProratePrincipal";
    public static final String PLEDGE_PRORATE_ACCRUAL = "PledgeProrateAccrual";
    public static final String REPO_TRADE = "FatherRepoTrade";
    public static final String REPO_TRADE_DIRECTION = "RepoTradeDirection";
    public static final String PLEDGE_DIRECTION = "PledgeTradeDirection";
    public static final String PLEDGE_NOMINAL = "TradeNominal";

    private ConcurrentHashMap<Long, Trade> cachedFathersRepoTrades = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, HashMap<String,PricerMeasure>> cachedFathersRepoPricers = new ConcurrentHashMap<>();
    private int mtmPricerMeasureType = -1;
    private int marketValueManMeasureType = -1;

    /**
     *
     * Calculate prorate for triparty pledges
     * @param pledgeTrade
     * @param fatherTripartyRepo
     * @param row
     */
    public void calculate(Trade pledgeTrade, Trade fatherTripartyRepo, ReportRow row){
        Trade fatherRepoTrade = null;
        if(null!=fatherTripartyRepo){
            fatherRepoTrade = fatherTripartyRepo;
        }else {
            fatherRepoTrade = getFatherRepoTrade(pledgeTrade);
        }
        calculateProrate(pledgeTrade,fatherRepoTrade,row,null);
    }

    /**
     *
     * Calculate prorate for triparty pledges adjusting the last one to 100% of the distribution
     * @param tripartyPledgeBean
     *
     */
    public void calculate(TripartyPledgeProrateCalculatorBean tripartyPledgeBean){
        final Trade fatherRepo = tripartyPledgeBean.getFatherRepo();
        final List<ReportRow> rows = sortRowsByTradeID(tripartyPledgeBean.getRows());
        if(!Util.isEmpty(rows)){
            for(int i = 0;i<rows.size();i++){
                final ReportRow row = rows.get(i);
                calculateProrate((Trade) row.getProperty(ReportRow.TRADE),fatherRepo,row,tripartyPledgeBean);

                final Amount mtm = Optional.ofNullable(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM)).map(Amount.class::cast).orElse(new Amount(0.0));
                final Amount accrual = Optional.ofNullable(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL)).map(Amount.class::cast).orElse(new Amount(0.0));
                final Amount principal = Optional.ofNullable(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL)).map(Amount.class::cast).orElse(new Amount(0.0));

                if(i==rows.size()-1){ // Adjust Last Pledge
                    final Double fatherRepoTotalMTM = parseDouble(tripartyPledgeBean.getFatherRepoTotalMTM());
                    final Double fatherRepoTotalAccrual = parseDouble(tripartyPledgeBean.getFatherRepoTotalAccrual());
                    final Double fatherRepoTotalPrincipal =parseDouble(tripartyPledgeBean.getFatherRepoTotalPrincipal());

                    final Double mtmSUM = parseDouble(tripartyPledgeBean.getMtmSUM());
                    final Double accrualSUM = parseDouble(tripartyPledgeBean.getAccrualSUM());
                    final Double principalSUM = parseDouble(tripartyPledgeBean.getPrincipalSUM());

                    double mtmtResult = fatherRepoTotalMTM - (mtmSUM+mtm.get());
                    if(!(mtmtResult > 0.99) && mtmtResult!=0.0){
                        row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM, new Amount(fatherRepoTotalMTM - mtmSUM));
                    }

                    double accrualResult = fatherRepoTotalAccrual - (accrualSUM+accrual.get());
                    if(!(accrualResult> 0.99) && accrualResult!=0.0){
                        row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL, new Amount(fatherRepoTotalAccrual - accrualSUM));
                    }

                    double principalResult= fatherRepoTotalPrincipal - (principalSUM+principal.get());
                    if(!(principalResult > 0.99) && principalResult!=0.0){
                        row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL, new Amount(fatherRepoTotalPrincipal - principalSUM));
                    }

                }else {
                    tripartyPledgeBean.sumMTM(parseDouble(mtm.get()));
                    tripartyPledgeBean.sumPrincipal(parseDouble(principal.get()));
                    tripartyPledgeBean.sumAccrual(parseDouble(accrual.get()));
                }
            }
        }

    }

    private double parseDouble(double value){
        try{
            final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
            decimalSymbol.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat("0.00",decimalSymbol);
            return Double.parseDouble(df.format(value));
        }catch (Exception e){
            Log.error(this,"Error: " + e);
        }
        return 0.0;
    }



    /**
     * Calculate prorate for peldges
     *
     * @param pledgeTrade
     * @param fatherRepoTrade
     * @param row
     * @param tripartyPledgeBean
     */
    private void calculateProrate(Trade pledgeTrade, Trade fatherRepoTrade, ReportRow row, TripartyPledgeProrateCalculatorBean tripartyPledgeBean){
        if(Optional.ofNullable(pledgeTrade).isPresent() && Optional.ofNullable(fatherRepoTrade).isPresent()){

            final JDatetime valuationDatetime = getValDateTime(row);
            double proratePercentage = getProrate(pledgeTrade);
            final HashMap<String, PricerMeasure> pricerForCalculate = getPricerForCalculate(fatherRepoTrade,valuationDatetime);
            final HashMap<String, BOCre> boCreForCalculate = getBOCresForCalculate(fatherRepoTrade);

            double pricerMtm = Optional.ofNullable(pricerForCalculate.get(MTM_NET)).map(PricerMeasure::getValue).orElse(0.0);
            double pricerAccrual = Optional.ofNullable(pricerForCalculate.get(ACCRUAL)).map(PricerMeasure::getValue).orElse(0.0);
            double pricerMarketValueMan = Optional.ofNullable(pricerForCalculate.get(MARKETVALUEMAN)).map(PricerMeasure::getValue).orElse(0.0);
            final double crePrincipalAmount = Optional.ofNullable(boCreForCalculate.get(PRINCIPAL)).orElse(new BOCre()).getAmount(0);

            row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE,new Amount(proratePercentage));
            row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM,calculateProrate(pricerMtm, proratePercentage));
            row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MARKETVALUEMAN,calculateProrate(pricerMarketValueMan, proratePercentage));
            row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL, calculateProrate(pricerAccrual, proratePercentage));
            row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL,calculateProrate(crePrincipalAmount,proratePercentage));
            row.setProperty(RepoTripartyPledgeReportTemplate.REPO_TRADE,fatherRepoTrade);
            row.setProperty(RepoTripartyPledgeReportTemplate.REPO_TRADE_DIRECTION,mapDirection(fatherRepoTrade));
            row.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_DIRECTION,mapDirection(pledgeTrade));

            //Add Repo totals on Row
            if(Optional.ofNullable(tripartyPledgeBean).isPresent()){
                if(tripartyPledgeBean.getFatherRepoTotalMTM()==0.0){
                    tripartyPledgeBean.setFatherRepoTotalMTM(pricerMtm);
                }
                if(tripartyPledgeBean.getFatherRepoTotalAccrual()==0.0){
                    tripartyPledgeBean.setFatherRepoTotalAccrual(pricerAccrual);
                }
                if(tripartyPledgeBean.getFatherRepoTotalPrincipal()==0.0){
                    tripartyPledgeBean.setFatherRepoTotalPrincipal(crePrincipalAmount);
                }
            }

        }
    }

    /**
     * @param pledgeTrade
     * @return
     */
    private Double getProrate(Trade pledgeTrade){
        if(Optional.ofNullable(pledgeTrade).isPresent()){
            Locale locale = new Locale("es", "ES");
            final double marketValue = Util.stringToNumber(pledgeTrade.getKeywordValue("19A::MKTP"),locale);
            final double collateralValue = Util.stringToNumber(pledgeTrade.getKeywordValue("19A::COVA"),locale);
            if(Util.isNonZeroNumber(collateralValue) && Util.isNonZeroNumber(marketValue)){
                return Math.abs(marketValue/collateralValue)*100;
            }
        }
        return 0.0;
    }


    /**
     * Calculate MTM and ACCRUAL for father repo.
     *
     * @param trade
     * @return
     */
    private HashMap<String,PricerMeasure> getPricerForCalculate(Trade trade,JDatetime valDateTime){
        HashMap<String,PricerMeasure> pricerMeasures = new HashMap<>();

        if(Optional.ofNullable(trade).isPresent()){

            if(cachedFathersRepoPricers.containsKey(trade.getLongId())){
                pricerMeasures = cachedFathersRepoPricers.get(trade.getLongId());
            }else {
                final PricerMeasure accrual = BOCreUtils.getInstance().calculatePM(valDateTime, trade, PricerMeasure.ACCRUAL_FIRST, PRICING_ENV);
                pricerMeasures.put(ACCRUAL,accrual);

                loadMTMPricerType();
                if(existMTMPricer()){
                    final PricerMeasure mtm = BOCreUtils.getInstance().calculatePM(valDateTime, trade, mtmPricerMeasureType, PRICING_ENV);
                    pricerMeasures.put(MTM_NET,mtm);
                }

                loadMARKETVALUEMANPricerType();
                if(existMARKETVALUEMANPricer()){
                    final PricerMeasure marketValueMan = BOCreUtils.getInstance().calculatePM(valDateTime, trade, marketValueManMeasureType, PRICING_ENV);
                    pricerMeasures.put(MARKETVALUEMAN,marketValueMan);
                }

                cachedFathersRepoPricers.put(trade.getLongId(),pricerMeasures);
            }
        }
        return pricerMeasures;
    }

    /**
     * Load MTM_NET_MUREX Pricer Type
     */
    private void loadMTMPricerType(){
        if(mtmPricerMeasureType==-1){
            mtmPricerMeasureType = Arrays.stream(PricerMeasureUtility.getAllPricerMeasures(DSConnection.getDefault(), new Vector<>()))
                    .filter(p -> p.getName().equalsIgnoreCase(MTM_NET_MUREX))
                    .findFirst().map(PricerMeasure::getType).orElse(0);
        }else if(mtmPricerMeasureType==0){
            Log.info(this,"Pricer MTM_NET_MUREX does not exist on environment.");
        }
    }

    /**
     * Load MTM_NET_MUREX Pricer Type
     */
    private void loadMARKETVALUEMANPricerType(){
        if(marketValueManMeasureType==-1){
            marketValueManMeasureType = Arrays.stream(PricerMeasureUtility.getAllPricerMeasures(DSConnection.getDefault(), new Vector<>()))
                    .filter(p -> p.getName().equalsIgnoreCase(MARKETVALUEMAN))
                    .findFirst().map(PricerMeasure::getType).orElse(0);
        }else if(marketValueManMeasureType==0){
            Log.info(this,"Pricer MARKETVALUEMAN does not exist on environment.");
        }
    }

    /**
     * @return true if MTM_NET_MUREX pricer type is loaded
     */
    private boolean existMTMPricer(){
        return mtmPricerMeasureType!=-1 && mtmPricerMeasureType!=0;
    }

    /**
     * @return true if MARKETVALUEMAN pricer type is loaded
     */
    private boolean existMARKETVALUEMANPricer(){
        return marketValueManMeasureType!=-1 && marketValueManMeasureType!=0;
    }

    /**
     * @param trade
     * @return PRINCIAL BoCre
     */
    private HashMap<String,BOCre> getBOCresForCalculate(Trade trade){
        HashMap<String,BOCre> cres = new HashMap<>();
        if(Optional.ofNullable(trade).isPresent()){
            CreArray array = loadBOCres(trade);
            Arrays.stream(array.getCres()).forEach(cre -> {
                final String eventType = cre.getEventType();
                if(cres.containsKey(eventType)){
                    final BOCre boCre = cres.get(eventType);
                    if(boCre.getId()<cre.getId()){
                        cres.put(eventType,cre);
                    }
                }else {
                    cres.put(cre.getEventType(),cre);
                }
            });
        }
        return cres;
    }

    /**
     * Load last BoCres PRINCIPAL, for father repo trade.
     * @param trade
     * @return
     */
    private CreArray loadBOCres(Trade trade){
        String whereClause = buildWhereClause();
        List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(PRINCIPAL);
        CustomBindVariablesUtil.addNewBindVariableToList("NEW",bindVariables);
        CustomBindVariablesUtil.addNewBindVariableToList("SENT",bindVariables);
        CustomBindVariablesUtil.addNewBindVariableToList(trade.getLongId(),bindVariables);
        try {
            return DSConnection.getDefault().getRemoteBO().getBOCres("trade", whereClause, bindVariables);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading BoCres:" + e.getCause());
        }
        return new CreArray();
    }

    /**
     * @return
     */
    private String buildWhereClause(){
        final StringBuilder where = new StringBuilder();
        where.append(" bo_cre_type LIKE ? ");
        where.append(" AND ");
        where.append(" cre_status LIKE ? ");
        where.append(" AND ");
        where.append(" sent_status LIKE ? ");
        where.append(" AND ");
        where.append(" trade.trade_id = bo_cre.trade_id");
        where.append(" AND ");
        where.append(" trade.trade_id = ? ");
        //where.append(" ORDER BY  bo_cre_id DESC");
        return where.toString();
    }

    /**
     * @param value
     * @param percentage
     * @return
     */
    private Amount calculateProrate(Double value, Double percentage){
        return new Amount((value*percentage)/100);
    }

    /**
     * @param trade
     * @return
     */
    private String mapDirection(Trade trade){
        AtomicReference<String> direction = new AtomicReference<>("");
        if(trade.getProduct() instanceof Repo){
            if("Repo".equalsIgnoreCase(((Repo)trade.getProduct()).getDirection())){
                direction.set(PAY);
            }else {
                direction.set(RECEIVE);
            }
        }else if(trade.getQuantity() < 0.0D){
            direction.set(PAY);
        }else {
            direction.set(RECEIVE);
        }
        return direction.get();
    }

    /**
     * @param trade
     * @return
     */
    public Trade getFatherRepoTrade(Trade trade){
        if(Optional.ofNullable(trade).isPresent()){
            try {
                final Long repoTradeId = Optional.ofNullable(trade.getInternalReference()).map(Long::parseLong).orElse(0L);
                if(cachedFathersRepoTrades.containsKey(repoTradeId)){
                    return cachedFathersRepoTrades.get(repoTradeId);
                }else {
                    final Trade repoTrade = DSConnection.getDefault().getRemoteTrade().getTrade(repoTradeId);
                    cachedFathersRepoTrades.put(repoTradeId,repoTrade);
                    return repoTrade;
                }
            } catch (Exception e) {
                Log.error(this,"Error loading Trade: " + e.getCause());
            }
        }
        return null;
    }

    private JDatetime getValDateTime(ReportRow row){
        return Optional.ofNullable(row).map(r -> r.getProperty("ValuationDatetime")).filter(JDatetime.class::isInstance).map(JDatetime.class::cast).orElse(JDate.getNow().getJDatetime(TimeZone.getDefault()));
    }

    private List<ReportRow> sortRowsByTradeID(List<ReportRow> rows){
        if(!Util.isEmpty(rows)){
            rows.sort((o1, o2) -> {
                final long longId = null!=o1.getProperty(ReportRow.TRADE) ? ((Trade) o1.getProperty(ReportRow.TRADE)).getLongId() : 0L;
                final long longId1 = null!=o2.getProperty(ReportRow.TRADE) ? ((Trade) o2.getProperty(ReportRow.TRADE)).getLongId() : 0L;
                if(longId>longId1)
                    return 1;
                if(longId<longId1)
                    return -1;
                return 0;
            });
        }
        return rows;
    }

    public void clearCache(){
        cachedFathersRepoTrades.clear();
    }

}
