package calypsox.tk.bo.workflow.rule;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;


public class SantSetAttributesXFerAndUnderlyingsTransferRule
implements WfTransferRule
{
	protected static String RULE_NAME = "SantSetAttributesXFerAndUnderlyings";

	public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
		return true;
	}

	public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, RULE_NAME);

		if (Util.isEmpty(map)) {
			map = getKeywordToValueMap(wc, transfer, oldTransfer, messages, dsCon, excps, task, dbCon, events);
		}
		
		TransferArray underlyings = transfer.getUnderlyingTransfers();
		if(Util.isEmpty(underlyings)){
            try {
            	underlyings = DSConnection.getDefault().getRemoteBO().getNettedTransfers(transfer.getLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading Netting Transfer for BOTransfer: " + transfer.getLongId());
            }
        }
		
		if (!Util.isEmpty(map))    {
			Iterator i = map.keySet().iterator();
			while (i.hasNext())      {
				String attribute = (String)i.next();
				transfer.setAttribute(attribute, (String)map.get(attribute));

				for (BOTransfer currentTransfer : underlyings) {
					currentTransfer = cloneTransferIfInmutable(currentTransfer);
					currentTransfer.setAttribute(attribute, (String)map.get(attribute));
				}
			}
		}
		return true;
	}
	
	public String getDescription() {
		return "This rule updates transfer keywords and does the same for its underlyings.\n";
	}
	
	public String getName() {
		return RULE_NAME;
	}

	public Map<String, String> getKeywordToValueMap(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
		Map<String, String> map = null;
		String s = wc.getComment();
		if (!Util.isEmpty(s))    {
			s = s.trim();
			if ((s.startsWith("{")) || (s.endsWith("}")))      {
				s = s.substring(1, s.length() - 1);
				map = Util.stringToMap(s);
			}
		}
		return map;
	}

	private BOTransfer cloneTransferIfInmutable(BOTransfer transfer){
		BOTransfer mutableTransfer=null;
		if(!transfer.isMutable()){
			try {
				mutableTransfer= (BOTransfer) transfer.clone();
			} catch (CloneNotSupportedException exc) {
				Log.warn(this.getClass().getSimpleName(),exc.getCause());
			}
		}else{
			mutableTransfer=transfer;
		}
		return mutableTransfer;
	}
}
