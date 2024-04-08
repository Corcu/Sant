package calypsox.tk.confirmation.builder.brs;

import calypsox.tk.confirmation.builder.CalConfirmationFinantialDataBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.*;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.refdata.FdnRateIndex;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class BRSConfirmationFinantialDataBuilder extends CalConfirmationFinantialDataBuilder {

    PerformanceSwap brs;

    private static final String SANTANDER_STR = "Santander";
    private static final String CPTY_STR="the Counterparty";

    private static final String MX_DIRTY_KWD="Mx Dirty Price";

    public BRSConfirmationFinantialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        Product tradeProduct = Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
        if (tradeProduct instanceof PerformanceSwap) {
            brs = (PerformanceSwap) tradeProduct;
            referenceSecurity = getBRSReferenceBond();
        }
    }



    public String buildRelevantJurisdiction() {
        int issuerId = Optional.ofNullable(referenceSecurity)
                .map(Security::getIssuerId)
                .orElse(0);
        return Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(), issuerId))
                .map(LegalEntity::getCountry).orElse("");
    }

    private Security getBRSReferenceBond() {
        Security sec = null;
        if (Optional.ofNullable(brs).map(PerformanceSwap::getReferenceProduct)
                .orElse(null) instanceof Security) {
            sec = (Security) brs.getReferenceProduct();
        }
        return sec;
    }

    public String buildFaceAmount() {
        return Optional.ofNullable(brs)
                .map(PerformanceSwap::getPrimaryLeg)
                .map(leg -> ((PerformanceSwapLeg) leg).getNotional())
                .map(this::formatNumber).orElse("0.0");
    }

    public String buildPriceFixingIndicator(){
       return Optional.ofNullable(brs)
                .map(PerformanceSwap::getPrimaryLeg)
                .map(leg -> ((PerformanceSwapLeg) leg).getFixingType())
                .filter(ft->FixingType.DIRTYPRICE.strValue.equals(ft)).map(ft -> FixingType.DIRTYPRICE.intValue)
                .orElse(FixingType.CLEANPRICE.intValue);
    }
    public String buildInitialPrice() {
        String initialPrice;
        if(FixingType.DIRTYPRICE.intValue.equals(buildPriceFixingIndicator())) {
           initialPrice=Optional.ofNullable(trade)
                    .map(t -> t.getKeywordValue(MX_DIRTY_KWD)).orElse("");
        }else{
            initialPrice=Optional.ofNullable(brs)
                    .map(PerformanceSwap::getPrimaryLeg)
                    .map(leg -> ((PerformanceSwapLeg) leg).getInitialPrice()*100.0D)
                    .map(number->String.format(Locale.ENGLISH, "%.8f", number)).orElse("");
        }
        return initialPrice;
    }

    public String buildFinalValuationDate() {
        String date=Optional.ofNullable(trade).map(trade -> trade.getKeywordValue("Mx_InterestValuationEndDate"))
                .orElse("");
        String[] splittedDate=date.split("-");
        if(!Util.isEmpty(splittedDate)&& splittedDate.length==3) {
            JDate jdate = JDate.valueOf(Integer.parseInt(splittedDate[0]), Integer.parseInt(splittedDate[1]), Integer.parseInt(splittedDate[2]));
            date=jdate.toString();
        }else{
            date=Optional.ofNullable(brs).map(PerformanceSwap::getMaturityDate).map(JDate::toString).orElse("");
        }
        return date;
    }


    public String buildTRPeriodPaymentDate() {
       /*int rollDay=Optional.ofNullable(brs)
                .map(PerformanceSwap::getPrimaryLeg)
                .map(leg -> ((PerformanceSwapLeg) leg).getIncomePmtSchedule())
                .map(ScheduleParams::getOffset).orElse(0);
        return NumberNames.getEnum(rollDay).getFormattedName();*/
        return NumberNames.TWO.getFormattedName();
    }

    public String buildCouponType() {
        String secLeg = Optional.ofNullable(brs)
                .map(PerformanceSwap::getSecondaryLeg)
                .map(leg -> ((SwapLeg) leg).getLegType())
                .orElse("");
        if(SwapLeg.LEG_TYPE_FLOATING.equals(secLeg)){
            secLeg="Floating";
        }
        return secLeg;
    }

    public String buildSpreadAdd() {
        String spreadRate= getRateFromSchedule();
        spreadRate=spreadRate.replace("-","Minus ");
        return  spreadRate.concat(" %");
    }

    public String buildFloatingRateOption(){
        return Optional.ofNullable(brs)
                .map(PerformanceSwap::getSecondaryLeg)
                .filter(leg->leg instanceof SwapLeg)
                .filter(leg->SwapLeg.LEG_TYPE_FLOATING.equals(((SwapLeg)leg).getLegType()))
                .map(leg-> ((SwapLeg) leg).getRateIndex())
                .map(this::formatRateIndexName)
                .orElse("Not applicable");
    }
    public String buildSwapRateIndexTenor(){
        return Optional.ofNullable(brs)
                .map(PerformanceSwap::getSecondaryLeg)
                .filter(leg->leg instanceof SwapLeg)
                .filter(leg->SwapLeg.LEG_TYPE_FLOATING.equals(((SwapLeg)leg).getLegType()))
                .map(leg-> ((SwapLeg) leg).getRateIndex())
                .map(ri -> ri.getTenor().getName())
                .orElse("");
    }

    /*private String formatTenor(String tenor){
        String formattedTenor=tenor;
        if(!Util.isEmpty(tenor)){
            formattedTenor=formattedTenor.replace("M"," month");
            if(formattedTenor.contains("Y")){
                formattedTenor=formattedTenor.replace("1Y","12 month");
                if(formattedTenor.equals("2Y")){
                    formattedTenor="24 month";
                }
            }else if(formattedTenor.contains("D")){
                formattedTenor=formattedTenor.replace("D"," day");
            }
            if(!(formattedTenor.contains("1 "))){
                formattedTenor=formattedTenor.concat("s");
            }
        }
        return formattedTenor;
    }*/

   private String getRateFromLeg(SwapLeg secondaryLeg){
       return Optional.ofNullable(secondaryLeg)
               .filter(leg -> SwapLeg.LEG_TYPE_FIXED.equals((leg).getLegType()))
               .map(leg -> (leg).getFixedRate()*100.0D).map(this::formatNumber)
               .orElse(getFloatRateFromLeg(secondaryLeg));
   }

    private String getFloatRateFromLeg(SwapLeg secondaryLeg){
        Optional<SwapLeg> secLegOpt=Optional.ofNullable(secondaryLeg);
        BigDecimal spreadBasisPoints= BigDecimal.valueOf(secLegOpt.map(SwapLeg::getSpread)
                    .map(spread->spread*100.0D).orElse(0.0D));
        return spreadBasisPoints.setScale(8, RoundingMode.HALF_EVEN).toString();
    }


    /**
     * @return If primary leg receives, the payer will be the CPTY. Otherwise PO's.
     */
    public String buildAmountPayer() {
        PerformanceSwappableLeg payerLeg = Optional.ofNullable(brs).map(PerformanceSwap::getPrimaryLeg).filter(leg -> leg instanceof PerformanceSwapLeg)
                .filter(leg -> ((PerformanceSwapLeg) leg).getNotional() < 0.0D).orElse(null);
        String payer = SANTANDER_STR;
        if (payerLeg == null) {
            payer = CPTY_STR;
        }
        return payer;
    }

    public String buildCalculationAgent() {
        return SANTANDER_STR;
    }

    public String buildFinalPriceDirection() {
        int buySell = Optional.ofNullable(brs).map(brs -> brs.getBuySell(trade)).orElse(0);
        if (buySell == -1) {
            buySell = 0;
        }
        return String.valueOf(buySell);
    }

    public String buildRateDayCountFraction() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getSecondaryLeg)
                .filter(leg -> leg instanceof SwapLeg)
                .map(leg -> ((SwapLeg) leg).getDayCount()).map(DayCount::toString).orElse("");
    }

    public String buildRatePayer() {
        String ratePayer = SANTANDER_STR;
        double quantity = Optional.ofNullable(trade).map(Trade::getQuantity).orElse(0.0D);
        if (quantity <= 0.0D) {
            ratePayer = CPTY_STR;
        }
        return ratePayer;
    }

    public String buildRollConvention() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getSecondaryLeg)
                .filter(leg -> leg instanceof SwapLeg)
                .map(leg -> ((SwapLeg) leg).getCouponDateRoll())
                .map(DateRoll::toString).map(RollDay::valueOf)
                .map(RollDay::getRollDay)
                .orElse("");
    }

    public String buildRollConventionMaturity() {
        return RollDay.MOD_FOLLOW.getRollDay();
    }

    public String buildFixPeriodPaymentDateComplete() {
        return findPeriodMonths(Optional.ofNullable(getSwapCouponFrequency())
                .map(Frequency::getTimesPerYr).orElse(0));
    }

    public String buildSwapCouponFreqCode(){
        return Optional.ofNullable(getSwapCouponFrequency())
                .map(Frequency::toString).orElse("");
    }

    private Frequency getSwapCouponFrequency(){
       return Optional.ofNullable(brs)
                .map(PerformanceSwap::getSecondaryLeg)
                .map(leg -> ((SwapLeg) leg).getCouponFrequency()).orElse(null);
    }
    public String buildFixPeriodPaymentDay() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getSecondaryLeg)
                .filter(leg -> leg instanceof SwapLeg)
                .map(leg -> ((SwapLeg) leg).getStartDate()).map(JDate::toString).orElse("");
    }

    public String buildPremiumType() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getPremiumType).orElse("");
    }

    public String buildPrimaryIncomePaymentType() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getPrimaryLeg)
                .filter(leg -> leg instanceof PerformanceSwapLeg)
                .map(leg -> ((PerformanceSwapLeg) leg).getIncomePmtType()).orElse("");
    }

    public String buildPrimaryLegConfig() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getPrimaryLegConfig)
                .orElse("");
    }

    public String buildSecondaryLegConfig() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getSecondaryLegConfig)
                .orElse("");
    }

    public String buildPrimaryReturnPaymentType() {
        return Optional.ofNullable(brs).map(PerformanceSwap::getPrimaryLeg)
                .filter(leg -> leg instanceof PerformanceSwapLeg)
                .map(leg -> ((PerformanceSwapLeg) leg).getReturnPmtType()).orElse("");
    }

    public String buildEarlyAmount(){
        String earlyAmount="0.0";
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList)
                .orElse(new Vector<>());
        for(Fee fee:fees){
            if("TERMINATION_FEE".equals(fee.getType())){
                earlyAmount=String.format(Locale.ENGLISH, "%.2f", fee.getAmount());
            }
        }
        return earlyAmount;
    }
    public String buildEarlyCurrency(){
        String earlyCurr="";
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList)
                .orElse(new Vector<>());
        for(Fee fee:fees){
            if("TERMINATION_FEE".equals(fee.getType())){
                earlyCurr=fee.getCurrency();
            }
        }
        return earlyCurr;
    }
    public String buildEarlySettleDate(){
        String formattedDate="";
        String termDate= Optional.ofNullable(trade)
                .map(trade -> trade.getKeywordValue("TerminationTradeDate"))
                .map(date->date.substring(0,date.indexOf(' ')))
                .orElse("");
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        try {
             formattedDate = JDate.valueOf(formatter.parse(termDate)).toString();
        } catch (ParseException e) {
           Log.warn(this.getClass().getSimpleName(),"Error parsing earlySettleDate field");
        }
        return formattedDate;
    }

    public String buildTerminationFeeDate(){
        String feeDate="";
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList)
                .orElse(new Vector<>());
        for(Fee fee:fees){
            if("TERMINATION_FEE".equals(fee.getType())){
                feeDate=fee.getFeeDate().toString();
            }
        }
        return feeDate;
    }

    public String buildSwapFirstIntPaymentDate(){
        PerformanceSwap performanceSwap= (PerformanceSwap) trade.getProduct();
        SwapLeg leg= (SwapLeg) performanceSwap.getSecondaryLeg();
        CashFlow firstFlow=Optional.ofNullable(leg.getFlows()).map(f->f.get(0)).orElse(null);
        String firstIntPayDate="";
        if(firstFlow instanceof CashFlowInterest){
           firstIntPayDate=firstFlow.getDate().toString();
        }
        return firstIntPayDate;
    }

    private String getCouponType(Bond bond) {
        String couponType;
        if (bond.isExotic()) {
            couponType = "Exotic";
        } else if (bond.isVariableCouponB()) {
            couponType = "Variable";
        } else if (bond.getFixedB()) {
            couponType = "Fixed";
        } else {
            couponType = !bond.getFixedB() ? "Floating" : "";
        }
        return couponType;
    }

    public String findPeriodMonths(int numberOfPaymentMonths){
        String[] months={"January","February","March","April","May","June","July","August","September",
        "October","November","December"};
        StringBuilder result=new StringBuilder();
        if(numberOfPaymentMonths>0){
            int monthStep=12/numberOfPaymentMonths;
             if(monthStep<1){
                monthStep=1;
            }
            int currentMonth=Optional.ofNullable(brs).map(PerformanceSwap::getSecondaryLeg)
                    .filter(leg -> leg instanceof SwapLeg)
                    .map(leg -> ((SwapLeg) leg).getStartDate()).map(JDate::getMonth).map(month -> month-1).orElse(0);
            result.append(months[currentMonth]);
            for(int i=0;i<numberOfPaymentMonths-1;i++){
                currentMonth=currentMonth+monthStep;
                if(currentMonth>11){
                    currentMonth=currentMonth-12;
                }
                if(i==numberOfPaymentMonths-2){
                    result.append(" and ");
                    result.append(months[currentMonth]);
                }else{
                    result.append(", ");
                    result.append(months[currentMonth]);
                }

            }
        }
        return result.toString();
    }

    private String getRateFromSchedule(){
        PerformanceSwap performanceSwap= (PerformanceSwap) trade.getProduct();
        SwapLeg leg= (SwapLeg) performanceSwap.getSecondaryLeg();
        return getRateFromPeriod(leg.getFlows());
    }

    private enum RollDay{
        MOD_FOLLOW("Modified Following"),
        NO_CHANGE("No Change"),
        PRECEDING("Preceding"),
        FOLLOWING("Following"),
        MOD_PRECED("Modified Preceding"),
        END_MONTH("End Month");

        String rollDay;

        RollDay(String rollDayStr){
            this.rollDay=rollDayStr;
        }

        String getRollDay(){
            return this.rollDay;
        }
    }

    private enum NumberNames{
        ZERO(0),
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        ELEVEN(11),
        TWELVE(12),
        THIRTEEN(13),
        FOURTEEN(14),
        FIFTEEN(15),
        SIXTEEN(16),
        SEVENTEEN(17),
        EIGHTEEN(18),
        NINETEEN(19),
        TWENTY(20);

        int numberValue;

        NumberNames(int number){
            this.numberValue=number;
        }

        String getFormattedName(){
            return StringUtils.capitalize(this.name().toLowerCase());
        }

        static NumberNames getEnum(int number) {
            for (NumberNames name : NumberNames.values()) {
                if (name.numberValue==number){
                   return name;
                }
            }
            return ZERO;
        }
    }

    private enum FixingType{

        DIRTYPRICE("0","DirtyPrice"),
        CLEANPRICE("1", "CleanPrice");
        String intValue;
        String strValue;
        FixingType(String intValue, String strValue){
            this.intValue=intValue;
            this.strValue=strValue;
        }
    }

    protected String formatRateIndexName(RateIndex rateIndex){
        String name=Optional.ofNullable(rateIndex).map(FdnRateIndex::getName).orElse("");
        String currency=Optional.ofNullable(rateIndex).map(FdnRateIndex::getCurrency).orElse("");
        return currency +
                " " + name;
    }
}
