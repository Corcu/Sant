package calypsox.tk.bo.workflow.rule;

import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TaskArray;

/**
 * Rule to validate if thresold is over
 *
 * @author mgarcsan
 */
public class SantCheckGRMCollateralRule extends BaseCollateralWorkflowRule {

	public static String THRESHOLD_GRM_LIMIT = "thresholdGRM_limit";
	public static String THRESHOLD_GRM_CONTRACT_TYPE = "thresholdGRM_contractType";

	/**
	 * Returns the rule description
	 *
	 * @return
	 */
	@Override
	public String getDescription() {
		return "This rule checks if the thresold limit has been violated creates an information task";
	}

	/**
	 * Returns true if the action is rule is applicable
	 *
	 * @param paramTaskWorkflowConfig
	 * @param entry
	 * @param paramEntityState1
	 * @param paramEntityState2
	 * @param paramList
	 * @param paramDSConnection
	 * @param paramList1
	 * @param paramTask
	 * @param paramObject
	 * @param paramList2
	 * @return
	 */
	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig,
			MarginCallEntry entry, EntityState paramEntityState1,
			EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1,
			Task paramTask, Object paramObject, List<PSEvent> paramList2) {
		return true;
	}

	/**
	 * Applies the action creating the linked task to the margin call
	 */
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig,
			MarginCallEntry entry, DSConnection dsCon) {
		Log.info(SantCheckGlobalMTACollateralRule.class,
				"SantCheckGRMCollateralRule Update - Start");

		WorkflowResult wfr = new WorkflowResult();

		if (!isNotified(entry) && checkThreshold(entry)) {

			try {

				// Create a new tasks
				TaskArray tasks = new TaskArray();
				Task task = new Task();
				task.setObjectLongId(entry.getCollateralConfigId());
				task.setObjectClassName("MarginCallEntry");
				task.setEventClass("Exception");
				task.setNewDatetime(entry.getLastUpdate());
				task.setDatetime(entry.getLastUpdate());
				task.setOwner(entry.getLastUser());
				task.setPriority(1);
				task.setId(0L);
				task.setStatus(0);
				task.setEventType("EX_INFORMATION");
				task.setSource("SantCheckGRM");
				task.setComment("Margin call " + entry.getCollateralConfigId()
						+ " Net Balance  ("
						+ Math.abs(entry.getNetBalance())
						+ ") has passed the threshold amount ("
						+ Math.abs(entry.getThresholdAmount()) + ")");
				tasks.add(task);

				// Save Tasks in BBDD

				DSConnection.getDefault().getRemoteBO()
						.saveAndPublishTasks(tasks, 0, (String) null);
			} catch (Exception exception) {
				Log.error(this, exception);

			}
		}

		Log.info(SantCheckGRMCollateralRule.class,
				"SantCheckGRMCollateralRule Update- End");
		wfr.success();

		return wfr;
	}

	/**
	 * Checks thresold limit
	 * 
	 * @param entry
	 * @return
	 */
	protected boolean checkThreshold(MarginCallEntry entry) {
		Log.info(SantCheckGRMCollateralRule.class,
				"SantCheckGRMCollateralRule Check - Start");
		boolean result = false;

		double mTm = Math.abs(entry.getNetBalance());

		double threshold = Math.abs(entry.getThresholdAmount());

		Vector<String> thresholdLimits = LocalCache.getDomainValues(
				DSConnection.getDefault(), THRESHOLD_GRM_LIMIT);

		Vector<String> contractTypes = LocalCache.getDomainValues(
				DSConnection.getDefault(), THRESHOLD_GRM_CONTRACT_TYPE);

		if (contractTypes != null
				&& !contractTypes.isEmpty()
				&& contractTypes.contains(entry.getCollateralConfig()
						.getContractType()) && thresholdLimits != null
				&& !thresholdLimits.isEmpty()) {

			String thresholdLimit = thresholdLimits.get(0);

			if (!Util.isEmpty(thresholdLimit)) {

				double limitPercent = Double.parseDouble(thresholdLimit);

				double limit = threshold - limitPercent * (threshold) / 100;

				if (mTm >= limit) {
					result = true;
				}

			}
		}

		return result;
	}

	/**
	 * Checks if exists a previous notification for the margin call
	 * 
	 * @param entry
	 * @return
	 */
	protected boolean isNotified(MarginCallEntry entry) {

		boolean result = false;

		try {
			TaskArray tasks = DSConnection
					.getDefault()
					.getRemoteBO()
					.getTasks(
							"object_id = "
									+ entry.getCollateralConfigId()
									+ " AND object_classname = 'MarginCallEntry' "
									+ "AND source = 'SantCheckGRM' AND event_type ='EX_INFORMATION' "
									+ "AND event_class ='"
									+ Task.EXCEPTION_EVENT_CLASS
									+ "' AND task_status <> " + Task.COMPLETED,
							null);

			result = !tasks.isEmpty();

		} catch (CalypsoServiceException exception) {
			Log.error(this, exception);
		}

		return result;

	}

}