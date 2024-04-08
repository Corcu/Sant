package calypsox.engine.payment;

import com.calypso.engine.payment.MatchingInfo;
import com.calypso.engine.payment.SecFinanceTransferMatching;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.secfinance.facet.MaturityType;
import com.calypso.tk.service.DSConnection;

public class SecLendingTransferMatching extends SecFinanceTransferMatching
{
  @Override
  public int match(BOTransfer transfer1, BOTransfer transfer2, MatchingInfo matchingInfo) {
	  if (!transfer1.getTradeDate().equals(transfer2.getTradeDate())) {
		  return 0;
	  }
	  
	  if (transfer2.getStatus() == Status.S_VERIFIED) {
		  long tradeID = transfer2.getTradeLongId();
		  int tradeVersion = transfer2.getTradeVersion();
		  Trade trade = null; 
		  try {
			  trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeID);
			  trade = DSConnection.getDefault().getRemoteTrade().undo(trade, tradeVersion);

			  if (trade != null && (trade.getAction() == Action.TERMINATE || trade.getStatus() == Status.S_TERMINATED)) {
				  SecLending sl = (SecLending)trade.getProduct();
				  if (sl.getMaturityType().equals(MaturityType.OPEN.getKey()) && sl.getStartDate() == transfer2.getSettleDate()) {
					 return 1;
				  } 
				  
			  }
		  } catch (CalypsoServiceException e) {
			  Log.error(this, "Could not retrieve Trade " + e.toString());
		  }
	  }
	  
	  return super.match(transfer1, transfer2, matchingInfo);
  }
  
  @Override 
  public boolean isDefaultMatchingRequired(int columnId) {
	  if (columnId == INTERNAL_SDIVERSION || columnId == EXTERNAL_SDIVERSION) {
		  return false;
	  }
	  
	  if (columnId == NETTING_TYPE) {
		  return false;
	  }
	  
	  if (columnId == CALLABLE_DATE) {
		  return false;
	  }
	  
	  if (columnId == INTERNAL_SDSTATUS || columnId == EXTERNAL_SDSTATUS) {
		  return false;
	  }
	  
	  return super.isDefaultMatchingRequired(columnId);
  }
}

