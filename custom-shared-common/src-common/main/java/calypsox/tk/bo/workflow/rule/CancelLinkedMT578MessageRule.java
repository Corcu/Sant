package calypsox.tk.bo.workflow.rule;

import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;

import java.util.List;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CancelLinkedMT578MessageRule implements WfMessageRule {

    private final String msgFunctionAttr="Message_Function";
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Cancel linked NEWM MT578 messages";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if(isCancOrRemoMsg(message)){
            cancelLinkedMT578(message,dsCon);
        }
        return true;
    }

    private void cancelLinkedMT578(BOMessage message,DSConnection dsCon){
         String sqlWhereTemplate="bo_message.template_name IN (?) AND " +
                "EXISTS (SELECT 1 FROM mess_attributes WHERE  mess_attributes.message_id = bo_message.message_id  AND attr_name='Common_Reference' AND  (attr_value = ?))";
         String commonRef=message.getAttribute("Common_Reference");
         if(!Util.isEmpty(commonRef)){
             List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable("MT578");
             bindVariables=CustomBindVariablesUtil.addNewBindVariableToList(commonRef,bindVariables);
             getAndSaveMessages(dsCon,sqlWhereTemplate,bindVariables);
            }
    }

    private void getAndSaveMessages(DSConnection dsCon, String where,List<CalypsoBindVariable> bindVariables){
        try {
            if(dsCon!=null) {
                MessageArray messages = dsCon.getRemoteBO().getMessages(where, bindVariables);
                messages=setCancelActionAndFilterIfApplicable(messages, dsCon);
                dsCon.getRemoteBO().saveMessages(0,"",messages,new TaskArray());
            }
        } catch (CalypsoServiceException exc) {
            Log.error(this,"Bulk message saving failed, trying one by one saving...",exc.getCause());
        }
    }

    private MessageArray setCancelActionAndFilterIfApplicable(MessageArray messages,DSConnection dsCon){
        MessageArray filteredMessages=new MessageArray();
        for(BOMessage message:messages){
            Action action=Action.CANCEL;
            message=cloneMsgIfInmutable(message);
            if(message!=null&&isNotCancOrRemoMsg(message)
                    &&BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, dsCon, null)){
                message.setAction(Action.CANCEL);
                filteredMessages.add(message);
                Log.debug(this,"Action cancel set to msg id: "+message.getLongId());
            }
        }
        return filteredMessages;
    }

    private boolean isNotCancOrRemoMsg(BOMessage message) {
       return !isCancOrRemoMsg(message);
    }
    private boolean isCancOrRemoMsg(BOMessage message) {
        String msgFunction=message.getAttribute(msgFunctionAttr);
        return  "CANC".equalsIgnoreCase(msgFunction)||"REMO".equalsIgnoreCase(msgFunction);
    }

    private BOMessage cloneMsgIfInmutable(BOMessage message){
        BOMessage mutableMsg=null;
        if(!message.isMutable()){
            try {
                mutableMsg= (BOMessage) message.clone();
            } catch (CloneNotSupportedException exc) {
                Log.warn(this,exc.getCause());
            }
        }else{
            mutableMsg=message;
        }
        return mutableMsg;
    }
    /*private void saveInBatch(MessageArray messages,DSConnection dsCon){
        for(BOMessage message:messages){
            try {
                dsCon.getRemoteBO().save(message,0,"");
            } catch (CalypsoServiceException exc) {
                Log.error(this,"Msg Id: "+message.getLongId()+" saving failed",exc.getCause());
            }
        }
    }*/
}
