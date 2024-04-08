package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;

import java.util.TimeZone;

public class BOCreEquityNOM_FULL_REV extends BOCreEquity {

    private Product security;

    public BOCreEquityNOM_FULL_REV(BOCre cre, Trade trade) {
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
        this.partenonId = BOCreUtils.getInstance().loadPartenonIdUnliqued(this.boCre, this.trade);
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
    }

    @Override
    public JDate loadEffectiveDate(){
        return this.boCre.getEffectiveDate();
    }

    /**
     * @return SettlementDate from BOCre
     */
    @Override
    protected JDate loadSettlemetDate(){
        return BOCreUtils.getInstance().loadSettlemetDateUnliqued(this.boCre);
    }

    /**
     * @return TradeDate from BoCre
     */
    @Override
    protected JDate loadTradeDate(){
        return BOCreUtils.getInstance().loadTradeDateUnliqued(this.boCre);
    }

    /**
     * @return Trade Long ID form BoCre
     */
    @Override
    protected Long loadTradeId(){
        return BOCreUtils.getInstance().loadTradeIdUnliqued(this.boCre);
    }

}
