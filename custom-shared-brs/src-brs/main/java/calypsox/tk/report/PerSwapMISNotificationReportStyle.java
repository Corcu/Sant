package calypsox.tk.report;

import calypsox.tk.bo.mis.PerSwapMisBean;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;

public class PerSwapMISNotificationReportStyle extends TradeReportStyle {

    public static final String ORIGIN = "ORIGIN";
    public static final String PROCESSDATE = "PROCESSDATE";
    public static final String ENTITY = "ENTITY";
    public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
    public static final String BRANCH_ID = "BRANCH_ID";
    public static final String DEAL_ID = "DEAL_ID";
    public static final String PARTENONID = "PARTENONID";
    public static final String ISIN = "ISIN";
    public static final String TRADEDATE = "TRADEDATE";
    public static final String VALUEDATE = "VALUEDATE";
    public static final String MATURITYDATE = "MATURITYDATE";
    public static final String FODEALFO = "FODEALFO";
    public static final String CONTRACTORIGIN = "CONTRACTORIGIN";
    public static final String CONTRACTORIGINBO = "CONTRACTORIGINBO";
    public static final String INSTRUMENT = "INSTRUMENT";
    public static final String INSTRTYPE = "INSTRTYPE";
    public static final String DIRECTION = "DIRECTION";
    public static final String GLSCOUNTERPARTY = "GLSCOUNTERPARTY";
    public static final String COUNTERPARTYDESC = "COUNTERPARTYDESC";
    public static final String COUNTERPARTYSECTOR = "COUNTERPARTYSECTOR";
    public static final String COUNTERPARTYCOUNTRY = "COUNTERPARTYCOUNTRY";
    public static final String STRATEGYHEDGE = "STRATEGYHEDGE";
    public static final String STRATEGYFRONT = "STRATEGYFRONT";
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String CURRENCY = "CURRENCY";
    public static final String PRINCIPALCUR = "PRINCIPALCUR";
    public static final String RATE = "RATE";
    public static final String INTERESTACCRUAL = "INTERESTACCRUAL";
    public static final String MARKETVALUEMAN = "MARKETVALUEMAN";
    public static final String PRINCIPALCASH = "PRINCIPALCASH";
    public static final String SPREAD = "SPREAD";
    public static final String COUNTERPARTYECONOMICGROUP = "COUNTERPARTYECONOMICGROUP";
    public static final String SCOPE = "SCOPE";
    public static final String CONTRACTCHANNEL = "CONTRACTCHANNEL";
    public static final String CURRENTINTERESTTYPE = "CURRENTINTERESTTYPE";
    public static final String COUPONINDEX = "COUPONINDEX";
    public static final String COUPONCURRENTTERM = "COUPONCURRENTTERM";
    public static final String COUPONBASECALC = "COUPONBASECALC";
    public static final String NEXTFIXINGDATE = "NEXTFIXINGDATE";
    public static final String INTERESTSETTLEMENTFREQUENCY = "INTERESTSETTLEMENTFREQUENCY";
    public static final String INTERESTFREQUENCYFIXATION = "INTERESTFREQUENCYFIXATION";
    public static final String FUNDINGNEXTFIXINGRATE = "FUNDINGNEXTFIXINGRATE";
    public static final String AMORTTYPE = "AMORTTYPE";
    public static final String AMORTFREQUENCY = "AMORTFREQUENCY";
    public static final String AMORTNEXTDATE = "AMORTNEXTDATE";
    public static final String NEXTINTERESTCASH = "NEXTINTERESTCASH";
    public static final String NEXTNOMINALAMORT = "NEXTNOMINALAMORT";
    public static final String STATUS = "STATUS";
    public static final String SETTLECURRCOUPON = "SETTLECURRCOUPON";
    public static final String SETTLECURRPERFORMANCE = "SETTLECURRPERFORMANCE";
    public static final String PRODUCT_DESCRIPTION = "PRODUCT_DESCRIPTION";
    public static final String MTM = "MTM";
    public static final String ENTERED_DATE = "ENTERED_DATE";
    public static final String ENTERED_USER = "ENTERED_USER";
    public static final String QUANTITY = "QUANTITY";
    public static final String TRADE_PRICE = "TRADE_PRICE";
    public static final String TRADER = "TRADER";
    public static final String OUTSTANDING_NOTIONAL = "OUTSTANDING_NOTIONAL";
    public static final String RATE_INDEX_SPREAD = "RATE_INDEX_SPREAD";
    public static final String UPFRONT_SETTLEMENT_DATE = "UPFRONT_SETTLEMENT_DATE";
    public static final String MATURITY_DATE = "MATURITY_DATE";
    public static final String REMAIN_MATURITY = "REMAIN_MATURITY";
    public static final String PREVIOUS_FIXING_DATE = "PREVIOUS_FIXING_DATE";
    public static final String FIRST_CASHFLOW_DATE = "FIRST_CASHFLOW_DATE";
    public static final String PERF_PMT_LAG = "PERF_PMT_LAG";
    public static final String PERF_PMT_LAG_DAYS = "PERF_PMT_LAG_DAYS";
    public static final String PERF_PMT_ROLL_DAY = "PERF_PMT_ROLL_DAY";
    public static final String LEG_COMPOUNDING_FREQUENCY = "LEG_COMPOUNDING_FREQUENCY";
    public static final String LEG_COMPOUNDING_METHOD = "LEG_COMPOUNDING_METHOD";
    public static final String LEG_DAY_COUNT = "LEG_DAY_COUNT";
    public static final String LEG_FINAL_STUB_IDX_TENOR = "LEG_FINAL_STUB_IDX_TENOR";
    public static final String LEG_FINAL_STUB_INTERPOLATION_REQUIRED = "LEG_FINAL_STUB_INTERPOLATION_REQUIRED";
    public static final String LEG_FIRST_COUPON_RATE = "LEG_FIRST_COUPON_RATE";
    public static final String LEG_FIRST_PMT_DATE = "LEG_FIRST_PMT_DATE";
    public static final String LEG_FIRST_STUB_IDX_TENOR1 = "LEG_FIRST_STUB_IDX_TENOR1";
    public static final String LEG_FIRST_STUB_IDX_TENOR2 = "LEG_FIRST_STUB_IDX_TENOR2";
    public static final String LEG_FIXED_RATE = "LEG_FIXED_RATE";
    public static final String LEG_FIXING_BUSINESS_DAY = "LEG_FIXING_BUSINESS_DAY";
    public static final String LEG_FLOATING_RATE = "LEG_FLOATING_RATE";
    public static final String LEG_HOLIDAY_CALENDARS_INCOME_PMT = "LEG_HOLIDAY_CALENDARS_INCOME_PMT";
    public static final String LEG_INCOME_PAYMENT_TYPE = "LEG_INCOME_PAYMENT_TYPE";
    public static final String LEG_INITIAL_STUB_IDX_TENOR = "LEG_INITIAL_STUB_IDX_TENOR";
    public static final String LEG_INITIAL_STUB_INTERPOLATION_REQUIRED = "LEG_INITIAL_STUB_INTERPOLATION_REQUIRED";
    public static final String LEG_LAST_STUB_IDX_TENOR1 = "LEG_LAST_STUB_IDX_TENOR1";
    public static final String LEG_LAST_STUB_IDX_TENOR2 = "LEG_LAST_STUB_IDX_TENOR2";
    public static final String LEG_NOTIONAL_ADJ_METHOD = "LEG_NOTIONAL_ADJ_METHOD";
    public static final String LEG_PAY_HOLIDAY_CALENDAR = "LEG_PAY_HOLIDAY_CALENDAR";
    public static final String LEG_PAYMENT_DAY_ROLL = "LEG_PAYMENT_DAY_ROLL";
    public static final String LEG_PAYMENT_FREQUENCY = "LEG_PAYMENT_FREQUENCY";
    public static final String LEG_PAYMENT_TIMING = "LEG_PAYMENT_TIMING";
    public static final String LEG_RATE_RESET_HOLIDAY_CALENDAR = "LEG_RATE_RESET_HOLIDAY_CALENDAR";
    public static final String LEG_RATE_RESET_LAG = "LEG_RATE_RESET_LAG";
    public static final String LEG_RATE_RESET_OFFSET = "LEG_RATE_RESET_OFFSET";
    public static final String LEG_RATE_RESET_ROLL_CONVENTION = "LEG_RATE_RESET_ROLL_CONVENTION";
    public static final String LEG_RESET_TIMING = "LEG_RESET_TIMING";
    public static final String LEG_SAMPLE_FREQUENCY = "LEG_SAMPLE_FREQUENCY";
    public static final String LEG_START_DATE = "LEG_START_DATE";
    public static final String LEG_STUB_FIRST_DATE = "LEG_STUB_FIRST_DATE";
    public static final String LEG_STUB_LAST_DATE = "LEG_STUB_LAST_DATE";
    public static final String LEG_STUB_TYPE = "LEG_STUB_TYPE";
    public static final String LEG_TYPE = "LEG_TYPE";

    public static final String BRANCH = "BRANCH";
    public static final String LASTFIXINGDATE = "LASTFIXINGDATE";
    public static final String FINANCINGCOSTPORC = "FINANCINGCOSTPORC";
    public static final String ACCRUEDPREMIUMS = "ACCRUEDPREMIUMS";
    public static final String PENDINGACCRUEDPREMIUMS = "PENDINGACCRUEDPREMIUMS";
    public static final String UNDERLYING = "UNDERLYING";

    public static final String PAY_LEG_FIRST_PMT_DATE = "Pay Leg First Pmt Date";
    public static final String NEXT_FIXING_DATE = "Next Fixing Date";

    public static final String FILLER1 = "FILLER1";
    public static final String FILLER2 = "FILLER2";
    public static final String FILLER3 = "FILLER3";
    public static final String FILLER4 = "FILLER4";
    public static final String FILLER5 = "FILLER5";

    public static final String[] ADDITIONAL_COLUMNS = {ORIGIN,PROCESSDATE,ENTITY,ACCOUNTING_CENTER,BRANCH_ID,DEAL_ID,PARTENONID,ISIN,TRADEDATE,VALUEDATE,MATURITYDATE,
            FODEALFO,CONTRACTORIGIN,CONTRACTORIGINBO,INSTRUMENT,INSTRTYPE,DIRECTION,GLSCOUNTERPARTY,COUNTERPARTYDESC,COUNTERPARTYSECTOR,COUNTERPARTYCOUNTRY,STRATEGYHEDGE,
            STRATEGYFRONT,PORTFOLIO,CURRENCY,PRINCIPALCUR,RATE,INTERESTACCRUAL,MARKETVALUEMAN,PRINCIPALCASH,SPREAD,COUNTERPARTYECONOMICGROUP,SCOPE,CONTRACTCHANNEL,
            CURRENTINTERESTTYPE,COUPONINDEX,COUPONCURRENTTERM,COUPONBASECALC,NEXTFIXINGDATE,INTERESTSETTLEMENTFREQUENCY,INTERESTFREQUENCYFIXATION,FUNDINGNEXTFIXINGRATE,
            AMORTTYPE,AMORTFREQUENCY,AMORTNEXTDATE,NEXTINTERESTCASH,NEXTNOMINALAMORT,STATUS,SETTLECURRCOUPON,SETTLECURRPERFORMANCE,PRODUCT_DESCRIPTION,MTM,ENTERED_DATE,ENTERED_USER,
            QUANTITY,TRADE_PRICE,TRADER,OUTSTANDING_NOTIONAL,RATE_INDEX_SPREAD,UPFRONT_SETTLEMENT_DATE,MATURITY_DATE,REMAIN_MATURITY,PREVIOUS_FIXING_DATE,FIRST_CASHFLOW_DATE,
            PERF_PMT_LAG,PERF_PMT_LAG_DAYS,PERF_PMT_ROLL_DAY,LEG_COMPOUNDING_FREQUENCY,LEG_COMPOUNDING_METHOD,LEG_DAY_COUNT,LEG_FINAL_STUB_IDX_TENOR,LEG_FINAL_STUB_INTERPOLATION_REQUIRED,
            LEG_FIRST_COUPON_RATE,LEG_FIRST_PMT_DATE,LEG_FIRST_STUB_IDX_TENOR1,LEG_FIRST_STUB_IDX_TENOR2,LEG_FIXED_RATE,LEG_FIXING_BUSINESS_DAY,LEG_FLOATING_RATE,
            LEG_HOLIDAY_CALENDARS_INCOME_PMT,LEG_INCOME_PAYMENT_TYPE,LEG_INITIAL_STUB_IDX_TENOR,LEG_INITIAL_STUB_INTERPOLATION_REQUIRED,LEG_LAST_STUB_IDX_TENOR1,LEG_LAST_STUB_IDX_TENOR2,
            LEG_NOTIONAL_ADJ_METHOD,LEG_PAY_HOLIDAY_CALENDAR,LEG_PAYMENT_DAY_ROLL,LEG_PAYMENT_FREQUENCY,LEG_PAYMENT_TIMING,LEG_RATE_RESET_HOLIDAY_CALENDAR,LEG_RATE_RESET_LAG,
            LEG_RATE_RESET_OFFSET,LEG_RATE_RESET_ROLL_CONVENTION,LEG_RESET_TIMING,LEG_SAMPLE_FREQUENCY,LEG_START_DATE,LEG_STUB_FIRST_DATE,LEG_STUB_LAST_DATE,LEG_STUB_TYPE,LEG_TYPE
            ,BRANCH,LASTFIXINGDATE,FINANCINGCOSTPORC,ACCRUEDPREMIUMS,PENDINGACCRUEDPREMIUMS,UNDERLYING,FILLER1,FILLER2,FILLER3,FILLER4,FILLER5};

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        Optional<PerSwapMisBean> beanOpt = Optional.ofNullable(row.getProperty("PerSwapMisBean"));
        row.setProperty("Default",beanOpt.get().getTrade());

        if(beanOpt.isPresent()){
            PerSwapMisBean bean = beanOpt.get();

            if(ORIGIN.equalsIgnoreCase(columnId)){
                return bean.getOrigin();

            }else if(PROCESSDATE.equalsIgnoreCase(columnId)){
                return bean.getProcessdate();

            }else if(ENTITY.equalsIgnoreCase(columnId)){
                return bean.getEntity();

            }else if(ACCOUNTING_CENTER.equalsIgnoreCase(columnId)){
                return bean.getAccounting_center();

            }else if(BRANCH_ID.equalsIgnoreCase(columnId)){
                return bean.getBranch_id();

            }else if(DEAL_ID.equalsIgnoreCase(columnId)){
                return bean.getDeal_id();

            }else if(PARTENONID.equalsIgnoreCase(columnId)){
                return bean.getPartenonid();

            }else if(ISIN.equalsIgnoreCase(columnId)){
                return bean.getIsin();

            }else if(TRADEDATE.equalsIgnoreCase(columnId)){ return bean.getTradedate();

            }else if(VALUEDATE.equalsIgnoreCase(columnId)){ return bean.getValuedate();

            }else if(MATURITYDATE.equalsIgnoreCase(columnId)){ return  bean.getMaturitydate();

            }else if(FODEALFO.equalsIgnoreCase(columnId)){ return bean.getFodealfo();

            }else if(CONTRACTORIGIN.equalsIgnoreCase(columnId)){ return bean.getContractorigin();

            }else if(CONTRACTORIGINBO.equalsIgnoreCase(columnId)){ return bean.getContractoriginbo();

            }else if(INSTRUMENT.equalsIgnoreCase(columnId)){ return bean.getInstrument();

            }else if(INSTRTYPE.equalsIgnoreCase(columnId)){ return bean.getInstrtype();

            }else if(DIRECTION.equalsIgnoreCase(columnId)){ return bean.getDirection();

            }else if(GLSCOUNTERPARTY.equalsIgnoreCase(columnId)){ return bean.getGlscounterparty();

            }else if(COUNTERPARTYDESC.equalsIgnoreCase(columnId)){ return bean.getCounterpartydesc();

            }else if(COUNTERPARTYSECTOR.equalsIgnoreCase(columnId)){ return bean.getCounterpartysector();

            }else if(COUNTERPARTYCOUNTRY.equalsIgnoreCase(columnId)){ return bean.getCounterpartycountry();

            }else if(STRATEGYHEDGE.equalsIgnoreCase(columnId)){ return bean.getStrategyhedge();

            }else if(STRATEGYFRONT.equalsIgnoreCase(columnId)){ return bean.getStrategyfront();

            }else if(PORTFOLIO.equalsIgnoreCase(columnId)){ return bean.getPortfolio();

            }else if(CURRENCY.equalsIgnoreCase(columnId)){ return bean.getCurrency();

            }else if(PRINCIPALCUR.equalsIgnoreCase(columnId)){ return bean.getPrincipalcur();

            }else if(RATE.equalsIgnoreCase(columnId)){
                return (bean.getCurrentRate()).replace(",",".");
                /**
                 final Object pay_leg_fixed_rate = getSColumnValue(bean, row, "Pay Leg Fixed Rate", errors);
                 String result = "";
                 if(pay_leg_fixed_rate instanceof Rate){
                 final String rateValue = ((Rate) pay_leg_fixed_rate).toString();
                 if(rateValue.contains(",")){
                 result = rateValue.replace(",",".");
                 }
                 }else {
                 return pay_leg_fixed_rate;
                 }
                 return result;
                 */
            }else if(INTERESTACCRUAL.equalsIgnoreCase(columnId)){ return bean.getInterestaccrual();

            }else if(MARKETVALUEMAN.equalsIgnoreCase(columnId)){ return bean.getMarketvalueman();

            }else if(PRINCIPALCASH.equalsIgnoreCase(columnId)){ return bean.getPrincipalcash();

            }else if(SPREAD.equalsIgnoreCase(columnId)){ return bean.getSpread();

            }else if(COUNTERPARTYECONOMICGROUP.equalsIgnoreCase(columnId)){ return bean.getCounterpartyeconomicgroup();

            }else if(SCOPE.equalsIgnoreCase(columnId)){ return bean.getScope();

            }else if(CONTRACTCHANNEL.equalsIgnoreCase(columnId)){ return bean.getContractchannel();

            }else if(CURRENTINTERESTTYPE.equalsIgnoreCase(columnId)){ return bean.getCurrentinteresttype();

            }else if(COUPONINDEX.equalsIgnoreCase(columnId)){ return bean.getCouponindex();

            }else if(COUPONCURRENTTERM.equalsIgnoreCase(columnId)){ return bean.getCouponcurrentterm();

            }else if(COUPONBASECALC.equalsIgnoreCase(columnId)){ return bean.getCouponbasecalc();

            }else if(NEXTFIXINGDATE.equalsIgnoreCase(columnId)){ return bean.getNextfixingdate();

            }else if(INTERESTSETTLEMENTFREQUENCY.equalsIgnoreCase(columnId)){
                return getSColumnValue(bean,row,"Pay Leg Payment Frequency",errors);

            }else if(INTERESTFREQUENCYFIXATION.equalsIgnoreCase(columnId)){ return bean.getInterestfrequencyfixation();

            }else if(FUNDINGNEXTFIXINGRATE.equalsIgnoreCase(columnId)){ return bean.getFundingnextfixingrate();

            }else if(AMORTTYPE.equalsIgnoreCase(columnId)){ return bean.getAmorttype();

            }else if(AMORTFREQUENCY.equalsIgnoreCase(columnId)){ return bean.getAmortfrequency();

            }else if(AMORTNEXTDATE.equalsIgnoreCase(columnId)){ return bean.getAmortnextdate();

            }else if(NEXTINTERESTCASH.equalsIgnoreCase(columnId)){ return bean.getNextinterestcash();

            }else if(NEXTNOMINALAMORT.equalsIgnoreCase(columnId)){ return bean.getNextnominalamort();

            }else if(STATUS.equalsIgnoreCase(columnId)){ return bean.getStatus();

            }else if(SETTLECURRCOUPON.equalsIgnoreCase(columnId)){ return bean.getSettlecurrcoupon();

            }else if(SETTLECURRPERFORMANCE.equalsIgnoreCase(columnId)){ return bean.getSettlecurrperformance();

            }else if(PRODUCT_DESCRIPTION.equalsIgnoreCase(columnId)){ return bean.getProduct_description();

            }else if(MTM.equalsIgnoreCase(columnId)){ return bean.getMtm();

            }else if(ENTERED_DATE.equalsIgnoreCase(columnId)){ return bean.getEntered_date();

            }else if(ENTERED_USER.equalsIgnoreCase(columnId)){ return bean.getEntered_user();

            }else if(QUANTITY.equalsIgnoreCase(columnId)){ return bean.getQuantity();

            }else if(TRADE_PRICE.equalsIgnoreCase(columnId)){ return bean.getTrade_price();

            }else if(TRADER.equalsIgnoreCase(columnId)){ return bean.getTrader();

            }else if(OUTSTANDING_NOTIONAL.equalsIgnoreCase(columnId)){ return bean.getOutstanding_notional();

            }else if(RATE_INDEX_SPREAD.equalsIgnoreCase(columnId)){ return bean.getRate_index_spread();

            }else if(UPFRONT_SETTLEMENT_DATE.equalsIgnoreCase(columnId)){ return bean.getUpfront_settlement_date();

            }else if(MATURITY_DATE.equalsIgnoreCase(columnId)){
                return bean.getMaturity_date();

            }else if(REMAIN_MATURITY.equalsIgnoreCase(columnId)){
                return bean.getRemain_maturity();

            }else if(PREVIOUS_FIXING_DATE.equalsIgnoreCase(columnId)){
                return bean.getPrevious_fixing_date();

            }else if(FIRST_CASHFLOW_DATE.equalsIgnoreCase(columnId)){
                return bean.getFirst_cashflow_date();

            }else if(PERF_PMT_LAG.equalsIgnoreCase(columnId)){
                return bean.getPerf_pmt_lag();

            }else if(PERF_PMT_LAG_DAYS.equalsIgnoreCase(columnId)){
                return bean.getPerf_pmt_lag_days();

            }else if(PERF_PMT_ROLL_DAY.equalsIgnoreCase(columnId)){
                return bean.getPerf_pmt_roll_day();

            }else if(LEG_COMPOUNDING_FREQUENCY.equalsIgnoreCase(columnId)){
                return bean.getLeg_compounding_frequency();

            }else if(LEG_COMPOUNDING_METHOD.equalsIgnoreCase(columnId)){
                return bean.getLeg_compounding_method();

            }else if(LEG_DAY_COUNT.equalsIgnoreCase(columnId)){
                return bean.getLeg_day_count();

            }else if(LEG_FINAL_STUB_IDX_TENOR.equalsIgnoreCase(columnId)){
                return bean.getLeg_final_stub_idx_tenor();

            }else if(LEG_FINAL_STUB_INTERPOLATION_REQUIRED.equalsIgnoreCase(columnId)){
                return bean.getLeg_final_stub_interpolation_required();

            }else if(LEG_FIRST_COUPON_RATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_first_coupon_rate();

            }else if(LEG_FIRST_PMT_DATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_first_pmt_date();

            }else if(LEG_FIRST_STUB_IDX_TENOR1.equalsIgnoreCase(columnId)){
                return bean.getLeg_first_stub_idx_tenor1();

            }else if(LEG_FIRST_STUB_IDX_TENOR2.equalsIgnoreCase(columnId)){
                return bean.getLeg_first_stub_idx_tenor2();

            }else if(LEG_FIXED_RATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_fixed_rate();

            }else if(LEG_FIXING_BUSINESS_DAY.equalsIgnoreCase(columnId)){
                return bean.getLeg_fixing_business_day();

            }else if(LEG_FLOATING_RATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_floating_rate();

            }else if(LEG_HOLIDAY_CALENDARS_INCOME_PMT.equalsIgnoreCase(columnId)){
                return bean.getLeg_holiday_calendars_income_pmt();

            }else if(LEG_INCOME_PAYMENT_TYPE.equalsIgnoreCase(columnId)){
                return bean.getLeg_income_payment_type();

            }else if(LEG_INITIAL_STUB_IDX_TENOR.equalsIgnoreCase(columnId)){
                return bean.getLeg_initial_stub_idx_tenor();

            }else if(LEG_INITIAL_STUB_INTERPOLATION_REQUIRED.equalsIgnoreCase(columnId)){
                return bean.getLeg_initial_stub_interpolation_required();

            }else if(LEG_LAST_STUB_IDX_TENOR1.equalsIgnoreCase(columnId)){
                return bean.getLeg_last_stub_idx_tenor1();

            }else if(LEG_LAST_STUB_IDX_TENOR2.equalsIgnoreCase(columnId)){
                return bean.getLeg_last_stub_idx_tenor2();

            }else if(LEG_NOTIONAL_ADJ_METHOD.equalsIgnoreCase(columnId)){
                return bean.getLeg_notional_adj_method();

            }else if(LEG_PAY_HOLIDAY_CALENDAR.equalsIgnoreCase(columnId)){
                return bean.getLeg_pay_holiday_calendar();

            }else if(LEG_PAYMENT_DAY_ROLL.equalsIgnoreCase(columnId)){
                return bean.getLeg_payment_day_roll();

            }else if(LEG_PAYMENT_FREQUENCY.equalsIgnoreCase(columnId)){
                return bean.getLeg_payment_frequency();

            }else if(LEG_PAYMENT_TIMING.equalsIgnoreCase(columnId)){
                return bean.getLeg_payment_timing();

            }else if(LEG_RATE_RESET_HOLIDAY_CALENDAR.equalsIgnoreCase(columnId)){
                return bean.getLeg_rate_reset_holiday_calendar();

            }else if(LEG_RATE_RESET_LAG.equalsIgnoreCase(columnId)){
                return bean.getLeg_rate_reset_lag();

            }else if(LEG_RATE_RESET_OFFSET.equalsIgnoreCase(columnId)){
                return bean.getLeg_rate_reset_offset();

            }else if(LEG_RATE_RESET_ROLL_CONVENTION.equalsIgnoreCase(columnId)){
                return bean.getLeg_rate_reset_roll_convention();

            }else if(LEG_RESET_TIMING.equalsIgnoreCase(columnId)){
                return bean.getLeg_reset_timing();

            }else if(LEG_SAMPLE_FREQUENCY.equalsIgnoreCase(columnId)){
                return bean.getLeg_sample_frequency();

            }else if(LEG_START_DATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_start_date();

            }else if(LEG_STUB_FIRST_DATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_stub_first_date();

            }else if(LEG_STUB_LAST_DATE.equalsIgnoreCase(columnId)){
                return bean.getLeg_stub_last_date();

            }else if(LEG_STUB_TYPE.equalsIgnoreCase(columnId)){
                return bean.getLeg_stub_type();

            }else if(LEG_TYPE.equalsIgnoreCase(columnId)){
                return bean.getLeg_type();
            }else if(BRANCH.equalsIgnoreCase(columnId)){
                return bean.getBranch();
            }else if(LASTFIXINGDATE.equalsIgnoreCase(columnId)){
                return bean.getLastfixingdate();
            }else if(FINANCINGCOSTPORC.equalsIgnoreCase(columnId)){
                return bean.getFinancingcostporc();
            }else if(ACCRUEDPREMIUMS.equalsIgnoreCase(columnId)){
                return bean.getAccruedpremiums();
            }else if(PENDINGACCRUEDPREMIUMS.equalsIgnoreCase(columnId)){
                return bean.getPendingaccruedpremiums();
            }else if(UNDERLYING.equalsIgnoreCase(columnId)){
                return bean.getUnderlying();
            }else if(PAY_LEG_FIRST_PMT_DATE.equalsIgnoreCase(columnId)){
                return bean.getPayLegFirstPmtDate();
            } else if(NEXT_FIXING_DATE.equalsIgnoreCase(columnId)) {
                return bean.getNextfixingdate();
            } else if(FILLER1.equalsIgnoreCase(columnId)){
                return bean.getFiller1();
            }else if(FILLER2.equalsIgnoreCase(columnId)){
                return bean.getFiller2();
            }else if(FILLER3.equalsIgnoreCase(columnId)){
                return bean.getFiller3();
            }else if(FILLER4.equalsIgnoreCase(columnId)){
                return bean.getFiller4();
            }else if(FILLER5.equalsIgnoreCase(columnId)){
                return bean.getFiller5();
            }else {
                Object columnValue = getSColumnValue(bean,row, columnId, errors);
                if (columnValue instanceof Rate){
                    final String rateValue = ((Rate) columnValue).toString();
                    if(rateValue.contains(",")){
                        return rateValue.replace(",",".");
                    }

                }
                return columnValue;
            }
        }

        return null;
    }


    private Object getSColumnValue( PerSwapMisBean bean,ReportRow row, String columnId, Vector errors){
        String substring = "";
        if("S".equalsIgnoreCase(bean.getDirection())){
            if(!Util.isEmpty(columnId) && columnId.contains("Pay")){
                substring = columnId.substring(3, columnId.length());
                columnId = "Rec" + substring;
            }
        }

        row.setProperty("Trade", bean.getTrade());
        Object columnValue = super.getColumnValue(row, columnId, errors);
        if(null == columnValue && columnId.contains("Rec")){
            substring = columnId.substring(3, columnId.length());
            columnId = "Rcv" + substring;
            columnValue = super.getColumnValue(row, columnId, errors);
        }

        return columnValue;
    }
}
