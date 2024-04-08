package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class SetReprocessTagToCDUFMessageRule implements WfMessageRule {


    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Add reprocess tag inside AdviceDoc's CDUF to allow the DUP Validator to whitelist desired validations under reprocessing";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        AdviceDocument adviceDoc=getAdviceDoc(message);
        if(adviceDoc!=null){
            setReprocessTagToAdviceDoc(adviceDoc);
        }
        return true;
    }

    /**
     * In beta. The doc must be unmarshalled to be able to add the keyword value.
     * @param document
     */
    protected void setReprocessTagToAdviceDoc(AdviceDocument document){
        StringBuffer doc=document.getDocument();
        String toReplace="</TradeKeywords>";
        String replacement="<Keyword>\n"+
                "                <KeywordName>IsReprocess</KeywordName>\n" +
                "                <KeywordValue>true</KeywordValue>\n" +
                "                </Keyword>\n"+
                "                </TradeKeywords>\n";
        if(!doc.toString().contains("<KeywordName>IsReprocess</KeywordName>")) {
            String reprocessDoc = doc.toString().replaceAll(toReplace, replacement);
            document.setDocument(new StringBuffer(reprocessDoc));
            try {
                DSConnection.getDefault().getRemoteBO().save(document);
            } catch (CalypsoServiceException exc) {
                Log.error(this, exc.getCause());
            }
        }
    }


    private AdviceDocument getAdviceDoc(BOMessage message){
        AdviceDocument doc=null;
        try {
            doc=DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(message.getLongId(),new JDatetime());
        } catch (CalypsoServiceException exc) {
            Log.error(this,exc.getCause());
        }
        return doc;
    }
}
