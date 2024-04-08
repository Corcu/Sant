package calypsox.tk.bo.cremapping.event;

import java.util.TimeZone;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.core.SantanderUtil;

public class BOCreEquityPOSITION_SHORTLONG extends BOCreEquity {
    private Equity security;

    public BOCreEquityPOSITION_SHORTLONG(BOCre cre, Trade trade) {
        super(cre, trade);

    }

    @Override
    protected void init() {
        super.init();
        this.security = (Equity)BOCreUtils.getInstance().loadSecurity(this.boCre);
    }

    @Override
    protected String loadCreEventType() {
        String toReplace = "RECLASS_";
        return null!= this.boCre ? this.boCre.getEventType().substring(toReplace.length()) : "";
    }

    @Override
    public void fillValues() {
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.accountingRule = getAccountingRule(this.boCre);
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

    private String getAccountingRule(BOCre boCre) {
        int ruleId = this.boCre.getAccountingRuleId();
        try {
            if (ruleId > 0) {
                AccountingRule accRule = DSConnection.getDefault().getRemoteAccounting().getAccountingRule(ruleId);
                return accRule.getName();
            }
        } catch (Exception e) {
            Log.error(this, "Could not get the accounting rule " + ruleId);
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
