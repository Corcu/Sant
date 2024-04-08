package calypsox.tk.bo.workflow.rule;

import static calypsox.tk.core.CollateralStaticAttributes.DVP;
import static calypsox.tk.core.CollateralStaticAttributes.FOP;
import static calypsox.tk.core.CollateralStaticAttributes.RVP;
import static calypsox.tk.core.CollateralStaticAttributes.TK_DVP_ALLOCATION;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;

/*
 * Checks if Trade is MArinCall and the underlying is Security AND KEY_WORD_DVP_ALLOCATION = DVP or RVP or FOP
 * If so it returns true so the trade moves forward in the workflow.
 * Otherwise returns false.
 */
public class SantCheckMarginCallSecurityDVPTradeRule implements WfTradeRule {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade,
	    final Trade oldTrade, final Vector messages,
	    final DSConnection dsCon, final Vector exception, final Task task,
	    final Object dbCon, final Vector events) {

	Log.debug(SantCheckMarginCallSecurityDVPTradeRule.class,
		"Check - Start");

	if (!trade.getProductType().equals(Product.MARGINCALL)) {
	    return true;
	}

	final MarginCall marginCall = (MarginCall) trade.getProduct();
	if ((!marginCall.getFlowType().equals("SECURITY"))
		&& (marginCall.getSecurity() == null)) {
	    return true;
	}

	// So we have MArginCall with underlying Security and flowType=SECURITY
	final String tkDVPAllocation = trade.getKeywordValue(TK_DVP_ALLOCATION);
	if (DVP.equals(tkDVPAllocation) || RVP.equals(tkDVPAllocation)
		|| FOP.equals(tkDVPAllocation)) {
	    return true;
	} else {
	    final String msg = TK_DVP_ALLOCATION
		    + " trade keyword must be filled.[SantCheckMarginCallSecurityDVPTradeRule]";
	    messages.add(msg);
	    return false;
	}

    }

    @Override
    public String getDescription() {
	final String desc = "Checks if Trade is MArinCall and the underlying is Security "
		+ "AND KEY_WORD_DVP_ALLOCATION = DVP or RVP or FOP\n"
		+ "If so it returns true so the trade moves forward in the workflow \n"
		+ "Otherwise returns false.";
	return desc;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade,
	    final Trade oldTrade, final Vector messages,
	    final DSConnection dsCon, final Vector exception, final Task task,
	    final Object dbCon, final Vector events) {

	return true;
    }
}
