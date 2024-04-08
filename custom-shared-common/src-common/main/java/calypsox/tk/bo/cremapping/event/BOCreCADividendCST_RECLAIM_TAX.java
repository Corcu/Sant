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

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreCADividendCST_RECLAIM_TAX extends SantBOCre {

    private Product security;

    public BOCreCADividendCST_RECLAIM_TAX(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromCA(this.trade);
    }

    public void fillValues() {
        this.eventType = "CST_RECLAIM";
        this.creDescription = this.boCre.getDescription();
        this.settlementMethod = new CABOCreSettleMethodHandler().getCreSettlementMethod(this.creBoTransfer);
        this.mcContractID = BOCreUtils.getInstance().loadContractID(this.trade);
        if (getContract() != null){
            this.mcContractType = getContract().getContractType();
        }
        this.transferAccount = getInstance().getTransferAccountEquity(this.settlementMethod,this.creBoTransfer);
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.ownIssuance = isOwnIssuance();
        this.partenonId = BOCreUtils.getInstance().getPartenonCA(this.trade);
        this.role = this.trade.getRole();
        this.counterparty = loadCounterPartyCADividend(this.role);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = BOCreUtils.getInstance().getIssuerName(BOCreUtils.getInstance().getLegalEntity(this.security));
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.claimProductType = BOCreUtils.getInstance().loadClaimProductType(this.trade);
        this.issuerShortName = BOCreUtils.getInstance().getIssuerCode(BOCreUtils.getInstance().getLegalEntity(this.security));
        this.caReference = BOCreUtils.getInstance().getCSTCAReference(this.trade, this.creBoTransfer);

    }

    @Override
    public CollateralConfig getContract() {
        final int contractId = this.trade != null ? trade.getKeywordAsInt("CASource") : 0;
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
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
    protected String getSubType() { return null!=security ? this.security.getSecCode("EQUITY_TYPE") : "";   }

    @Override
    protected String loadProccesingOrg(){ return null!=trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";    }

    public String isOwnIssuance() {
        return BOCreConstantes.BSTE.equals(getIssuerCode()) ? "SI" : "NO";
    }

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
