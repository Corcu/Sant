package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.PARTENONMSGBondMessageFormatter;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.Vector;

public class SantDisallowBondChangesBooleanTradeRule implements WfTradeRule {

    protected static final String KW_PARTENON_ALIAS = "PartenonAlias";
    protected static final String ISIN = "ISIN";

    //Values to compare
    String newIsin = "";
    String oldIsin = "";
    JDatetime tradeDate = null;
    JDatetime oldTradeDate = null;
    double quantity = 0.0;
    double oldQuantity = 0.0;
    double nominal = 0.0;
    double oldNominal = 0.0;
    String currency = "";
    String oldCurrency = "";
    double principal = 0.0;
    double oldPrincipal = 0.0;
    JDate startDate = null;
    JDate oldStartDate = null;
    String direction = "";
    String oldDirection = "";
    double allocatedQuantity = 0.0;
    double oldAllocatedQuantity = 0.0;
    double dirtyPrice = 0.0;
    double oldDirtyPrice = 0.0;
    double cleanPrice = 0.0;
    double oldCleanPrice = 0.0;

    protected String getRuleName() {
        return "SantDisallowBondChangesBoolean";
    }

    @Override
    public String getDescription() {
        return "If a partenon change is required, returns false";
    }

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector excps, Task task, Object dbCon, Vector events) {

        //Common for all products check counterparty
        if (trade.getCounterParty().getId() != oldTrade.getCounterParty().getId()) {
            return false;
        }

        //Check PARTENONMSG*Prodcut*MessageFormatter
        if (validatePARTENONMSGMessageFormatter(trade)) {
            return false;
        }

        initValues(trade, oldTrade);

        //Check ISIN
        if (!newIsin.equalsIgnoreCase(oldIsin)) {
            return false;
        }

        //Check TradeDate
        if (tradeDate != null && !tradeDate.equals(oldTradeDate)) {
            return false;
        }

        //Check ValueDate/StartDate
        if (startDate != null && !startDate.equals(oldStartDate)) {
            return false;
        }

        //Check Direction
        if (!direction.equalsIgnoreCase(oldDirection)) {
            return false;
        }

        //Check Sec. Quantity
        if (!compareDouble(quantity, oldQuantity) || !compareDouble(allocatedQuantity, oldAllocatedQuantity)) {
            return false;
        }

        //Check Nominal
        if (!compareDouble(nominal, oldNominal)) {
            return false;
        }

        //Check Currency
        if (!currency.equalsIgnoreCase(oldCurrency)) {
            return false;
        }

        //Check Principal
        if (!compareDouble(principal, oldPrincipal)) {
            return false;
        }

        //Check Book common for all prodcuts
        if (trade.getBookId() != oldTrade.getBookId()) {
            return false;
        }

        //Check dirty price
        if (!compareDouble(dirtyPrice, oldDirtyPrice)){
            return false;
        }

        //Check clean price
        if (!compareDouble(cleanPrice, oldCleanPrice)){
            return false;
        }

        return true;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    protected void initValues(Trade trade, Trade oldTrade) {
        //Common values
        newIsin = trade.getProduct().getSecCode(ISIN);
        oldIsin = oldTrade.getProduct().getSecCode(ISIN);
        tradeDate = trade.getTradeDate();
        oldTradeDate = oldTrade.getTradeDate();
        quantity = trade.getQuantity();
        oldQuantity = oldTrade.getQuantity();
        nominal = trade.computeNominal();
        oldNominal = oldTrade.computeNominal();
        currency = trade.getProduct().getCurrency();
        oldCurrency = oldTrade.getProduct().getCurrency();
        startDate = trade.getSettleDate();
        oldStartDate = oldTrade.getSettleDate();
        direction = mapDirection(quantity);
        oldDirection = mapDirection(oldQuantity);
        allocatedQuantity = trade.getAllocatedQuantity();
        oldAllocatedQuantity = oldTrade.getAllocatedQuantity();
        principal = 0.0;
        oldPrincipal = 0.0;
        if (Optional.of(trade.getProduct()).filter(Bond.class::isInstance).isPresent()) { //Repo Check
            Bond newBond = (Bond) trade.getProduct();
            Bond oldBond = (Bond) oldTrade.getProduct();
            principal = Math.abs(newBond.calcSettlementAmount(trade, (JDate) null, (PricingEnv) null));
            oldPrincipal = Math.abs(oldBond.calcSettlementAmount(oldTrade, (JDate) null, (PricingEnv) null));
        }
        dirtyPrice = trade.getNegociatedPrice();
        oldDirtyPrice = oldTrade.getNegociatedPrice();
        cleanPrice = trade.getTradePrice();
        oldCleanPrice = oldTrade.getTradePrice();
    }

    protected boolean compareDouble(double first, double second) {
        DecimalFormat dec = new DecimalFormat("#0.00");
        return dec.format(first).equalsIgnoreCase(dec.format(second));
    }

    protected boolean validatePARTENONMSGMessageFormatter(Trade trade){
        //Check alias of PartenonID
        String partenonAliasKW = trade.getKeywordValue(KW_PARTENON_ALIAS);
        if(!Util.isEmpty(partenonAliasKW)){
            PARTENONMSGBondMessageFormatter formatter = new PARTENONMSGBondMessageFormatter();
            if (!partenonAliasKW.equals(formatter.getAlias(trade))) {
                return true;
            }
        }
        return false;
    }

    protected String mapDirection(double quantity){
        if(quantity>0.0){
            return "BUY";
        }else {
            return "SELL";
        }
    }
}
