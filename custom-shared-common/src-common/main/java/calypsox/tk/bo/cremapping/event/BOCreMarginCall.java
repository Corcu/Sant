package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;
/**
 * @author acd
 */
public class BOCreMarginCall extends SantBOCre {

    private boolean predateTrade = false;

    public BOCreMarginCall(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    public void fillValues() {
        if(BOCreConstantes.COT_REV.equalsIgnoreCase(this.boCre.getEventType())){
            this.ccpName = BOCreUtils.getInstance().getCcpName(this.collateralConfig);
        }

    }

    @Override
    public JDate getCancelationDate() {
        return getInstance().isCanceledEvent(this.boCre) ? getInstance().getActualDate() : null;
    }

    /**
     * COT
     *  Formula : Posición teórica en TRADE DATE de Account ID en (TradeDate D-1) + SUMA [CREs de tipo COT con SentStatus = SENT y Sent Date = Today] + Movimiento CRE
     * COT_REV
     *  Formula : Posición teórica en SETTLE DATE de Account ID en (BOCre SettleDate D-1) + SUMA [CREs de tipo COT_REV con SentStatus = SENT y Sent Date = Today] + Movimiento CRE
     */
    public Double getCashPosition() {
        JDate valDate = null;
        if(BOCreConstantes.COT.equalsIgnoreCase(this.boCre.getEventType())){
            //TradeDate -1
            valDate = addBusinessDays(this.trade.getTradeDate().getJDate(TimeZone.getDefault()),-1);
            return getInstance().getInvLastCashPosition(this.collateralConfig,this.trade, BOCreConstantes.DATE_TYPE_TRADE,BOCreConstantes.THEORETICAL,valDate);
        }else{
            //SettelDate -1
            valDate =  addBusinessDays(this.boCre.getSettlementDate(),-1);
            return getInstance().getInvLastCashPosition(this.collateralConfig,this.trade, BOCreConstantes.DATE_TYPE_SETTLE,BOCreConstantes.THEORETICAL,valDate);
        }
    }

    @Override
    protected String loadSettlementMethod() {
        return "";
    }

    @Override
    protected Double getCreAmount() {
        if(getInstance().isPredate(this.boCre,this.trade)){
            this.predateTrade = true;
        }
        return this.amount1;
    }

    @Override
    protected Double getPosition(){
        //esperar dos segundos antes del cálculo del saldo.... (-_-)
        JDate jDate = getInstance().getActualDate();
        if(BOCreConstantes.COT_REV.equalsIgnoreCase(eventType)){
            if(this.trade.getSettleDate().before(jDate)){
                doSleep();
            }
        }else if(BOCreConstantes.COT.equalsIgnoreCase(eventType)
                && this.trade.getTradeDate().getJDate(TimeZone.getDefault()).before(jDate)){
                doSleep();
        }

        if(getInstance().isNoCouponType(this.trade)){
            final Double creAmount = getCreAmount();
            final Double cashPosition = getCashPosition();
            final Double boCresAmount = getBOCresAmount();
            generatePositionLog(cashPosition,boCresAmount ,creAmount );
            return null!=cashPosition && null!=boCresAmount ? cashPosition + boCresAmount + creAmount : 0.0;
        }else{
            return 0.0;
        }

    }

    protected Double getBOCresAmount(){
        String from = getInstance().buildFrom(this.boCre.getEventType());
        String where = buildWhere();
        return getInstance().getBOCresAmount(from,where,this.boCre,this.predateTrade);
    }

    public CollateralConfig getContract() {
        if(null!=trade && trade.getProduct() instanceof MarginCall){
            final MarginCallConfig marginCallConfig = ((MarginCall) this.trade.getProduct()).getMarginCallConfig();
            final int contractId = null!= marginCallConfig ? marginCallConfig.getId() : 0;
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;
    }

    protected Account getAccount() {
        return getInstance().getAccount(this.collateralConfig,this.boCre.getCurrency(0));
    }

    private void doSleep(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.error(this,e);
        }
    }

    /**
     * @return
     */
    protected String buildWhere(){
        JDate valDate = null;
        if(BOCreConstantes.COT_REV.equalsIgnoreCase(eventType)){
            valDate = this.boCre.getSettlementDate();
        }else if(BOCreConstantes.COT.equalsIgnoreCase(eventType)){
            valDate = this.boCre.getTradeDate();
        }
        if(null!=this.collateralConfig ){
            return getInstance().buildWhere(this.boCre.getEventType(), this.collateralConfig.getId(),valDate);
        }
        return "";
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeMarginCall(this.trade);
    }

    @Override
    protected String getDebitCredit(double value) {
        if(getInstance().isNoCouponType(this.trade)){
            return super.getDebitCredit(value);
        }
        return "NULL";
    }

    protected String loadAccountCurrency(){
        if(getInstance().isNoCouponType(this.trade)){
            return super.loadAccountCurrency();
        }else{
            return "";
        }
    }

    public void generatePositionLog(Double cashPosition, Double boCresAmount, Double creAmount ){
        StringBuilder log = new StringBuilder();
        log.append("CreID: " + this.boCre.getId());
        log.append(" | TradeID: " + this.trade.getLongId());
        log.append(" | CreType: " + this.boCre.getEventType());
        log.append(" | Position: " + cashPosition);
        log.append(" | SumCres: " + boCresAmount);
        log.append(" | CreAmount: " + creAmount + " |");
        Log.system("Saldo Cre", log.toString());
    }

    private JDate addBusinessDays(JDate date,int num){
        return null!=date ? date.addBusinessDays(-1,Util.string2Vector("SYSTEM")) : null;
    }
}
