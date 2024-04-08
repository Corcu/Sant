package calypsox.tk.bo;

import com.calypso.engine.advice.MessageEngine;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOMessageHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.AuditFilter;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author aalonsop
 */
public class BOCALYPSOCONFIRMMessageHandler extends BOMessageHandler {

    @Override
    public boolean isMessageAccepted(BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, PSEvent event, List<Task> exceptions, DSConnection ds) {
        boolean isAccepted = super.isMessageAccepted(message, oldMessage, trade, transfer, event, exceptions, ds);
        if (isAccepted) {
            createCancelMsg(trade, oldMessage);
        }
        return isAccepted;
    }

    void createCancelMsg(Trade trade, BOMessage oldMessage) {
        if (isProductTypeAccepted(trade) && isCPTYAmendment(trade, oldMessage)) {
            try {
                BOMessage cancelMsg = (BOMessage) oldMessage.clone();
                cancelMsg.setLongId(0L);
                cancelMsg.setLinkedLongId(oldMessage.getLongId());
                cancelMsg.setSubAction(Action.CANCEL);
                cancelMsg.setAction(Action.NEW);
                cancelMsg.setStatus(Status.S_NONE);
                JDatetime currentTime=new JDatetime();
                cancelMsg.setCreationDate(currentTime);
                cancelMsg.setCreationSystemDate(currentTime);
                cancelMsg.setUpdateDatetime(currentTime);
                cancelMsg.setTradeUpdateDatetime(currentTime);
                DSConnection.getDefault().getRemoteBO().save(cancelMsg, 0L, MessageEngine.ENGINE_NAME);
            } catch (CloneNotSupportedException | CalypsoServiceException exc) {
                Log.error(this, exc.getCause());
            }
        }
    }

    boolean isProductTypeAccepted(Trade trade) {
        return trade.getProduct() instanceof Bond;
    }

    boolean isCPTYAmendment(Trade trade, BOMessage oldMessage) {
        boolean res = false;
        if(oldMessage!=null) {
            AuditFilter af = new AuditFilter();
            String cptyAuditField = "_counterPartyId";
            Set<String> fields = new TreeSet<>();
            fields.add(cptyAuditField);
            af.setFields(fields);
            Trade oldTrade = null;
            try {
                oldTrade = DSConnection.getDefault().getRemoteTrade().undo(trade, oldMessage.getTradeVersion());
            } catch (Exception exc) {
                Log.error(this, exc);
            }
            if (oldTrade != null) {
                res = af.accept(AuditFilter.OP_IN, oldTrade, trade, new ArrayList<>());
            }
        }
        return res;
    }

    @Override
    public boolean isSameReceiverRequired() {
        return false;
    }
}
