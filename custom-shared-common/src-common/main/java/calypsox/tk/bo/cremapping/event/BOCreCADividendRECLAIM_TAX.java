package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreCADividendRECLAIM_TAX extends BOCreCADividendCST{

    private Equity security;

    public BOCreCADividendRECLAIM_TAX(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = (Equity)BOCreUtils.getInstance().loadSecurity(this.boCre);
    }

    public void fillValues() {
        this.creDescription = this.boCre.getDescription();
        this.role = this.trade.getRole();
        this.counterparty = loadCounterPartyCADividend(this.role);
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.ownIssuance = isOwnIssuance();
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = this.security.getIssuer().getExternalRef();
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.issuerShortName = this.security.getIssuer().getCode();
        this.caReference = BOCreUtils.getInstance().getCAReference(this.trade);

        //DIVIDENDOS
        if(("Agent").equals(this.role)){
            this.retentionPercentage = ( 1 - (this.amount1 / (this.trade.getNegociatedPrice() * this.trade.getQuantity()))) * 100;
            this.retentionAmount = (this.trade.getNegociatedPrice() * this.trade.getQuantity()) - this.amount1;
            this.netAmount = this.amount1;
        }

        // CA RF
        this.caType = BOCreUtils.getInstance().getCaType(this.trade);
    }

    @Override
    protected JDate getCancelationDate() {
        if(null != this.boCre &&
                (BOCreConstantes.CANCELED_TRADE_EVENT.equalsIgnoreCase(this.boCre.getOriginalEventType()))){
            return getInstance().getActualDate();
        } else return null;
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
        return null!=security ? this.security.getSecCode("EQUITY_TYPE") : "";
    }

    @Override
    protected String loadProccesingOrg(){ return null!=trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";    }

    @Override
    protected String loadEndOfMonth() {
        return "";
    }

    public String isOwnIssuance() {
        String issuerName = "";
        if (this.security.getIssuer() != null) {
            issuerName = this.security.getIssuer().getCode() != null ? this.security.getIssuer().getCode() : "";
        }
        return BOCreConstantes.BSTE.equals(issuerName) ? "SI" : "NO";
    }

}
