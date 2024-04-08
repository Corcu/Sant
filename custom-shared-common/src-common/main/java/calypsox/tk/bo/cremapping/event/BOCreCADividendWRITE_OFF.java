package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
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

public class BOCreCADividendWRITE_OFF extends SantBOCre {

    private Product security;
    public static final String writeOffRole = "AgentReal";

    public BOCreCADividendWRITE_OFF(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromCA(this.trade);
    }

    public void fillValues() {
        this.creDescription = this.boCre.getDescription();
        this.direction = getDirection(this.direction);
        this.settlementMethod = getInstance().getSettleMethod(this.creBoTransfer);
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = BOCreUtils.getInstance().getIssuerName(BOCreUtils.getInstance().getLegalEntity(this.security));
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.role = writeOffRole;
        this.counterparty = loadCounterPartyCADividend(this.trade.getRole());
        this.issuerShortName = BOCreUtils.getInstance().getIssuerCode(BOCreUtils.getInstance().getLegalEntity(this.security));
        this.caReference = BOCreUtils.getInstance().getCAReference(this.trade);
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.ownIssuance = isOwnIssuance();
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
        if(failed && null!=originalEventType && originalEventType.contains("SETTLED")){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType.contains("PAYMENT") ? "SETTLED_RECEIPT" : "SETTLED_PAYMENT";
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
    protected String getSubType() { return null!=security ? this.security.getSecCode("EQUITY_TYPE") : "";   }

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

    public String isOwnIssuance() {
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
                issuerName = le.getCode() != null ? le.getCode() : "";
            }
        }
        return BOCreConstantes.BSTE.equals(issuerName) ? "SI" : "NO";
    }

    public String getDirection(String direction) {
        return "PAY".contains(direction) ? "REC": "PAY";
    }

}
