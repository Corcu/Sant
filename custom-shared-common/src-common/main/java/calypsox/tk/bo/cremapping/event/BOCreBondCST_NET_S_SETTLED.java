package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.cremapping.util.BondBOCreSettleMethodHandler;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreBondCST_NET_S_SETTLED extends SantBOCre{

    private Product security;

    public BOCreBondCST_NET_S_SETTLED(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromBond(this.trade);
    }

    @Override
    protected void fillValues() {
        this.settlementMethod = new BondBOCreSettleMethodHandler().getCreSettlementMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccountEquity(this.settlementMethod,this.creBoTransfer);
        this.nettingParent = null!=this.creBoTransfer ? this.boCre.getNettedTransferLongId() : 0L;
        this.nettingType = getInstance().getNettingType(this.creBoTransfer);
        //Valor por defecto TR (Termino), dado que es un campo obligatorio a enviar a MIC, aunque no aplica para bonos
        this.underlyingType = BOCreConstantes.TR;
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        //Dado que es un campo obligatorio a enviar a MIC, se mete logica B (Buy) o S (Sell)
        this.tomadoPrestado = BOCreUtils.getInstance().loadBS(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.productCurrency = BOCreUtils.getInstance().loadProductCurrency(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
        if(failed && null!=originalEventType && originalEventType.contains("SETTLED")){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType;
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        List<String> listevents = Util.stringToList("CANCELED_SEC_PAYMENT,CANCELED_SEC_RECEIPT,CANCELED_RECEIPT,CANCELED_PAYMENT");

        if(listevents.contains(originalEventType) && null!=this.trade){
            return JDate.getNow();
        }

        return null;
    }

    public CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    protected String getSubType() {
        return getInstance().getSubTypeBond(this.trade);
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadSettlementMethod(){
        return getInstance().getSettleMethod(this.creBoTransfer);
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeBondForwardSpot(this.trade);
    }

}
