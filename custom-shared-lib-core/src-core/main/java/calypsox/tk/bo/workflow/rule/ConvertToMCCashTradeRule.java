/**
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;

/**
 * @author fperezur
 *
 */
public class ConvertToMCCashTradeRule implements WfTradeRule {

	private static final String TRANSFER_SUB_TYPE = "COLLATERAL";
	private static final double CASH_TRADE_PRICE = 0;

	/** Trade keywords*/
	private static final String TRIPARTY_ALLOC_KW = "FromTripartyAllocation";
	private static final String COLLATERAL_TYPE_KW = "CollateralType";
	private static final String REVERSE_ALLOC_KW = "ReversedAllocationTrade";
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.bo.workflow.WfTradeRule#check(com.calypso.tk.bo.
	 * TaskWorkflowConfig, com.calypso.tk.core.Trade, com.calypso.tk.core.Trade,
	 * java.util.Vector, com.calypso.tk.service.DSConnection, java.util.Vector,
	 * com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
			Vector messages, DSConnection dsCon, Vector excps, Task task,
			Object dbCon, Vector events) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.bo.workflow.WfTradeRule#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Trade rule to change the Security Transfer to Cash Transfer";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.bo.workflow.WfTradeRule#update(com.calypso.tk.bo.
	 * TaskWorkflowConfig, com.calypso.tk.core.Trade, com.calypso.tk.core.Trade,
	 * java.util.Vector, com.calypso.tk.service.DSConnection, java.util.Vector,
	 * com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
			Vector messages, DSConnection dsCon, Vector excps, Task task,
			Object dbCon, Vector events) {

		boolean noErrors = true; 
		String allocationKW = trade.getKeywordValue(TRIPARTY_ALLOC_KW);
		String colTypeKW = trade.getKeywordValue(COLLATERAL_TYPE_KW);
		String RevAllocTradeKW = trade.getKeywordValue(REVERSE_ALLOC_KW);
		
		if (trade.getProduct() instanceof MarginCall && StringUtils.isBlank(RevAllocTradeKW) && 
				StringUtils.isNotBlank(allocationKW) && "true".equalsIgnoreCase(allocationKW) && 
				StringUtils.isNotBlank(colTypeKW) && "cash".equalsIgnoreCase(colTypeKW)) {
			MarginCall marginCall = (MarginCall) trade.getProduct();
			if (marginCall != null && marginCall.getSecurity() != null) {
				boolean isBond = marginCall.getSecurity() instanceof Bond;
				
				//Get the necessary values from original product
				double currQty = trade.getQuantity() * marginCall.getPrincipal();
				String tradeCcy = trade.getTradeCurrency();
				
				if(isBond){
					Bond sec = (Bond)marginCall.getSecurity();
					if (sec.isMutable()) {
						sec.setFaceValue(1.0);
					} else {
						try {
							Bond newSec = (Bond) sec.cloneIfImmutable();
							newSec.setFaceValue(1.0);
							marginCall.setSecurity(newSec);
						} catch (CloneNotSupportedException e) {
							Log.error(this, e);
						}
					}
				}
				
				//Product modifications
				marginCall.setSecurity(null);
				marginCall.setFlowType(TRANSFER_SUB_TYPE);
				marginCall.setPrincipal(currQty);
				marginCall.setCurrencyCash(tradeCcy);
				marginCall.setIsPledgeMovementB(false);
				marginCall.setOrdererRole("Client");
				marginCall.setOrdererLeId(trade.getCounterParty().getId());

				//Trade modifications
				if (marginCall.getSecurity() == null) {
					trade.setProduct(marginCall);
					double cashQty = trade.getQuantity()/ Math.abs(trade.getQuantity());
					trade.setQuantity(cashQty);
					trade.setTradePrice(CASH_TRADE_PRICE);
				} else {
					Log.error(this, "It has not been possible to change the Security to Cash type");
					noErrors = false;
				}
				
			}
		}
		return noErrors;
	}
}
