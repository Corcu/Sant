package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Arrays;
import java.util.Optional;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreSecLendingCST_UNNET extends BOCreMarginCallCST_UNNET {
    public BOCreSecLendingCST_UNNET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.creDescription = this.creBoTransfer!=null ? this.creBoTransfer.getTransferType() : "";
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
    }

    @Override
    public CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    @Override
    public Double getCashPosition() {
        //final BOCre cRfromTrade = getInstance().getCRfromTrade(this.trade);
        //return getInstance().getAccountBalancefromCre(cRfromTrade);
        return 0.0;
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
        if(failed && Arrays.asList("SETTLED_PAYMENT", "SETTLED_RECEIPT").contains(originalEventType)){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType;
    }

    protected String loadCreEventType(){
        return null!= this.boCre ? this.boCre.getEventType() : "";
    }

    @Override
    protected String loadProductType() {
        return "SecLending";
    }

    @Override
    protected String loadCounterParty() {
        LegalEntity le = null;
        int externalLegalEntityId = 0;
        externalLegalEntityId = null != this.creBoTransfer ? this.creBoTransfer.getExternalLegalEntityId() : 0;
        le = BOCache.getLegalEntity(DSConnection.getDefault(), externalLegalEntityId);
        return null != le ? le.getExternalRef() : "";
    }

    @Override
    protected String getSubType() {
        return this.creBoTransfer != null ? this.creBoTransfer.getTransferType() : "";
    }

    @Override
    protected String getDebitCredit(double value) {
        return "";
    }

    @Override
    protected String loadAccountCurrency() {
        return null != this.account ? this.account.getCurrency() : "";
    }


}
