package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;
/**
 * @author acd
 */
public class BOCreMarginCallCST extends SantBOCre {

    public BOCreMarginCallCST(BOCre cre, Trade trade) {
        super(cre,trade);
    }

    public void fillValues() {
        this.creDescription = "Cash Settlement";
        this.settlementMethod = !Util.isEmpty(getInstance().getSettleMethodFromSdi(this.creBoTransfer)) ? getInstance().getSettleMethodFromSdi(this.creBoTransfer) : getInstance().getSettleMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccountSM(this.settlementMethod,this.creBoTransfer);
        this.nettingType = null!=this.creBoTransfer && !Util.isEmpty(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettingType() : "";
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getLongId() : 0;
    }

    public CollateralConfig getContract() {
        if(null!=trade && this.trade.getProduct() instanceof MarginCall){
            final MarginCallConfig marginCallConfig = ((MarginCall) this.trade.getProduct()).getMarginCallConfig();
            final int contractId = null!= marginCallConfig ? marginCallConfig.getId() : 0;
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;
    }

    @Override
    public JDate getCancelationDate() {
        return getInstance().isCanceledTransfer(this.boCre)
               && getInstance().isCanceledEvent(this.trade) ? getInstance().getActualDate() : null;
    }

    @Override
    protected Double getPosition() {
        return getCashPosition();
    }

    @Override
    protected String loadCounterParty() {
        LegalEntity le = null;
        int externalLegalEntityId = 0;
        if(!BOCreUtils.getInstance().isCouponType(this.creBoTransfer)){
            externalLegalEntityId = null!= this.clientBoTransfer ? this.clientBoTransfer.getExternalLegalEntityId() : 0;
            le = BOCache.getLegalEntity(DSConnection.getDefault(),externalLegalEntityId );
        }else{
            externalLegalEntityId = null!= this.creBoTransfer ? this.creBoTransfer.getExternalLegalEntityId() : 0;
            le = BOCache.getLegalEntity(DSConnection.getDefault(),externalLegalEntityId );
        }
        return null!=le ? le.getExternalRef() : "";
    }

    /**
     *Formula : Posición liquidada en SETTLE DATE de Account ID en D-1 + SUMA [CREs de tipo CST con SentStatus = SENT,
     *  ProductType = MarginCall y Sent Date = Today] + Movimiento CRE
     */
    public Double getCashPosition() {
        if(getInstance().isNoCouponType(this.trade)){
            final BOCre cRfromTrade = getInstance().getCRfromTrade(this.trade);
            return getInstance().getAccountBalancefromCre(cRfromTrade);
        }else{
            return 0.0;
        }
    }

    @Override
    protected Account getAccount() {
        return getInstance().getAccount(this.clientBoTransfer);
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeMarginCall(this.trade);
    }

    @Override
    protected String getSubType(){
        if(!BOCreUtils.getInstance().isCouponType(this.creBoTransfer)){
            return null!=this.clientBoTransfer ? this.clientBoTransfer.getTransferType() : "";
        }else{
            return this.creBoTransfer.getTransferType();
        }
    }

    @Override
    protected String loadAccountCurrency() {
        if(getInstance().isNoCouponType(this.trade)){
            return super.loadAccountCurrency();
        }else{
            return "";
        }
    }

    @Override
    protected String getDebitCredit(double value) {
        if(getInstance().isNoCouponType(this.trade)){
            return super.getDebitCredit(value);
        }else{
            return "NULL";
        }
    }
}
