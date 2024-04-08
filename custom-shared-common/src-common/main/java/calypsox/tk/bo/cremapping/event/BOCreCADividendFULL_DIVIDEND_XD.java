package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreCADividendFULL_DIVIDEND_XD extends BOCreCADividendCST{

    private Equity security;

    public BOCreCADividendFULL_DIVIDEND_XD(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = (Equity)BOCreUtils.getInstance().loadSecurity(this.boCre);
    }

    public void fillValues() {
        this.creDescription = this.boCre.getDescription();
        //Quitar cuando MIC consuma FULL_DIVIDEN_XD
        this.eventType = BOCreConstantes.FULL_DIVIDEND_RD;

        this.counterparty = null!=trade ? this.trade.getCounterParty().getExternalRef() : "";
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.partenonId = BOCreUtils.getInstance().generateAlias(this.security, this.portfolioStrategy, this.getSubType(), this.internal, this.portfolio);
        this.ownIssuance = isOwnIssuance();
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = this.security.getIssuer().getExternalRef();
        this.buySell = loadBuySell();
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.role = this.trade.getRole();
        this.caReference = BOCreUtils.getInstance().getCAReference(this.trade);
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

    @Override
    protected String loadCounterParty() {
        return null!=trade ? this.trade.getCounterParty().getExternalRef() : "";
    }

    /**
     * Load Buy or Sell by direction
     * @return
     */
    protected String loadBuySell(){
        String out = "BUY";
        if ("PAY".equalsIgnoreCase(this.direction)){
            out = "SELL";
        }
        return out;
    }

    public String isOwnIssuance() {
        String issuerName = "";
        if (this.security.getIssuer() != null) {
            issuerName = this.security.getIssuer().getCode() != null ? this.security.getIssuer().getCode() : "";
        }
        return BOCreConstantes.BSTE.equals(issuerName) ? "SI" : "NO";
    }

}
