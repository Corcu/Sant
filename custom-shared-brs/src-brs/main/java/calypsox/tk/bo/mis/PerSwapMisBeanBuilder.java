package calypsox.tk.bo.mis;

import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class PerSwapMisBeanBuilder {
    Trade trade;
    JDate proccesDate;

    private static final String FLOAT = "Float";


    public PerSwapMisBeanBuilder(Trade trade,JDate proccesDate) {
        this.trade = trade;
        this.proccesDate = proccesDate;
    }

    public List<PerSwapMisBean> build(PricingEnv env){
        List<PerSwapMisBean> beans = new ArrayList<>();

        if(Optional.ofNullable(this.trade).isPresent() && this.trade.getProduct() instanceof PerformanceSwap){
            PerSwapMisBean primaryLeg = new PerSwapMisBean();
            final PerformanceSwap product = (PerformanceSwap) this.trade.getProduct();
            final String legType = loadProductIntType(product.getReferenceProduct());

            final PLMark plMarkValue = getPLMarkValue(env, trade, proccesDate);
            final PerformanceSwapLeg productPrimaryLeg = (PerformanceSwapLeg) product.getPrimaryLeg();

            fillStatics(primaryLeg);
            fillCommonsValues(primaryLeg);

            primaryLeg.setIsin(product.getReferenceProduct().getSecCode("ISIN"));
            primaryLeg.setFodealfo(trade.getKeywordValue("MurexRootContract"));
            primaryLeg.setContractorigin(trade.getKeywordValue("MurexRootContract"));
            primaryLeg.setContractoriginbo(buildField(trade.getLongId()));
            primaryLeg.setDirection(loadPrimaryLegDirection(product));

            primaryLeg.setStrategyhedge(null!=trade.getBook().getAccountingBook() ? trade.getBook().getAccountingBook().getName() : "" ); // Accounting link del BOOK

            primaryLeg.setStrategyfront("");
            primaryLeg.setCurrency(product.getPrimaryLegCurrency());


            //COGER DEL CASHFLOW dentro del rango de fecha.
            //final CashFlowSet flows = primaryLeg1.getFlows(); //coger Flows
            
            double notional = UtilReport.getCashFlowNotional(productPrimaryLeg.getFlows(), proccesDate, CashFlow.INTEREST);
            
            primaryLeg.setPrincipalcur(getFormatAmount(product.getPrimaryLeg().getPrincipal()));
            primaryLeg.setRate(getFormatAmount(getRate(product.getPrimaryLeg())));
            primaryLeg.setInterestaccrual(buildField(trade.getAccrual())); //TODO
            primaryLeg.setPrincipalcash(getFormatAmount(notional == 0 ? product.getPrimaryLeg().getPrincipal() : notional));

            primaryLeg.setFiller1(getSingByDirection(primaryLeg.getDirection())+getFormatAmount(product.getPrimaryLeg().getPrincipal()).replace("-",""));

            primaryLeg.setCounterpartyeconomicgroup(getAttFromLE(trade.getCounterParty(),"RISK_SECTOR"));
            primaryLeg.setScope(getScope(trade));
            primaryLeg.setContractchannel("DIRECTO"); //TODO
            primaryLeg.setCurrentinteresttype(getIntType(legType)); //TODO

            if(FLOAT.equalsIgnoreCase(legType)){
                final RateIndex rateIndex = product.getReferenceProduct().getRateIndex();
                final double rateIndexSpread = ((Bond) product.getReferenceProduct()).getRateIndexSpread();

                primaryLeg.setSpread(getFormatSpread(rateIndexSpread));
                primaryLeg.setCouponindex(trade.getKeywordValue("MX_REFINDEX"));
                primaryLeg.setCouponcurrentterm(rateIndex.getTenor().getName());
                primaryLeg.setInterestfrequencyfixation(rateIndex.getTenor().getName());
                primaryLeg.setNextfixingdate(formatDate(UtilReport.getCashFlowDate(productPrimaryLeg, proccesDate, CashFlow.INTEREST, "ResetDate", true)));
            }

            final CashFlow nextInterestCash = getNextInterestCash(product.getPrimaryLeg().getFlows());
            if(null!=nextInterestCash && nextInterestCash.getAmount()!=0.0){
                primaryLeg.setNextinterestcash(getSingByDirection(primaryLeg.getDirection())+getFormatAmount(nextInterestCash.getAmount()).replace("-",""));
            }

//            final String amortStructure = ((Bond) product.getReferenceProduct()).getAmortStructure();
//            primaryLeg.setAmorttype(Util.isEmpty(amortStructure) ? "Bullet" : amortStructure);

            Security sec = productPrimaryLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                Bond bond = ((Bond) sec);
                if (bond.getAmortizingB() && "Schedule".equals(bond.getPrincipalStructure())) {
                    String amortFreq = ((Bond)product.getReferenceProduct()).getAmortFrequency().toString();
                    if(Util.isEmpty(amortFreq)){
                        primaryLeg.setAmorttype("CUSTOMIZED");
                    }
                    else {
                        primaryLeg.setAmorttype("PERIODICO");
                    }
                }
                else {
                    primaryLeg.setAmorttype("");
                }
            }

            if(isNoBullet(primaryLeg)){
                primaryLeg.setAmortfrequency(((Bond)product.getReferenceProduct()).getAmortFrequency().toString());
                List<NotionalDate> amortScheduled = ((Bond)product.getReferenceProduct()).getAmortSchedule();
                NotionalDate nextAmort = getNextAmort(amortScheduled, proccesDate);
                if(null!=nextAmort){
                    primaryLeg.setAmortnextdate(formatDate(nextAmort.getStartDate()));
                    primaryLeg.setNextnominalamort(getFormatAmount(nextAmort.getNotionalAmt()));
                }
            }

            primaryLeg.setStatus(trade.getStatus().getStatus());
            primaryLeg.setProduct_description(product.getReferenceProduct().getDescription());

            primaryLeg.setMtm(buildField(getPLMark(plMarkValue,"NPV_LEG1")));
            primaryLeg.setMarketvalueman(primaryLeg.getMtm());

            primaryLeg.setEntered_date(formatDate(trade.getEnteredDate().getJDate(TimeZone.getDefault())));
            primaryLeg.setEntered_user(trade.getEnteredUser());

            primaryLeg.setQuantity(buildField(trade.getQuantity()));
            primaryLeg.setTrade_price(buildField(trade.getTradePrice()));
            primaryLeg.setTrader(trade.getTraderName());
            primaryLeg.setOutstanding_notional(getFormatAmount(product.getNotional(trade.getSettleDate()))); //TODO
            primaryLeg.setRate_index_spread(buildField("")); //TODO
            primaryLeg.setUpfront_settlement_date(buildField(""));
            primaryLeg.setMaturity_date(formatDate(product.getMaturityDate())); //TODO
            primaryLeg.setRemain_maturity(formatDate(product.getFinalPaymentMaturityDate())); //TODO

            primaryLeg.setPrevious_fixing_date(buildField("")); //TODO
            primaryLeg.setFirst_cashflow_date(buildField("")); //TODO
            primaryLeg.setPerf_pmt_lag(buildField("")); //TODO
            primaryLeg.setPerf_pmt_lag_days(""); //TODO
            primaryLeg.setPerf_pmt_roll_day(""); //TODO

            final String compoundMethod = ((Bond) product.getReferenceProduct()).getCompoundMethod();
            final Frequency compoundFrequency = ((Bond) product.getReferenceProduct()).getCompoundFrequency();
            final DayCount dayCount = ((Bond) product.getReferenceProduct()).getDaycount();

            primaryLeg.setLeg_compounding_frequency(null != compoundFrequency ? compoundFrequency.toString() : "");
            //primaryLeg.setLeg_compounding_method(null != compoundFrequency ? compoundFrequency.toString() : "");
            primaryLeg.setLeg_compounding_method("");
            primaryLeg.setLeg_day_count(null!=dayCount ? dayCount.toString() : "");

            primaryLeg.setLeg_final_stub_idx_tenor("");
            primaryLeg.setLeg_final_stub_interpolation_required("");
            primaryLeg.setLeg_first_coupon_rate(buildField(0));
            primaryLeg.setLeg_first_pmt_date(buildField("")); //TODO
            primaryLeg.setLeg_first_stub_idx_tenor1("");
            primaryLeg.setLeg_first_stub_idx_tenor2("");
            primaryLeg.setLeg_fixed_rate("0");
            primaryLeg.setLeg_fixing_business_day("");
            primaryLeg.setLeg_floating_rate("0");
            primaryLeg.setLeg_holiday_calendars_income_pmt("");

            primaryLeg.setLeg_income_payment_type(((Bond) product.getReferenceProduct()).getPrePaidB() ? "PRE" : "POST");

            primaryLeg.setLeg_notional_adj_method(buildField(0));
            primaryLeg.setLeg_pay_holiday_calendar("");
            primaryLeg.setLeg_payment_day_roll("");
            primaryLeg.setLeg_payment_frequency("");
            primaryLeg.setLeg_payment_timing("");

            primaryLeg.setLeg_rate_reset_holiday_calendar("");
            primaryLeg.setLeg_rate_reset_lag(buildField(""));
            primaryLeg.setLeg_rate_reset_offset("");
            primaryLeg.setLeg_rate_reset_roll_convention("");
            primaryLeg.setLeg_reset_timing("");
            primaryLeg.setLeg_sample_frequency("");
            primaryLeg.setLeg_start_date("");
            primaryLeg.setLeg_stub_first_date("");
            primaryLeg.setLeg_stub_last_date("");
            primaryLeg.setLeg_stub_type("");

            primaryLeg.setLeg_type(legType);
            primaryLeg.setBranch(trade.getBook().getLegalEntity().getCode());
            primaryLeg.setLastfixingdate(formatDate(UtilReport.getCashFlowDate(productPrimaryLeg, proccesDate, CashFlow.INTEREST, "ResetDate", false)));
            primaryLeg.setFinancingcostporc("");
            primaryLeg.setAccruedpremiums("0.0");
            primaryLeg.setPendingaccruedpremiums("0.0");
            primaryLeg.setUnderlying(product.getReferenceProduct().getSecCode("ISIN"));

            primaryLeg.setCurrentRate(getCurrentRate(productPrimaryLeg, proccesDate));
            primaryLeg.setPayLegFirstPmtDate(getCashFlowDate(productPrimaryLeg, proccesDate, CashFlow.INTEREST, "Date", true));

            beans.add(primaryLeg);

            //Always create the second leg
            PerSwapMisBean secondaryLeg;
            try {
                secondaryLeg = (PerSwapMisBean) BeanUtils.cloneBean(primaryLeg);
                if(null!=secondaryLeg){
                    fillSecondLeg(product,secondaryLeg,plMarkValue);
                    beans.add(secondaryLeg);
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                    | NoSuchMethodException e) {
                Log.error("Cannot create the second direction of the position", e);

            }

        }

        return beans;
    }


    private void fillStatics(PerSwapMisBean bean){
        bean.setOrigin("CALYPSO");
        bean.setProcessdate(formatDate(proccesDate));

    }

    private void fillCommonsValues(PerSwapMisBean bean){
        bean.setOrigin("800018693");
        bean.setEntity(getAttFromLE(trade.getBook().getLegalEntity(),"ALIAS_ENTITY_GER"));
        bean.setBranch_id(buildField(trade.getBook().getLegalEntity().getId()));
        bean.setContractoriginbo(buildField(trade.getLongId()));
        bean.setDeal_id(buildField(trade.getLongId()));
        final String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
        bean.setAccounting_center(loadCenter(partenonAccountingID)); // partenon ID del trade
        bean.setPartenonid(partenonAccountingID);
        bean.setTradedate(formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault())));
        bean.setValuedate(formatDate(trade.getSettleDate()));
        bean.setMaturitydate(formatDate(trade.getMaturityDate()));
        bean.setInstrument("491-"+loadSubProduct(partenonAccountingID)); //TODO Agregar subtype
        bean.setInstrtype(trade.getProductType());
        bean.setGlscounterparty(trade.getCounterParty().getCode());
        bean.setCounterpartydesc(trade.getCounterParty().getExternalRef());
        bean.setCounterpartysector(getAttFromLE(trade.getCounterParty(),"RISK_SECTOR"));
        bean.setCounterpartycountry(getISOCode(trade.getCounterParty().getCountry()));
        bean.setPortfolio(trade.getBook().getName());
        bean.setFiller2(getInternalPortfolioMirror(trade));
    }

    private void fillSecondLeg(PerformanceSwap product,PerSwapMisBean secondaryLeg, PLMark plMark){
        SwapLeg secLeg = null;
        String legType = "";
        String compoundMethod = "";
        DayCount dayCount = null;
        Frequency compoundFrequency  = null;
        Vector holidays;

        if(product.getSecondaryLeg() instanceof SwapLeg){
            secLeg = (SwapLeg) product.getSecondaryLeg();
            legType = secLeg.getLegType();
            secLeg.getAmortAmount();
            compoundMethod = secLeg.getCompoundMethod().toString();
            compoundFrequency = secLeg.getCompoundFrequency();
            dayCount = secLeg.getDayCount();
            holidays = secLeg.getCouponHolidays();
            final Vector amortSchedule = secLeg.getAmortSchedule();

            String amortStructure = ((SwapLeg) product.getSecondaryLeg()).getPrincipalStructure();
            if (!Util.isEmpty(amortStructure) && "Bullet".equalsIgnoreCase(amortStructure)) {
                secondaryLeg.setAmorttype("");
            } else if (!Util.isEmpty(amortStructure) && "Schedule".equalsIgnoreCase(amortStructure)) {
                String amortFreq = secLeg.getAmortFrequency().toString();
                if(Util.isEmpty(amortFreq)){
                    secondaryLeg.setAmorttype("CUSTOMIZED");
                }
                else {
                    secondaryLeg.setAmorttype("PERIODICO");
                }
            }

            if(isNoBullet(secondaryLeg)){
                secondaryLeg.setAmortfrequency(secLeg.getAmortFrequency().toString());
                List<NotionalDate> amortScheduled = ((SwapLeg) product.getSecondaryLeg()).getAmortSchedule();
                NotionalDate nextAmort = getNextAmort(amortScheduled, proccesDate);
                if(null!=nextAmort){
                    secondaryLeg.setAmortnextdate(formatDate(nextAmort.getStartDate()));
                    secondaryLeg.setNextnominalamort(getFormatAmount(nextAmort.getNotionalAmt()));
                }
            }

            if ("BEG_PER".equalsIgnoreCase(secLeg.getResetTiming())) {
                secondaryLeg.setLeg_income_payment_type("PRE");
            } else if ("END_PER".equalsIgnoreCase(secLeg.getResetTiming())) {
                secondaryLeg.setLeg_income_payment_type("POST");
            }

        }

        //secLeg.getFre
        secondaryLeg.setInterestsettlementfrequency("");
        secondaryLeg.setMtm(buildField(getPLMark(plMark,"NPV_LEG2")));
        secondaryLeg.setMarketvalueman(secondaryLeg.getMtm());
        secondaryLeg.setIsin("");
        secondaryLeg.setStrategyfront("");
        secondaryLeg.setDirection(loadSecondaryLegDirection(secondaryLeg.getDirection()));
        //secondaryLeg.setMaturitydate("");
        //secondaryLeg.setMaturity_date("");
        secondaryLeg.setRemain_maturity("");


        if(FLOAT.equalsIgnoreCase(legType)){
            final RateIndex rateIndex = secLeg.getRateIndex();
            final double spread = secLeg.getSpread();
            secondaryLeg.setCouponindex(trade.getKeywordValue("MX_REFINDEX"));
            secondaryLeg.setCouponcurrentterm(rateIndex.getTenor().getName());
            secondaryLeg.setInterestfrequencyfixation(rateIndex.getTenor().getName());
            secondaryLeg.setCouponbasecalc(secLeg.getDayCount().toString());
            secondaryLeg.setSpread(getFormatSpread(spread));
            secondaryLeg.setNextfixingdate(formatDate(UtilReport.getCashFlowDate(secLeg, proccesDate, CashFlow.INTEREST, "ResetDate", true)));

        }else{
            secondaryLeg.setCouponindex("");
            secondaryLeg.setCouponcurrentterm("");
            secondaryLeg.setInterestfrequencyfixation("");
            secondaryLeg.setCouponbasecalc(secLeg.getDayCount().toString());
            secondaryLeg.setSpread(buildField(0.0));
            secondaryLeg.setNextfixingdate("");
        }

        final CashFlow nextInterestCash = getNextInterestCash(product.getSecondaryLegFlows());
        if(null!=nextInterestCash && nextInterestCash.getAmount()!=0.0){
            secondaryLeg.setNextinterestcash(getSingByDirection(secondaryLeg.getDirection())+getFormatAmount(nextInterestCash.getAmount()).replace("-",""));
        }
        
        double notional = UtilReport.getCashFlowNotional(secLeg.getFlows(), proccesDate, CashFlow.INTEREST);
        
        secondaryLeg.setCurrency(product.getSecondaryLegCurrency());
        secondaryLeg.setPrincipalcur(getFormatAmount(product.getSecondaryLeg().getPrincipal()));
        secondaryLeg.setRate(getFormatAmount(getRate(product.getSecondaryLeg())));
        secondaryLeg.setPrincipalcash(getFormatAmount(notional == 0 ? product.getSecondaryLeg().getPrincipal() : notional)); //TODO legs
        secondaryLeg.setFiller1(getSingByDirection(secondaryLeg.getDirection())+getFormatAmount(product.getSecondaryLeg().getPrincipal()).replace("-",""));

        secondaryLeg.setCurrentinteresttype(getIntType(legType)); //TODO

        secondaryLeg.setLeg_compounding_frequency(null != compoundFrequency ? compoundFrequency.toString() : "");
        //secondaryLeg.setLeg_compounding_method(null != compoundFrequency ? compoundFrequency.toString() : "");
        secondaryLeg.setLeg_compounding_method("");
        secondaryLeg.setLeg_day_count(null!=dayCount ? dayCount.toString() : "");

        secondaryLeg.setLeg_type(legType);
        secondaryLeg.setProduct_description(""); //leg
        secondaryLeg.setUnderlying("");

        secondaryLeg.setCurrentRate(getCurrentRate(secLeg, proccesDate));
        secondaryLeg.setPayLegFirstPmtDate(getCashFlowDate(secLeg, proccesDate, CashFlow.INTEREST, "Date", true));
        secondaryLeg.setLastfixingdate(formatDate(UtilReport.getCashFlowDate(secLeg, proccesDate, CashFlow.INTEREST, "ResetDate", false)));

    }

    public String getFormatAmount(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("###0.00");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(value);
        } else {
            return "";
        }
    }

    public String getFormatSpread(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("###0.000000");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(value);
        } else {
            return "";
        }
    }


    /**
     * Check Legs Currencies
     * @param product
     * @return
     */
    private boolean checkCurrencies(PerformanceSwap product){
        return !product.getPrimaryLegCurrency().equalsIgnoreCase(product.getSecondaryLegCurrency());
    }

    private PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
        PLMark plMark = null;
        if(null!=date && null!=pricingEnv){
            date = date.addBusinessDays(0,Util.string2Vector("SYSTEM"));
            try {
                plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(),
                        pricingEnv.getName(), date);
                return plMark;
            } catch (RemoteException e) {
                Log.error(this, e);
                return null;

            }
        }
        return null;
    }

    public String formatDate(JDate date){
        if(null!=date){
            JDateFormat format = new JDateFormat("yyyy-MM-dd");
            return format.format(date);
        }
        return "";
    }


    private String loadRate(){
        return "";
    }


    public String loadPrimaryLegDirection(PerformanceSwap product){
        String primayLegDesc = "";

        if(null!=trade && null!=product){
            PerformanceSwappableLeg primaryLeg = product.getPrimaryLeg();
            PerformanceSwapLeg primLeg = null;
            boolean perfLeg = false;

            if (primaryLeg instanceof PerformanceSwapLeg) {
                perfLeg = true;
                primLeg = (PerformanceSwapLeg)primaryLeg;
            }
            if (perfLeg) {
                if (primLeg.getNotional() < 0.0D) {
                    primayLegDesc = "B";
                } else if (primLeg.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() < 0.0D) {
                    primayLegDesc = "B";
                } else {
                    primayLegDesc = "S";
                }
            }
        }
        return primayLegDesc;
    }

    private String loadSecondaryLegDirection(String primaryDirection){
        if("B".equalsIgnoreCase(primaryDirection)){
            return "S";
        }else if("S".equalsIgnoreCase(primaryDirection)){
            return "B";
        }
        return "";
    }


    private String getIntType(String type){
        switch (type){
            case "Fixed":
                return "F";
            case "Float":
                return "V";
            default:
                return "";
        }
    }

    private String loadProductIntType(Product product){
        if(null!=product){
            if( product instanceof Bond){
                if(((Bond)product).getFixedB()){
                    return "Fixed";
                }else{
                    return "Float";
                }
            }
        }
        return "";
    }

    private Double getRate(PerformanceSwappableLeg product){
        if(null!=product){
            if(product instanceof SwapLeg){
                return ((SwapLeg)product).getFixedRate();
            }else  if(product instanceof PerformanceSwapLeg){
                return ((Bond)((PerformanceSwapLeg) product).getReferenceProduct()).getCouponRate(proccesDate);
            }
        }

        return 0.0D;
    }

    private String loadCenter(String value){
        if(!Util.isEmpty(value) && value.length()>=8){
            return value.substring(4,8);
        }
        return "";
    }

    private String loadSubProduct(String value){
        if(!Util.isEmpty(value) && value.length()>=18){
            return value.substring(18,value.length());
        }
        return "";
    }

    private String getPLMark(PLMark plMark, String type){
        double plmark = null != plMark && null != plMark.getPLMarkValueByName(type) ? plMark.getPLMarkValueByName(type).getMarkValue() : 0.0D;
        return getFormatAmount(plmark);
    }


    private String getAttFromLE(LegalEntity entity, String atttributeName){
        if(null!=entity && !Util.isEmpty(atttributeName)){
            Collection<LegalEntityAttribute> attributes = entity.getLegalEntityAttributes();
            if(!Util.isEmpty(attributes)){
                for(LegalEntityAttribute att : attributes){
                    if(att.getAttributeType().equalsIgnoreCase(atttributeName)){
                        return att.getAttributeValue();
                    }
                }
            }
        }
        return "";
    }

    private String getISOCode(String countryName){

        try {
            final Country country = BOCache.getCountry(DSConnection.getDefault(), countryName);
            if(null!=country){
                return country.getISOCode();
            }
        } catch (Exception e) {
            Log.error(this, "Error Extractin ISO Country from " + countryName + ": ", e);
        }
        return "";
    }

    private CashFlow getNextInterestCash(CashFlowSet flows){
        CashFlow intFlow = null;
        if(null!=flows){
            JDate date = null;
            for(CashFlow flow : flows.getFlows()){
                if(flow.getDate().after(proccesDate) && "INTEREST".equalsIgnoreCase(flow.getType())){
                    if(date!=null && flow.getDate().before(date)){
                        date = flow.getDate();
                        intFlow = flow;
                    }else if(date == null){
                        date = flow.getDate();
                        intFlow = flow;
                    }
                }
            }
        }
        return intFlow;
    }

    private NotionalDate getNextAmort( List<NotionalDate> amortScheduled,JDate proccesDate){
        NotionalDate current = null;
        if(!Util.isEmpty(amortScheduled)){
            for(NotionalDate date : amortScheduled){
                if(date.getStartDate().after(proccesDate) || date.getStartDate().equals(proccesDate)){
                    if(null!=current && date.getStartDate().before(current.getStartDate())){
                        current = date;
                    }else if(null==current){
                        current = date;
                    }
                }
            }
        }
        return current;
    }

    private String getScope(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "I" : "M";
    }

    private String buildField(Object t){
        return String.valueOf(t);
    }

    private String getSingByDirection(String direction){
        if("B".equalsIgnoreCase(direction)){
            return "-";
        }else if ("S".equalsIgnoreCase(direction)){
            return "+";
        }
        return "";
    }

    private boolean isNoBullet(PerSwapMisBean bean){
        return null!=bean && !Util.isEmpty(bean.getAmorttype()) && !bean.getAmorttype().equalsIgnoreCase("Bullet");
    }

    private String getCurrentRate(PerformanceSwappableLeg leg, JDate valDate){
        Rate currentRate = UtilReport.getCurrentRate(leg, valDate);
        if(currentRate!=null){
            return currentRate.toString();
        }
        return "";
    }

    private String getCashFlowDate(PerformanceSwappableLeg leg, JDate valDate, String flowType, String dateType, boolean next){
        return formatDate(UtilReport.getCashFlowDate(leg, valDate, CashFlow.INTEREST, "Date", true));
    }

    public String getInternalPortfolioMirror(Trade trade) {
        return trade.getMirrorBook() != null ? trade.getMirrorBook().toString() : "";
    }

}
