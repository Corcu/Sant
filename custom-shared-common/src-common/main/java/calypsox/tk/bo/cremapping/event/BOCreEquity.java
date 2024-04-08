package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

/**
 * @author acd
 */
public class BOCreEquity extends SantBOCre {

    private boolean predateTrade = false;

    public BOCreEquity(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    public void fillValues() {
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.ownIssuance = BOCreUtils.getInstance().isOwnIssuance(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.issuerName = BOCreUtils.getInstance().loadIssuerName(this.trade);
        this.productCurrency = BOCreUtils.getInstance().loadProductCurrency(this.trade);
        this.buySell = BOCreUtils.getInstance().loadBuySell(this.trade);
        this.accountingRule = BOCreUtils.getInstance().loadAccountingRule(this.boCre);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.underlyingSubType =  BOCreUtils.getInstance().loadUnderlyingSubType(trade);
        this.underlyingDeliveryType =  BOCreUtils.getInstance().loadUnderlyingDeliveryType(trade);
    }

    @Override
    public JDate getCancelationDate() {
        return getInstance().isCanceledEvent(this.boCre) ? getInstance().getActualDate() : null;
    }

    @Override
    protected String loadSettlementMethod() {
        return "";
    }

    @Override
    protected Double getPosition(){
        return 0.0;
    }

    public CollateralConfig getContract() {
        return null;
    }

    protected Account getAccount() {
        return null;
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeEquity();
    }

    @Override
    protected String getDebitCredit(double value) {
        return "";
    }

    protected String loadAccountCurrency() { return ""; }

    protected String getSubType(){
        if(null!=this.trade){
            final Equity product = (Equity) this.trade.getProduct();
            return product.getSecCode("EQUITY_TYPE");
        }
        return "";
    }

    @Override
    public JDate loadEffectiveDate() {
        return getInstance().isEventCOT_REV(this.boCre) ? this.trade.getSettleDate() : this.boCre.getEffectiveDate();
    }

}
