package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.Arrays;
import java.util.Vector;

public class SantRemoveOldP37MsgTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Remove Old P37 Messages.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        long longId = trade.getLongId();
        try {
            MessageArray message_id_desc = dsCon.getRemoteBackOffice().getMessages(null, "trade_id = " + longId + "AND message_type = 'P37_EXPORT'", "message_id DESC", null);
            if(null!=message_id_desc && !Util.isEmpty(message_id_desc.getMessages())){
                //Extract last message.
                BOMessage boMessage = message_id_desc.firstElement();
                message_id_desc.remove(boMessage);

                Arrays.stream(message_id_desc.getMessages()).forEach(boMessage1 -> {
                    try {
                        boolean remove = dsCon.getRemoteBackOffice().remove(boMessage);
                        if(remove){
                            Log.info(this,"Message removed: " + boMessage.getLongId());
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this,"Error removing message: " + boMessage.getLongId() + " Status: " + boMessage.getStatus() + " " + e.getMessage());
                    }
                });
            }
        } catch (CalypsoServiceException e) {
           Log.error(this,"Error loading messages for trade: " + trade.getLongId());
        }
        return true;
    }
}
