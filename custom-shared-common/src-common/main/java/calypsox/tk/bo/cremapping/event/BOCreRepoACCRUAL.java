package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.util.CollateralBasedUtil;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.Optional;
import java.util.TimeZone;

public class BOCreRepoACCRUAL extends SantBOCre {

    private CashFlow cashFlow;
    private CashFlowSet cashFlows;

    public BOCreRepoACCRUAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.cashFlows = BOCreUtils.getInstance().getRepoCashFlows(this.trade, this.boCre);
        this.cashFlow = BOCreUtils.getInstance().getCashFlowForRepoACCRUAL(this.cashFlows,this.trade,this.boCre);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Accrual";
        this.maturityDate = null!=cashFlow ? cashFlow.getEndDate() : null;
        BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.underlyingType = BOCreUtils.getInstance().getMaturityType(this.trade);
        this.revaluation = BOCreUtils.getInstance().getRevaluation(this.trade,this.effectiveDate);

        JDatetime effectiveDateTime = JDatetime.currentTimeValueOf(this.effectiveDate, TimeZone.getDefault());
        boolean isPredate = BOCreUtils.getInstance().isRepoAccrualPredate(this.boCre,this.trade);
        boolean isPartenonChange = BOCreUtils.getInstance().isRepoPartenonChange(this.boCre, this.trade);

        calculateAmount3(effectiveDateTime);

        if( isPredate ){
            this.currency2 = this.tradeCurrency;
            this.currency4 = this.tradeCurrency;
            final boolean undoTerminateAction = isUndoTerminate();
            if(undoTerminateAction){
                BOCre interest = BOCreUtils.getInstance().getLastBoCreINTEREST(JDate.getNow(),this.trade, "INTEREST","NEW",false);
                if(null!=interest){
                    this.amount4 = interest.getAmount(0);
                    this.currency4 = interest.getCurrency(0);
                }
                this.amount2 = 0.0;
                this.currency2 = "";
            }
        } if( isPartenonChange ){
            this.currency2 = this.tradeCurrency;
            this.currency4 = this.tradeCurrency;
        }else {
            doMagic();
        }
        this.payReceiveAmt2 = BOCreUtils.getInstance().getDirection(this.amount2);
        this.payReceiveAmt3 = BOCreUtils.getInstance().getDirection(this.amount3);
        this.payReceiveAmt4 = BOCreUtils.getInstance().getDirection(this.amount4);
        saveAmount4(amount4, currency4);
    }


    private void doMagic(){
        boolean actionPredate = BOCreUtils.getInstance().isActionPredate(this.trade);
        final String endDateChange = this.boCre.getAttributeValue("EndDateChange");
        final boolean undoTerminateAction = isUndoTerminate();

        JDatetime prevEffectiveDateTime = JDatetime.currentTimeValueOf(this.effectiveDate.addBusinessDays(-1, Util.string2Vector("SYSTEM")), TimeZone.getDefault());
        JDate repoEndDate = Optional.ofNullable(trade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getEndDate).orElse(JDate.getNow());

        BOCre accrual = BOCreUtils.getInstance().getLastBoCreACCRUAL(JDate.getNow(),this.trade, "ACCRUAL","NEW",false); //FIXME Buscar ultimo accrual enviado anterior al effectiveDate
        if(null!=accrual){
            this.amount2 = accrual.getAmount(0);
            this.currency2 = accrual.getCurrency(0);
        }
        if("true".equalsIgnoreCase(endDateChange)){
            this.originalEvent = "PREDATED";
        }else if(actionPredate){ //FIXME Evento Predatado
            if(JDate.getNow().after(repoEndDate)){ //Accion predatada sobre repo vencido
                //FIXME Evento Predatado y fecha de envio superior al EndDate del Repo
                //FIXME Buscar Accrual enviado a fecha TERM(endDate) D-1 -> si existe amount3 = ammount4, si no existe se coge ultimo accrual enviado antes de effectiveDate)
                BOCre accrualTermD1 = BOCreUtils.getInstance().getLastBoCreACCRUAL(repoEndDate,this.trade, "ACCRUAL","NEW",false);
                BOCre creforAmount2 = getBoCreAmount2(repoEndDate);
                //FIXME por el amount 1 se debe tomar el el pricer accrual first del Term menos 1 dia, si repoendDate es lunes coger domingo
                PricerMeasure prevAccrual = BOCreUtils.getInstance().calculatePM(getRepoPrevEndDate(repoEndDate), trade, PricerMeasure.ACCRUAL_FIRST, "OFFICIAL_ACCOUNTING");
                this.amount1 = null!=prevAccrual ? prevAccrual.getValue() : 0.0;
                this.currency1 = null!=prevAccrual ? prevAccrual.getCurrency() : "";
                this.direction = BOCreUtils.getInstance().getDirection(this.amount1);

                //FIXME el amount 2 se debe tomar el amount 1 de TERM-2 dias
                this.amount2 = null!=creforAmount2 ? creforAmount2.getAmount(0) : 0.0;
                this.currency2 = null!=creforAmount2 ? creforAmount2.getCurrency(0) : "";

                //FIXME Se debe tomar el amount4 si existe accrual a TERM-1 y si no la del ultimo accrual.
                calculateAmount3(getRepoPrevEndDate(repoEndDate));

                this.amount4 = null!=accrualTermD1 ? accrualTermD1.getAmount(3) : null!=accrual ? accrual.getAmount(3) : 0.0;
                this.currency4 = null!=accrualTermD1 ? accrualTermD1.getCurrency(3) : null!=accrual ? accrual.getCurrency(3) : "";

            }else {
                this.originalEvent = "PREDATED";
            }

        } else if (undoTerminateAction){
            BOCre interest = BOCreUtils.getInstance().getLastBoCreACCRUAL(JDate.getNow(),this.trade, "ACCRUAL","NEW",false);
            if(null!=interest){
                this.amount4 = interest.getAmount(3);
                this.currency4 = interest.getCurrency(3);
            }
            BOCre creforAmount2 = getBoCreAmount2(repoEndDate);
            this.amount2 = null!=creforAmount2 ? creforAmount2.getAmount(0) : 0.0;
            this.currency2 = null!=creforAmount2 ? creforAmount2.getCurrency(0) : "";
        } else {
            JDatetime repoPrevEndDate = null;
            if(effectiveDate.equals(repoEndDate)){
                repoPrevEndDate = getRepoPrevEndDate(repoEndDate); //TODO cambiar simpre -1 sin calendario.
            }else {
                repoPrevEndDate = prevEffectiveDateTime;
            }
            calculateAmount4(repoPrevEndDate);
        }
    }

    private BOCre getBoCreAmount2(JDate repoEndDate){
        BOCre creforAmount2 = null;
        if(repoEndDate.getDayOfWeek() == JDate.MONDAY){ //Accion predatada sobre repo vencido en lunes
            creforAmount2 = BOCreUtils.getInstance().getLastBoCreINTEREST(repoEndDate,this.trade, "INTEREST","REVERSAL",true);
            if(!Optional.ofNullable(creforAmount2).isPresent()){
                creforAmount2 = BOCreUtils.getInstance().getLastBoCreINTEREST(repoEndDate,this.trade, "INTEREST","NEW",false);
            }

        }else {
            creforAmount2 = BOCreUtils.getInstance().getLastBoCreACCRUAL(repoEndDate.addBusinessDays(-1, Util.string2Vector("SYSTEM")),this.trade, "ACCRUAL","NEW",false);
        }
        return creforAmount2;
    }

    private boolean isUndoTerminate(){
        final boolean undoTerminateAction = Optional.ofNullable(this.trade.getKeywordValue("UndoTerminateAction")).isPresent();
        final boolean isOnUndoTerminateDate = JDate.getNow().equals(JDate.valueOf(trade.getKeywordValue("UndoTerminateDate")));
        return undoTerminateAction && isOnUndoTerminateDate;
    }

    private void calculateAmount4(JDatetime prevEffectiveDateTime){
        PricerMeasure prevCumulative = BOCreUtils.getInstance().calculatePM(prevEffectiveDateTime, trade, getPricerMeasure(), "OFFICIAL_ACCOUNTING");
        this.amount4 = Optional.ofNullable(prevCumulative).map(PricerMeasure::getValue).orElse(0.0);
        this.currency4 = Optional.ofNullable(prevCumulative).map(PricerMeasure::getCurrency).orElse(this.tradeCurrency);
    }

    private void calculateAmount2(JDatetime prevEffectiveDateTime){
        PricerMeasure prevAccrual = BOCreUtils.getInstance().calculatePM(prevEffectiveDateTime, trade, PricerMeasure.ACCRUAL_FIRST, "OFFICIAL_ACCOUNTING");
        this.amount2 = Optional.ofNullable(prevAccrual).map(PricerMeasure::getValue).orElse(0.0);
        this.currency2 = Optional.ofNullable(prevAccrual).map(PricerMeasure::getCurrency).orElse(this.tradeCurrency);
    }

    private void calculateAmount3(JDatetime effectiveDateTime){
        PricerMeasure currentCumulative = BOCreUtils.getInstance().calculatePM(effectiveDateTime, trade, getPricerMeasure(), "OFFICIAL_ACCOUNTING");
        this.amount3 = Optional.ofNullable(currentCumulative).map(PricerMeasure::getValue).orElse(0.0);
        this.currency3 = Optional.ofNullable(currentCumulative).map(PricerMeasure::getCurrency).orElse(this.tradeCurrency);
    }

    private int getPricerMeasure(){
        return CollateralBasedUtil.isBSB(trade) ? PricerMeasure.SEC_FIN_SETTLED_INTEREST : PricerMeasure.CUMULATIVE_CASH_INTEREST;
    }

    @Override
    protected JDate loadSettlemetDate(){
        return null!=cashFlow ? cashFlow.getDate() : null;
    }

    @Override
    protected JDate loadTradeDate() {
        return null!=cashFlow ? cashFlow.getStartDate() : null;
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        if("CANCELED_TRADE".equalsIgnoreCase(originalEventType)){
            return JDate.getNow();
        }
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
    protected String loadIdentifierIntraEOD() {
        return "EOD";
    }

    @Override
    protected String loadSettlementMethod(){
        return "";
    }

    /**
     * Save amount4 only if repoEndDate = today || repoEndDate-1 = today
     * @param amount4
     * @param currency
     */
    private void saveAmount4( Double amount4, String currency){
        this.boCre.setAmount(3,amount4);
        this.boCre.setCurrency(3,currency);
    }

    /**
     * @param repoEndDate
     * @return
     */
    private JDatetime getRepoPrevEndDate(JDate repoEndDate){
        return repoEndDate.addDays(-1).getJDatetime(TimeZone.getTimeZone("Europe/Madrid"));
    }

}
