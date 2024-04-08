package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class LoaderMessageRule extends com.calypso.tk.bo.workflow.rule.LoaderMessageRule {

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean loaderReturnValue = super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        if (isReprocessedAndCompletedMessage(loaderReturnValue,message)) {
            updateUploadObjectStatus(message);
        }
        return loaderReturnValue;
    }

    /**
     * @see this.isReprocessedAndCompletedMessage
     * For reprocessed msgs, object status need to be updated and set as COMPLETED.
     * Only for REPROCESSED ones, UploaderEngine's already does this when an object is fully persisted.
     * If you don't like this crap, please feel free to remove it and achieve the same by using only WORKFLOW configurations.
     * @param boMessage
     */
    private void updateUploadObjectStatus(BOMessage boMessage) {
        if (boMessage.getTradeLongId() > 0L && !isMessageCompleted(boMessage)) {
            boMessage.setAttribute("UploadObjectStatus", "Completed");
        }
    }

    /**
     *
     * @param loaderReturnValue
     * @return true in case of being a reprocessed successful object load. Reprocessed means that first engine's
     * integration wasn't successful, so the message already has a valid ID.
     */
    private boolean isReprocessedAndCompletedMessage(boolean loaderReturnValue, BOMessage boMessage){
        return !this.isAuditProcessApplicable(boMessage) && loaderReturnValue;
    }
}
