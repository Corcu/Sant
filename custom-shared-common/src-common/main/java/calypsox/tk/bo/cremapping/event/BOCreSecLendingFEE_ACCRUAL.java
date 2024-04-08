package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.Vector;

public class BOCreSecLendingFEE_ACCRUAL extends SantBOCre {

    public BOCreSecLendingFEE_ACCRUAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    JDate start = null;
    JDate end = null;

    @Override
    protected void fillValues() {
        if(null==start || null == end)
            loadDates();
        this.maturityDate = end;
        this.tradeDate = start;
        this.partenonId = loadPartenonId(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.amount2 = loadAmount2(this.boCre);
        this.currency2 = this.currency1;
        this.payReceiveAmt2 = BOCreUtils.getInstance().getDirection(amount2);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.direction = BOCreUtils.getInstance().getDirection(loadCreAmount());
    }

    @Override
    protected String loadCreEventType(){
        if(null!= this.boCre){
                if("CANCELED_TRADE".equalsIgnoreCase(this.boCre.getOriginalEventType())
                        || "SLReverseCreFeeAccrualTradeRule".equalsIgnoreCase(this.boCre.getAttributeValue("createdIn"))){
                    return "CANCELED_TRADE";
                }else {
                return this.boCre.getEventType();
            }
        }
        return "";
    }

    public String loadPartenonId(Trade trade) {
        if(null!=trade){
            if("TRADE_VALUATION".equalsIgnoreCase(this.boCre.getOriginalEventType())
                    && "SLReverseCreFeeAccrualTradeRule".equalsIgnoreCase(this.boCre.getAttributeValue("createdIn"))){
                return trade.getKeywordValue("OldPartenonAccountingID");
            }else{
                String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
                return !Util.isEmpty(partenonAccountingID) ? partenonAccountingID : "";
            }
        }
        return "";
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        return null;
    }

    @Override
    protected CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    @Override
    protected String loadIdentifierIntraEOD(){
        return "";
    }

    @Override
    protected Double loadCreAmount(){
        loadDates();
        if(null!=start && start.gte(this.boCre.getEffectiveDate())){
            return loadAmount2(this.boCre);
        }else{
            BOCre previousFeeAccrualCre = BOCreUtils.getInstance().getPreviousFeeAccrualCre(this.boCre.getEffectiveDate(), this.trade);
            if(null!=previousFeeAccrualCre){
                return this.boCre.getAmount(0) - previousFeeAccrualCre.getAmount(0);
            }else{
                return this.boCre.getAmount(0);
            }
        }
    }

    @Override
    protected String loadSettlementMethod(){
        return "";
    }

    private Double loadAmount2(BOCre cre){
        if(null!=cre){
            if("REVERSAL".equalsIgnoreCase(boCre.getCreType())){
                return getAmountFromLinkId(cre);
            }else{
                return super.loadCreAmount();
            }
        }
        return 0.0;
    }

    private Double getAmountFromLinkId(BOCre cre){
        Double amount = 0.0;
        if(null!=cre){
            long linkedId = cre.getLinkedId();
            try {
                BOCre boCre = DSConnection.getDefault().getRemoteBackOffice().getBOCre(linkedId);
                if(null!=boCre){
                    amount = boCre.getAmount(0);
                }
            } catch (CalypsoServiceException e) {
               Log.error(this,e);
            }
        }
        return amount;
    }

    @Override
    protected JDate loadTradeDate(){
        return this.boCre.getTradeDate();
    }

    private JDate loadDates(){

        JDate effectiveDate = this.boCre.getEffectiveDate();

        if(this.trade!=null && this.trade.getProduct() instanceof SecLending) {
            JDate startDate = ((SecLending) this.trade.getProduct()).getStartDate();
            String maturityType = ((SecLending) this.trade.getProduct()).getMaturityType();
            JDate endDate = ((SecLending) this.trade.getProduct()).getEndDate();
            if("OPEN".equalsIgnoreCase(maturityType)){
                endDate = JDate.valueOf("01/01/3000");
            }
            DateRule feeBillingPeriodDateRule = ((SecLending) this.trade.getProduct()).getFeeBillingPeriodDateRule();
            Vector<JDate> generate = feeBillingPeriodDateRule.generate(startDate, endDate);
            SortShell.sort(generate);


            if(!Util.isEmpty(generate) && effectiveDate.before(generate.get(0))){
                start = startDate;
                end = checkPeriod(generate.get(0));
            }else{
                for(JDate period : generate){
                    if(period.gte(effectiveDate) || period.getMonth() == effectiveDate.getMonth()){
                        if(start==null){
                            start = period;
                        }
                        if (end == null){
                            if (BOCreUtils.getInstance().getLastWorkingDateOfMonth(0, period).equals(effectiveDate)){
                                end = effectiveDate;
                            }
                        }
                    }
                    if(period.after(effectiveDate)){
                        if(end == null) {
                            end = checkPeriod(period);
                        }
                    }
                }
            }

            if(end == null){
                end = endDate;
            }
            if(start == null){
               start = startDate;
            }
        }
        return null;
    }


    private JDate checkPeriod(JDate date){
        if(BOCreUtils.getInstance().isFirstWorkingDateOfMonth(date)) {
             return BOCreUtils.getInstance().getLastWorkingDateOfMonth(-1, date);
        }else{
            return date;
        }
    }

}
