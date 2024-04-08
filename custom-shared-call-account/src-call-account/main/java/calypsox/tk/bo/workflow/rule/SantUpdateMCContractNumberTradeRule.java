package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class SantUpdateMCContractNumberTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Fill MC_CONTRACT_NUMBER keyword when the contract of trade is updated from trade windows. It's developed for Repo and PDV.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        Product product;
        if (Optional.ofNullable(trade.getProduct()).isPresent()) {
            product = trade.getProduct();
            if (product instanceof SecFinance) {
                int mccId = ((SecFinance) product).getMarginCallContractId(trade);
                if (mccId > 0){
                    trade.addKeyword("MC_CONTRACT_NUMBER", mccId);
                    trade.setInternalReference(String.valueOf(mccId));
                }
            }
        }

        return true;
    }
}
