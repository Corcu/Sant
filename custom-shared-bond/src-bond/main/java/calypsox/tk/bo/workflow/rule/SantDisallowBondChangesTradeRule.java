package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.PARTENONMSGBondMessageFormatter;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class SantDisallowBondChangesTradeRule
        extends SantDisallowCommonChangesTradeRule {
    public static final String REPROCESS_KW = "Reprocess";
    private static final String ISIN = "ISIN";

    protected String getRuleName() {
        return "SantDisallowBondChanges";
    }

    @Override
    protected boolean validatePARTENONMSGMessageFormatter(Trade trade){
        //Check alias of PartenonID
        String partenonAliasKW = trade.getKeywordValue(KW_PARTENON_ALIAS);
        if(!Util.isEmpty(partenonAliasKW)){
            PARTENONMSGBondMessageFormatter formatter = new PARTENONMSGBondMessageFormatter();
            if (!partenonAliasKW.equals(formatter.getAlias(trade))) {
                Log.info(this, getRuleName() + " Partenon Alias has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
                return true;
            }
        }
        return false;
    }

    protected void initValues(Trade trade, Trade oldTrade){
        super.initValues(trade,oldTrade);
        if(Optional.of(trade.getProduct()).filter(Bond.class::isInstance).isPresent()){ //Repo Check
            Bond newBond = (Bond) trade.getProduct();
            Bond oldBond = (Bond) oldTrade.getProduct();
            principal = Math.abs(newBond.calcSettlementAmount(trade, (JDate)null, (PricingEnv)null));
            oldPrincipal = Math.abs(oldBond.calcSettlementAmount(oldTrade, (JDate)null, (PricingEnv)null));
        }
    }

    @Override
    public boolean checkTradeChanges(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector tradeFields, Vector productFields){
        if (trade.getProduct() instanceof Bond){
            Log.debug(Log.WFW, this.getLogCategory() + " - checkTradeChanges");// 345
            boolean validTradeChanges = true;
            boolean validProductChanges = true;
            Vector audits = new Vector();
            if (oldTrade == null) {
                return true;
            } else {
                trade.doAudit(oldTrade, audits, false);
                if (tradeFields == null) {
                    tradeFields = new Vector();
                }

                validTradeChanges = this.checkAudits("Trade", audits, tradeFields, messages);
                Product product = null;

                try {
                    product = (Product)trade.getProduct().clone();
                } catch (Exception var13) {
                    Log.error(this, var13);
                    return false;
                }

                if (product.hasSecondaryMarket()) {
                    audits = this.getProductAudits(product, oldTrade.getProduct());
                    validProductChanges = this.checkAudits("Product", audits, productFields, messages);
                }

                Log.debug(Log.WFW, this.getLogCategory() + " - checkTradeChanges returns: " + (validTradeChanges && validProductChanges ? "true" : "false"));
                return validTradeChanges && validProductChanges;
            }
        } else{
            return super.checkTradeChanges(wc, trade, oldTrade, messages, dsCon, tradeFields, productFields);
        }
    }
}
