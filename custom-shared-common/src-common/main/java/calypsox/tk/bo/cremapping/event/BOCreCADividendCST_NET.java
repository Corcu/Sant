package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.cremapping.util.CABOCreSettleMethodHandler;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreCADividendCST_NET extends SantBOCre {

    private Product security;

    public BOCreCADividendCST_NET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromCA(this.trade);
    }

    public void fillValues() {
        this.creDescription = this.boCre.getDescription();
        this.settlementMethod = new CABOCreSettleMethodHandler().getCreSettlementMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccountEquity(this.settlementMethod,this.creBoTransfer);
        this.nettingType = null!=this.creBoTransfer && !Util.isEmpty(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettingType() : "";
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getLongId() : 0;
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.role = this.trade.getRole();
        this.counterparty = loadCounterPartyCADividend(this.role);
        this.caReference = BOCreUtils.getInstance().getNetCAReference(this.creBoTransfer);
    }

    @Override
    public CollateralConfig getContract() {
        final int contractId = this.trade != null ? trade.getKeywordAsInt("CASource") : 0;
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
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
    protected JDate getCancelationDate() {
           if(null != this.boCre &&
                   (BOCreConstantes.CANCEL_PAYMENT_EVENT.equalsIgnoreCase(this.boCre.getOriginalEventType())
                           ||(BOCreConstantes.CANCELED_RECEIPT_EVENT.equalsIgnoreCase(this.boCre.getOriginalEventType())))){
               return getInstance().getActualDate();
           } else return null;
    }

    protected Account getAccount() { return null; }

    @Override
    protected Double getPosition(){
        return 0.0;
    }

    @Override
    protected Double loadCreAmount(){
        return null!=this.boCre ? this.boCre.getAmount(0) : 0.0D;
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadProductType() {
        return null!=this.trade ? this.trade.getProductType() : "";
    }

    @Override
    protected String getSubType() {
        return null != this.trade ? this.trade.getProductSubType() : "";
    }

    @Override
    protected String loadProccesingOrg(){ return null!=trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";    }

    public String getIssuerCode() {
        String issuerName = "";
        if (this.security instanceof Equity) {
            Equity equity = (Equity) this.security;
            if (equity.getIssuer() != null) {
                issuerName = equity.getIssuer().getCode() != null ? equity.getIssuer().getCode() : "";
            }
        } else if (this.security instanceof Bond) {
            Bond bond = (Bond) this.security;
            if (bond.getIssuerId() != 0) {
                LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
                issuerName =le.getCode() != null ? le.getCode() : "";
            }
        }
        return issuerName;
    }

}
