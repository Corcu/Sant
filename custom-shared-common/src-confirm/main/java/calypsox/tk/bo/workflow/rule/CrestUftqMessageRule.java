package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.Vector;

public class CrestUftqMessageRule implements WfMessageRule {

    private static final String QUERY_MSG_ATTRIBBUTE = "SELECT MAX(ATTR_VALUE) FROM mess_attributes WHERE ATTR_NAME='Crest_UFTQ'";
    private static final String DESCRIPTION = "This rule set the Crest_UFTQ attribute used in CREST message templates";
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String paramTemplate = wc.getRuleAdditionalInfo("CrestUftq").getRuleParam();
        if(paramTemplate != null && message.getTemplateName().contains(paramTemplate)) {
            String uftqNumber = String.format("%07d", getCrestUFTQAttributeValue(message, dsCon));
            message.setAttribute("Crest_UFTQ", String.valueOf(uftqNumber));
        }
        return true;
    }

    public static int getCrestUFTQAttributeValue(BOMessage message, DSConnection dsCon){
        int addOneUftqNumber = 1;
        try {
            Vector<Vector<String>> selectUFTQAttribute = (Vector<Vector<String>>) dsCon.getRemoteAccess().executeSelectSQL(QUERY_MSG_ATTRIBBUTE, null);
            if (selectUFTQAttribute.get(selectUFTQAttribute.size() - 1).toArray()[0] != null) {
                addOneUftqNumber = Integer.parseInt(String.valueOf(selectUFTQAttribute.get(selectUFTQAttribute.size() - 1).toArray()[0])) + 1;
                if(addOneUftqNumber>9999999){
                    addOneUftqNumber = addOneUftqNumber - 9999999;
                }

            }
        } catch (CalypsoServiceException e) {
            Log.error(CrestUftqMessageRule.class, e);
        }
        return addOneUftqNumber;
    }
}
