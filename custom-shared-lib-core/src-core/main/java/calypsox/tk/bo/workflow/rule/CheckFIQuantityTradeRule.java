package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.*;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class CheckFIQuantityTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        Product product = getProduct(trade);
        if (isValidUnderlying(product)) {
            double qty = trade.getQuantity();
            return qty != 0 && qty % 1 == 0;
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Check if trade quantity doesnt contain any decimals";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return false;
    }

    protected Product getProduct(Trade trade) {
        Product product = trade.getProduct();
        if (null != product) {
            if (product instanceof MarginCall) {
                return ((MarginCall) product).getSecurity();
            } else if (product instanceof SecFinance) {
                ((SecFinance) product).getSecurity();
            } else if (product instanceof PerformanceSwap) {
                return ((PerformanceSwap) product).getUnderlyingProduct();
            }
        }
        return null;
    }

    public boolean isValidUnderlying(Product security) {
        return null != security && security instanceof Bond;
    }

}
