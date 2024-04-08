package calypsox.tk.bo.workflow.rule;

import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

/**
 * WF rule that sets the MC_CONTRACT_NUMBER tradeKeyword to the CITIBANK MarginCall trades.
 * @author x957355
 *
 */
public class SantSetKoreaMCContractNumberTradeRule implements WfTradeRule{
	private static final String STATIC_DATA_FILER= "isMT599_Management";
	@Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Fill MC_CONTRACT_NUMBER keyword when the contract of trade is from Korea Collateral contract";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        Product product;
        if (Optional.ofNullable(trade.getProduct()).isPresent()) {
        	StaticDataFilter sdFilter = BOCache.getStaticDataFilter(dsCon, STATIC_DATA_FILER);
            product = trade.getProduct();
            if (product instanceof MarginCall) {
                int mccId = ((MarginCall) product).getMarginCallId();
                if (mccId > 0){
                	if(sdFilter != null && sdFilter.accept(trade)) {
                		trade.addKeyword("MC_CONTRACT_NUMBER", mccId);
                	}
                    
                }
            }
        }

        return true;
    }

}
