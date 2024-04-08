package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;

public class BOCreEquityPOSITION extends BOCreEquity {

    private Product security;

    public BOCreEquityPOSITION(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromEquity(this.trade);
    }

    @Override
    public void fillValues() {
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.ownIssuance = BOCreUtils.getInstance().isOwnIssuance(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.issuerName = BOCreUtils.getInstance().loadIssuerName(this.trade);
        this.productCurrency = BOCreUtils.getInstance().loadProductCurrency(this.trade);
        this.buySell = BOCreUtils.getInstance().loadBuySell(this.trade);
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.accountingRule = BOCreUtils.getInstance().loadAccountingRule(this.boCre);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.underlyingSubType =  BOCreUtils.getInstance().loadUnderlyingSubType(trade);
        this.underlyingDeliveryType =  BOCreUtils.getInstance().loadUnderlyingDeliveryType(trade);
        this.multiccy = BOCreUtils.getInstance().loadEquityMulticcy(trade);
        if("Y".equalsIgnoreCase(this.multiccy)){
            this.amount2 = BOCreUtils.getInstance().loadEquityMulticcyAmount2(trade, (Equity)trade.getProduct());
            this.currency2 = BOCreUtils.getInstance().loadEquityMulticcyCurrency2(trade);
            this.amount3 = BOCreUtils.getInstance().loadEquityMulticcyAmount3(trade, (Equity)trade.getProduct());
            this.currency3 = BOCreUtils.getInstance().loadEquityMulticcyCurrency3(trade);;
        }
    }

    @Override
    public JDate loadEffectiveDate(){
        return this.boCre.getEffectiveDate();
    }

}
