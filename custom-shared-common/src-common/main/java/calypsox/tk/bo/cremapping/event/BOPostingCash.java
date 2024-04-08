package calypsox.tk.bo.cremapping.event;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
//import calypsox.util.OBBReportUtil;

/**
 * @author acg
 */
public class BOPostingCash extends SantBOCre {

    private boolean predateTrade = false;

    public BOPostingCash(BOPosting posting, Trade trade) {
    	super();
        super.setBOPosting(posting,trade);
    }

    @Override
    public void fillValues(){
    	this.rate = null;
    	this.ratePositiveNegative = "";
    	this.accountBalance = null;
    	this.transferAccount = getInstance().getTransferAccount(this.settlementMethod,this.creBoTransfer);
        this.nettingType = null!=this.creBoTransfer && !Util.isEmpty(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettingType() : ""; 
        this.nettingParent = loadNettingParent();
        this.isin = "";
    	this.underlyingType = "";
    	this.partenonId = loadPartenonId();
    }

    @Override
    public JDate getCancelationDate() {
        return getInstance().isCanceledEvent(this.boPosting, this.trade) ? getInstance().getActualDate() : null;
    }

    /**
     * COT
     *  Formula : Posición teórica en TRADE DATE de Account ID en (TradeDate D-1) + SUMA [CREs de tipo COT con SentStatus = SENT y Sent Date = Today] + Movimiento CRE
     * COT_REV
     *  Formula : Posición teórica en SETTLE DATE de Account ID en (BOCre SettleDate D-1) + SUMA [CREs de tipo COT_REV con SentStatus = SENT y Sent Date = Today] + Movimiento CRE
     */
    public Double getCashPosition() {
        return null;
    }

    @Override
    protected String loadSettlementMethod() {
    	if(this.creBoTransfer!=null && "SWIFT".equalsIgnoreCase(this.creBoTransfer.getSettlementMethod())){
    		return "SWIFT";
    	}
    	else{
    		return "Direct";
    	}
    }

    @Override
    protected Double getPosition(){
    	return null;
    }
    
    @Override
    protected Integer loadAccountID(){
    	return null;
    }
    
//    protected Double getBOCresAmount(){
//        String from = getInstance().buildFrom(this.boCre.getEventType());
//        String where = buildWhere();
//        return getInstance().getBOCresAmount(from,where,this.boCre,this.predateTrade);
//    }

    public CollateralConfig getContract() {
        final MarginCallConfig marginCallConfig = ((MarginCall) this.trade.getProduct()).getMarginCallConfig();
        final int contractId = null!= marginCallConfig ? marginCallConfig.getId() : 0;
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
    }

    protected Account getAccount() {
        return getInstance().getAccount(this.collateralConfig,this.boPosting.getCurrency());
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
        return getInstance().buildWhere(this.boCre.getEventType(),  this.collateralConfig.getId(),valDate);
    }

    @Override
    protected String loadProductType() {
    	
        return null!=this.trade ?this.trade.getProductType() : "";
    }

    @Override
    protected String getDebitCredit(double value) {
        if(getInstance().isNoCouponType(this.trade)){
            return super.getDebitCredit(value);
        }
        return "";
    }

    protected String loadAccountCurrency(){
    	return "";
    }

    private void generatePositionLog(Double cashPosition, Double boCresAmount, Double creAmount ){
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
        
    
    /**
     * @return Posting Id
     */
    protected long loadCreId(){
    	return this.boPosting.getId();
    }

    /**
     * 
     * @return Posting Description
     */
    protected String loadCreDescription(){
    	return this.boPosting.getDescription();
    }
    
    /**
     * 
     * @return Posting Type
     */
    protected String loadCreType(){
    	return this.boPosting.getPostingType();
    }
    
    /**
     * 
     * @return Linked Id
     */
    protected long loadLinkedId(){
    	return this.boPosting.getLinkedId();
    }
    
    /**
     * 
     * @return Original Event Type
     */
    protected String loadOriginalEventType(){
    	return this.boPosting.getOriginalEventType();
    }
    
    /**
     * 
     * @return Creation Date
     */
    protected JDatetime loadCreationDate(){
    	return this.boPosting.getCreationDate();
    }
    
    /**
     * 
     * @return Effective Date
     */
    protected JDate loadEffectiveDate(){
    	return this.boPosting.getEffectiveDate();
    }
    
    /**
     * 
     * @return Booking Date
     */
    protected JDate loadBookingDate(){
    	return this.boPosting.getBookingDate();
    }
    
    /**
     * 
     * @return Status
     */
    protected String loadStatus(){
    	return this.boPosting.getStatus();
    }

    protected String loadCreEventType(){
        return null!= this.boPosting ? this.boPosting.getEventType() : "";
    }

    /**
     * @return Currency from BoCre
     */
    protected String loadCurrency(){
        return this.boPosting.getCurrency();
    }

    protected Double loadCreAmount(){
        return this.boPosting.getAmount();
    }

    /**
     * @return Proccesing OR form trade
     */
    protected String loadProccesingOrg(){
        return null!=trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";
    }

    protected Integer loadContractID(){
        return null;
    }
    protected String loadContractType(){
    	return null;
    }

    protected String loadStm(){
        return null;
    }

    protected String loadIdentifierIntraEOD(){
        return "";
    }

    /**
     * @return CounterParty From trade
     */
    protected String loadCounterParty(){
        return null!=trade ? this.trade.getCounterParty().getExternalRef() : "";
    }

    protected String getSubType(){
        return null!=this.trade ? this.trade.getProductSubType() : "";
    }

    protected String loadBookName(){
        return null!=this.book ? this.book.getName() : "";
    }
    
    /**
     * @return SettlementDate from BOPosting
     */
    protected JDate loadSettlemetDate(){
        return null!=this.creBoTransfer ? this.creBoTransfer.getSettleDate() : null;
    }

    /**
     * @return TradeDate from BoPosting
     */
    protected JDate loadTradeDate(){
    	return null!=this.creBoTransfer ? this.creBoTransfer.getSettleDate() : null;
    }

    /**
     * @return Trade Long ID form BOPosting
     */
    protected Long loadTradeId(){
        return this.boPosting.getTradeLongId();
    }
  
    protected String getDirection() {
        return getInstance().getDirection(this.boPosting.getOtherAmount());
    }
    
    protected String loadPartenonId() {
    	BOTransfer xfer = getInstance().getTransfer(this.boPosting);
    	boolean isCstNetSettled = BOCreConstantes.CST_NET_S_SETTLED.equalsIgnoreCase(this.boPosting.getEventType());
    	boolean isNettingTypeCptyOtc = null!=xfer ? "CounterPartyOTC".equalsIgnoreCase(xfer.getNettingType()) : false;
    	return (null==this.trade || (isCstNetSettled && isNettingTypeCptyOtc)) ? "" : this.trade.getKeywordValue("PartenonAccountingID");
    }
    
    protected Long loadNettingParent() {
    	final String creEventType = boPosting.getEventType();
        if(BOCreConstantes.CST_NET_S_SETTLED.equalsIgnoreCase(creEventType)){
        	//return null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getLongId() : 0;
        	return null!=this.creBoTransfer ? this.creBoTransfer.getLongId() : 0;
        }
        else if(BOCreConstantes.CST_UNNET_S_SETTLED.equalsIgnoreCase(creEventType)){
       		//return this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettedTransferLongId() : 0;
       		return null!=this.creBoTransfer ? this.creBoTransfer.getNettedTransferLongId() : 0;
        }
        return null;
    }


    protected String loadEndOfMonth(){
//        if(OBBReportUtil.isLastWorkingDateOfMonth(this.boPosting.getEffectiveDate())){
//            return "SI";
//        }
        return "NO";
    }


}
