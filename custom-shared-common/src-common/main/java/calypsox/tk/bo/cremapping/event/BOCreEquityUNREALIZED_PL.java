package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.core.SantanderUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;

public class BOCreEquityUNREALIZED_PL extends BOCreEquity {
    private Equity security;

    public BOCreEquityUNREALIZED_PL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = (Equity)BOCreUtils.getInstance().loadSecurity(this.boCre);
    }

    @Override
    public void fillValues() {
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.accountingRule =  BOCreUtils.getInstance().loadAccountingRuleProduc(this.boCre);
        this.internal = isInternal();
        this.partenonId = BOCreUtils.getInstance().generateAlias(this.security, this.portfolioStrategy, this.getSubType(), this.internal, this.portfolio);
        this.ownIssuance = isOwnIssuance();
        this.settleDate = null;
        this.tradeDate = null;
        this.issuerName = getIssuerExtRef();
        this.productCurrency = getProductCurrency();
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    private String isInternal() {
        if (Util.isEmpty(this.accountingRule)) {
            return null;
        }

        if (this.accountingRule.contains("Gestion")) {
            return "Y";
        }
        else if (this.accountingRule.contains("Real")) {
            return "N";
        }

        return null;
    }

    private String getProductCurrency() {
        return this.security.getCurrency();
    }

    private String getIssuerExtRef() {
        return this.security.getIssuer().getExternalRef();
    }

    public String isOwnIssuance() {
        String issuerName = "";
        if (this.security.getIssuer() != null) {
            issuerName = this.security.getIssuer().getCode() != null ? this.security.getIssuer().getCode() : "";
        }
        return SantanderUtil.PO_BSTE.equals(issuerName) ? "SI" : "NO";
    }

    @Override
    public JDate loadEffectiveDate(){
        return this.boCre.getEffectiveDate();
    }

    @Override
    public String loadIdentifierIntraEOD(){
        return "EOD";
    }

    @Override
    protected String getSubType() {
        return this.security.getSecCode("EQUITY_TYPE");
    }
}
