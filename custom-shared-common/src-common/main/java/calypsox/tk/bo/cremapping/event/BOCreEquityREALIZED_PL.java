package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.liquidation.loader.LiquidatedPositionCriteriaBuilder;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.LiquidatedPositionArray;

import java.util.TimeZone;

public class BOCreEquityREALIZED_PL extends BOCreEquity {

    private Product security;

    public BOCreEquityREALIZED_PL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromEquity(this.trade);
        this.trade = checkLiquidatedPositon(this.boCre, this.trade);
    }

    @Override
    public void fillValues() {
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.ownIssuance = BOCreUtils.getInstance().isOwnIssuance(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = BOCreUtils.getInstance().loadIssuerName(this.trade);
        this.productCurrency = BOCreUtils.getInstance().loadProductCurrency(this.trade);
        this.buySell = BOCreUtils.getInstance().loadBuySell(this.trade);
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
        return this.trade.getSettleDate();
    }

    /**
     * @return TradeDate from BoCre
     */
    @Override
    protected JDate loadTradeDate(){
        return JDate.valueOf(this.trade.getTradeDate());
    }

    /**
     * @return Trade Long ID form BoCre
     */
    @Override
    protected Long loadTradeId(){
        return this.trade.getLongId();
    }

    private Trade checkLiquidatedPositon(BOCre boCre, Trade trade){
        LiquidatedPositionArray liqPosArray;
        Trade linkedTrade;
        int liquidationConfig = 0;

        try {
            if ("REVERSAL".equals(boCre.getCreType())){
                return trade;
            }
            LiquidatedPositionCriteriaBuilder liqPosCriteria = LiquidatedPositionCriteriaBuilder.create().tradeId(boCre.getLinkedTradeLongId()).liquidationConfig(liquidationConfig).orderForUndo();
            liqPosArray = DSConnection.getDefault().getRemoteLiquidation().getLiquidatedPositions(liqPosCriteria);

        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve LiquidationPosition. " + e.toString());
            return null;
        }
        if (liqPosArray == null || liqPosArray.isEmpty()) {
            return null;
        }
        //Busca el registro del LiquidationPosition que coincida con Trade y linkedTrade
        for (int i = 0; i < liqPosArray.size(); i++){
            if (boCre.getTradeLongId() == liqPosArray.get(i).getFirstTradeId() && boCre.getLinkedTradeLongId() == liqPosArray.get(i).getSecondTradeId()) {
                try {
                    linkedTrade = DSConnection.getDefault().getRemoteTrade().getTrade(boCre.getLinkedTradeLongId());
                }
                catch (CalypsoServiceException e) {
                    Log.error(this, "Error loading Trade: " + boCre.getLinkedTradeLongId() + ": " + e);
                    return null;
                }
                return linkedTrade;
            } else if (boCre.getLinkedTradeLongId() == liqPosArray.get(i).getFirstTradeId() && boCre.getTradeLongId() == liqPosArray.get(i).getSecondTradeId()){
                return trade;
            }
        }
        return null;
    }

}
