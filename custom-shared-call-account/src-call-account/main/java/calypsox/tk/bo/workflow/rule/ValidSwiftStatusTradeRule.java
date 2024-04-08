package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.Vector;
/**
 * @author acd
 */
public class ValidSwiftStatusTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return validSwfitSatatusMessages(trade);
    }

    @Override
    public String getDescription() {
        return "Return false if some Swift message for the trade are in status SENT or ACKED";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }


    /**
     * @param trade
     * @return true if Swift Messages are not SENT or ACKED
     */
    private boolean validSwfitSatatusMessages(Trade trade){
        if(null!=trade){
            StringBuilder where = new StringBuilder();
            where.append(" address_method LIKE 'SWIFT' ");
            where.append(" AND TRADE_ID = " + trade.getLongId());
            where.append(" AND message_type LIKE 'PAYMENTMSG'");
            where.append(" AND message_status IN ('SENT','ACKED')");
            try {
                final MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(where.toString(),null);
                return messages!=null && Util.isEmpty(messages.getMessages());
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading Swift message for trade: " + trade.getLongId() );
            }
        }
        return true;
    }
}
