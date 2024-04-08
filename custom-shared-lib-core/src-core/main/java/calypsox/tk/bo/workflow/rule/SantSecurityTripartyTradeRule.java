package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * This rule checks if the trade is on an ISMA triparty contract and that the trade is on a security with a father num
 * front id keywords.
 * 
 * @author aela
 * 
 */
@SuppressWarnings("rawtypes")
public class SantSecurityTripartyTradeRule implements WfTradeRule {

	@SuppressWarnings("unchecked")
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector exception, Task task, Object dbCon, Vector events) {
		if (trade.getProduct() instanceof MarginCall) {
			MarginCall marginCallProduct = (MarginCall) trade.getProduct();
			if (marginCallProduct != null) {

				CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
						marginCallProduct.getMarginCallId());
				if (mcc != null) {
					if ("ISMA".equals(mcc.getContractType()) && "SI".equals(mcc.getAdditionalField("MC_TRIPARTY"))
							&& "SECURITY".equals(marginCallProduct.getSubType())) {
						if (Util.isEmpty(trade.getKeywordValue("FATHER_FRONT_ID"))) {
							messages.add("FATHER_FRONT_ID is mandatory for a security margin call trade on ISMA contracts flagued as triparty");
							return false;
						}
						// return
						// !Util.isEmpty(trade.getKeywordValue("FATHER_FRONT_ID"));

					}
				}
			}
		}
		return true;
	}

	@Override
	public String getDescription() {
		return "Check if the trade is on an ISMA triparty contract and that the trade is on a security with a num front id";
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection ds,
			Vector exception, Task task, Object dbCon, Vector events) {
		return true;
	}

}
