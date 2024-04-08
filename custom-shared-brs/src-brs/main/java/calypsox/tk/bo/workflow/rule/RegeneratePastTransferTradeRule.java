package calypsox.tk.bo.workflow.rule;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEventProcessTrade;
import com.calypso.tk.service.DSConnection;

public class RegeneratePastTransferTradeRule implements WfTradeRule {
	
	public static final String KW_REPROCESS_DATE = "ReprocessDate";
	public static final String DATE_PATTERN = "dd/MM/yyyy";
	
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
	   String keywordDateValue = trade.getKeywordValue(KW_REPROCESS_DATE);
	   if(!Util.isEmpty(keywordDateValue) && getReprocessDate(trade)==null) {
			   messages.add("Invalid format for keyword " + KW_REPROCESS_DATE + " = " + keywordDateValue +", date format should be " + DATE_PATTERN);
			   return false;
	   }
      return true;
   }

   public String getDescription() {
      String tmp = "Regenerate past transfers for Trade until " + KW_REPROCESS_DATE + ", date format should be " + DATE_PATTERN;
      return tmp;
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
      if(dbCon == null) {
    	  return true;
      } else if(oldTrade == null) {
    	  return true;
      } else if(trade.getLongId() == 0L) {
    	  return true;
      } else {
    	  JDate reprocessDate = getReprocessDate(trade);
    	  if(reprocessDate!=null) {
    		String[] engines  = {"TransferEngine"};
            PSEventProcessTrade ev = PSEventProcessTrade.createProcessTradeEvent(trade, reprocessDate, true, Action.NONE, engines);
            events.add(ev);
    	  }
         return true;
      }
   }

	protected JDate getReprocessDate(Trade trade) {
		return Util.istringToJDate(trade.getKeywordValue(KW_REPROCESS_DATE), DATE_PATTERN);
	}
	
}
