package calypsox.tk.bo.workflow.rule;

import java.util.ArrayList;
import java.util.List;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.impl.NotificationFactory;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventCollateralStatement;
import com.calypso.tk.service.DSConnection;

public class SantGenerateEventCollateralRule extends BaseCollateralWorkflowRule {
	
	
	private static final String TRIPARTY_CANCELNEW = "TRIPARTY_CANCELNEW";
	private static final String MC_NOTIFICATION = "MC_NOTIFICATION";
	private static final String MC_STATEMENT = "MC_STATEMENT";

	@Override
	public String getDescription() {

		return "GenerateEvent PSEventCollateralStatement for CANCEL/NEW";
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {

		return true;
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		WorkflowResult wfr = new WorkflowResult();
		
		
		 List<PSEventCollateralStatement> listEvents = new ArrayList<PSEventCollateralStatement>();
         
         PSEventCollateralStatement collateralEventNotif =  NotificationFactory.createStatement(((MarginCallEntry) entry), Action.valueOf(TRIPARTY_CANCELNEW));
         collateralEventNotif.setMessageType(MC_NOTIFICATION);
        
         PSEventCollateralStatement collateralEventStat = NotificationFactory.createStatement(((MarginCallEntry) entry), Action.valueOf(TRIPARTY_CANCELNEW));
         collateralEventStat.setMessageType(MC_STATEMENT);
        
         listEvents.add(collateralEventNotif);
         listEvents.add(collateralEventStat);
        
         try {
               Log.info(this, "Publishing PSEventCollateralStatement CANCELNEW");
               dsCon.getRemoteTrade().saveAndPublish(listEvents);
         } catch (CalypsoServiceException e) {
               Log.error(this, "Error publishing event: " + e);
               return wfr;
         }

		wfr.success();
		return wfr;
	}
	

}
