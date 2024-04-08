package calypsox.tk.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.FieldModification;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TransferArray;

public class STPTransferReport extends TransferReport {

	private static final String AUDIT_TRANSFER_QUERY = "(entity_class_name IN ('BOTransfer')) AND ( entity_id IN (@ids))";
	private static final String AUDIT_TRADE_QUERY = "(entity_class_name IN ('Trade')) AND ( entity_id IN (@ids))";
	private static final String TASKS_QUERY = "OBJECT_ID IN (@ids)";
	private static final String STATUS_FIELD = "_status";
	private static final String AUDIT_TRADE_FIELD_CUSTOM_TRANSFER_RULE = "_customTransferRuleB";
	private static final String AUDIT_TRADE_FIELD_TRANSFER_RULE = "__transferRules";
	private static final String CALYPSO_USER = "calypso_user";
	private static final String NETTED_TRANSFER = "Netted Transfer ";

	private Vector<AuditValue> audits;
	private TaskArray tasks;
	private List<AuditValue> auditValuesList;
	private List<Task> tasksList;
	private ConcurrentHashMap<String, BOTransfer> xfers;
	private HashMap<String, AuditValue> noSTPTransfersId;
	private HashMap<Long, String> transferCustomRule;
	private HashMap<Long, List<Task>> noSTPTransfersTasks;

	@Override
	public ReportOutput load(Vector errorMsgs) {
		Log.debug(this, "Entry method load(Vector errorMsgs)");

		DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

		// Se obtienen transfers de acuerdo a los criterios del template
		xfers = new ConcurrentHashMap<>();
		if (output != null) {
			ReportRow[] rows = output.getRows();
			noSTPTransfersId = new HashMap<>();
			transferCustomRule = new HashMap<>();
			for (int i = 0; i < rows.length; i++) {
				ReportRow row = rows[i];
				BOTransfer xfer = row.getProperty(ReportRow.TRANSFER);
				if (xfer != null) {
					xfers.put(String.valueOf(xfer.getLongId()), xfer);
				}

			}

			try {

				auditValuesList = new ArrayList<>();

				// Se recorre cada transfer
				for (String key : xfers.keySet()) {
					// Se revisa si la trade dueña de la transfer ha obtenido un cambio de SDI's
					if (isTransferRuleOperation(xfers.get(key))) {
						// Si la trade tiene un cambio de SDI's, se genera un audit dummy para todas sus transfers
						AuditValue av = createAndGetAV(xfers.get(key));
						// Se anaden las transfers con los audits dummy al mapa
						noSTPTransfersId.put(String.valueOf(key), av);
					}

				}

				// PREVENT ERROR OF LIMIT 1000 IDS ON CRITERIA IN
				int idCount = xfers.size();
				int pages = idCount / 1000;
				String ids = "";
				Log.debug(this, "transfer quantity= " + idCount);

				// Se obtienen audits de las transfers
				for (int i = 0; i <= pages; i++) {
					ids = String.join(",", xfers.keySet().stream().collect(Collectors.toList()).subList(i * 1000,
							idCount > 1000 ? (i + 1) * 1000 : i * 1000 + idCount));
					audits = DSConnection.getDefault().getRemoteTrade()
							.getAudit(AUDIT_TRANSFER_QUERY.replace("@ids", ids), (String) null, null);
					auditValuesList.addAll(audits);
					idCount -= 1000;
				}

				FieldModification field;
				String fieldName;
				String oldStatus;

				Log.debug(this, "Checking audit values");
				for (AuditValue audit : auditValuesList) {
					field = audit.getField();
					fieldName = field.getName();

					// Se revisa cada audit con la intencion de averiguar si el usuario ha aplicado una accion a la transfer
					if (!CALYPSO_USER.equalsIgnoreCase(audit.getUserName())
							&& STATUS_FIELD.equalsIgnoreCase(fieldName)) {
						Log.debug(this, "status change not made by " + CALYPSO_USER);
						Log.debug(this, "field=" + field);

						oldStatus = field.getOldValue();

						if (oldStatus != null) {

							ArrayList<TaskWorkflowConfig> wfs = getTransferWorkflow(audit.getEntityLongId(), oldStatus);
							if (wfs != null) {
								for (TaskWorkflowConfig wf : wfs) {
									// Si la transicion era STP y el usuario la aplico manualmente, se debe anadir transfer con respectivo audit al mapa
									if (wf.getUseSTPB()
											&& !noSTPTransfersId.containsKey(String.valueOf(audit.getEntityId()))) {
										noSTPTransfersId.put(String.valueOf(audit.getEntityId()), audit);
									}
								}
							}
						}

					}
				}

				if (noSTPTransfersId.size() > 0) {

					tasksList = new ArrayList<>();

					// PREVENT ERROR OF LIMIT 1000 IDS ON CRITERIA IN
					idCount = noSTPTransfersId.size();
					pages = idCount / 1000;

					List<String> l = new ArrayList<String>(noSTPTransfersId.keySet());
					for (int i = 0; i <= pages; i++) {

						ids = String.join(",",
								l.subList(i * 1000, idCount > 1000 ? (i + 1) * 1000 : i * 1000 + idCount));
						
						// Se obtienen las tasks de las transfers en el caso de que hayan comentarios en la transicion
						tasks = DSConnection.getDefault().getRemoteBO().getTasks(TASKS_QUERY.replace("@ids", ids),
								null);
						tasksList.addAll(tasks.toArrayList());
						idCount -= 1000;
					}
					noSTPTransfersTasks = new HashMap<>();
					if (tasksList != null && tasksList.size() > 0) {
						for (Task task : tasksList) {
							List<Task> tasks = noSTPTransfersTasks.get(task.getObjectLongId());
							if (tasks == null) {
								tasks = new ArrayList<>();
							}
							tasks.add(task);
							noSTPTransfersTasks.put(task.getObjectLongId(), tasks);
						}
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error trying to load audits values. " + e.getMessage());
			}
			setIsNotSTPProperties(rows);
		}

		Log.debug(this, "Exit method load(Vector errorMsgs)");
		return output;
	}

	/**
	 * Este metodo es para generar un audit dummy para las transfers que pertenecen
	 * a una trade con cambio de SDI
	 * @param xfer
	 * @return Dummy Audit Value
	 */
	private AuditValue createAndGetAV(BOTransfer xfer) {
		FieldModification fm = new FieldModification();
		fm.setName(AUDIT_TRADE_FIELD_TRANSFER_RULE);
		fm.setOldValue(xfer.getStatus().getStatus());
		AuditValue av = new AuditValue();
		av.setAction(Action.SETTLE);
		av.setField(fm);
		return av;
	}

	/**
	 * 
	 * @param transfer
	 * @return True si la trade tiene un cambio de SDI. False si no la tiene 
	 * @throws CalypsoServiceException
	 */
	private boolean isTransferRuleOperation(BOTransfer transfer) throws CalypsoServiceException {
		Trade trade = null;

		trade = DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());

		if (trade == null)
			return false;

		// Se obtiene audit de ta trade
		List<AuditValue> tradeAutid = getAuditTrade(trade);

		// Se obtienen audits con que indican cambio de SDI's (__transferRules)
		tradeAutid = tradeAutid.stream().filter(s -> isCustomRuleTransfer().test(s.getField().getName()))
				.collect(Collectors.toList());

		TransferArray transfers = DSConnection.getDefault().getRemoteBackOffice().getBOTransfers(trade.getLongId(), true);

		// Se revisan transfers canceladas en el caso de que el escenario sea un Split
		List<BOTransfer> canceled = transfers.stream().filter(x -> isCanceledTransfer().test(x.getStatus()))
				.collect(Collectors.toList());
		

		if (tradeAutid.isEmpty())
			return false;
		if (canceled.isEmpty())
			return false;
		return true;

	}

	public Predicate<String> isCustomRuleTransfer() {
		Predicate<String> isCustomRuleTransfer = new Predicate<String>() {

			@Override
			public boolean test(String t) {
				return t.equalsIgnoreCase(AUDIT_TRADE_FIELD_CUSTOM_TRANSFER_RULE)
						|| t.equalsIgnoreCase(AUDIT_TRADE_FIELD_TRANSFER_RULE);
			}
		};

		return isCustomRuleTransfer;
	}

	public Predicate<Status> isCanceledTransfer() {
		Predicate<Status> isCustomRuleTransfer = new Predicate<Status>() {

			@Override
			public boolean test(Status t) {
				return (t.equals(Status.CANCELED));
			}
		};

		return isCustomRuleTransfer;
	}

	void setIsNotSTPPropertiesInChildTransfer(ReportRow row, long nettedTransferId) {
		AuditValue audit = null;
		audit = noSTPTransfersId.get(String.valueOf(nettedTransferId));

		String message = NETTED_TRANSFER + nettedTransferId + " ";

		if (audit != null) {

			row.setProperty("STP", false);
			List<Task> transferTasks = noSTPTransfersTasks.get(nettedTransferId);
			if (transferTasks != null && transferTasks.size() > 0) {
				for (Task task : transferTasks) {

					int wfConfigId = task.getTaskWorkflowConfigId();

					TaskWorkflowConfig wfc = BOCache.getTaskWorkflowConfig(DSConnection.getDefault(), wfConfigId);

					if (wfc != null && !areAuditValuesEmpty(audit)
							&& audit.getAction().toString().equalsIgnoreCase(wfc.getPossibleAction().toString())
							&& audit.getField().getOldValue().equalsIgnoreCase(wfc.getStatus().toString())) {
						row.setProperty("COMMENT", message + task.getComment());
					} else {
						row.setProperty("COMMENT", message);
					}
				}
			}
		} else {
			row.setProperty("COMMENT", message);
		}
	}

	private void setIsNotSTPProperties(ReportRow[] rows) {
		BOTransfer transfer = null;
		AuditValue audit = null;
		for (ReportRow row : rows) {
			transfer = row.getProperty(ReportRow.TRANSFER);
			audit = noSTPTransfersId.get(String.valueOf(transfer.getLongId()));

			if (audit != null) {

				row.setProperty("STP", false);
				List<Task> transferTasks = noSTPTransfersTasks.get(transfer.getLongId());
				
				// Si la transfer tiene tasks, el reporte exhibe las acciones aplicadas a dichas transfers
				// El audit dummy sirve para segregar las transfers que tienen sus trades con cambios de SDI
				if (transferTasks != null && transferTasks.size() > 0 && !audit.getField().getName().equals(AUDIT_TRADE_FIELD_TRANSFER_RULE)) {
					for (Task task : transferTasks) {

						int wfConfigId = task.getTaskWorkflowConfigId();

						TaskWorkflowConfig wfc = BOCache.getTaskWorkflowConfig(DSConnection.getDefault(), wfConfigId);

						if (transfer.getNettedTransferLongId() > 0) {
							setIsNotSTPPropertiesInChildTransfer(row, transfer.getNettedTransferLongId());
						} else if (audit.getField().getName().equals(AUDIT_TRADE_FIELD_TRANSFER_RULE)) {
							row.setProperty("COMMENT", AUDIT_TRADE_FIELD_TRANSFER_RULE);
							row.setProperty("ACTION_PERFORMED", "There's no action performed");
							row.setProperty("PREVIOUS_STATUS", "There's no previous status");
							row.setProperty("RESULTING_STATUS", "There's no resulting status");
							row.setProperty("TASK_OWNER", "Task was not generated");
							row.setProperty("MODIFICATION_DATE", "Task was not generated");
						} else if (wfc != null && !areAuditValuesEmpty(audit)
								&& audit.getAction().toString().equalsIgnoreCase(wfc.getPossibleAction().toString())
								&& audit.getField().getOldValue().equalsIgnoreCase(wfc.getStatus().toString())) {
							row.setProperty("COMMENT", task.getComment());
							row.setProperty("ACTION_PERFORMED", audit.getAction().toString());
							row.setProperty("PREVIOUS_STATUS", wfc.getStatus().getStatus());
							row.setProperty("RESULTING_STATUS", wfc.getResultingStatus());
							row.setProperty("TASK_OWNER", task.getOwner());
							row.setProperty("MODIFICATION_DATE", task.getDatetime());
						} else if (transferCustomRule.get(transfer.getLongId()) != null) {

							row.setProperty("COMMENT", transferCustomRule.get(transfer.getLongId()));
							row.setProperty("ACTION_PERFORMED", audit.getAction().toString());
							row.setProperty("PREVIOUS_STATUS", audit.getField().getOldValue());
							row.setProperty("RESULTING_STATUS", audit.getField().getNewValue());
							row.setProperty("TASK_OWNER", "Task was not generated");
							row.setProperty("MODIFICATION_DATE", "Task was not generated");

						}
					}
				} else if (audit.getField().getName().equals(AUDIT_TRADE_FIELD_TRANSFER_RULE)) {
					row.setProperty("COMMENT", AUDIT_TRADE_FIELD_TRANSFER_RULE);
					row.setProperty("ACTION_PERFORMED", "There's no action performed");
					row.setProperty("PREVIOUS_STATUS", "There's no previous status");
					row.setProperty("RESULTING_STATUS", "There's no resulting status");
					row.setProperty("TASK_OWNER", "Task was not generated");
					row.setProperty("MODIFICATION_DATE", "Task was not generated");
				} else if (transfer.getNettedTransferLongId() > 0){
					setIsNotSTPPropertiesInChildTransfer(row, transfer.getNettedTransferLongId());
				}
			}
		}
	}

	boolean areAuditValuesEmpty(AuditValue audit) {
		if (audit.getAction() == null)
			return false;
		String action = audit.getAction().toString();
		String oldValue = audit.getField().getOldValue();

		return action.isEmpty() || oldValue == null;

	}

	private List<AuditValue> getAuditTrade(Trade trade) throws CalypsoServiceException {
		List<AuditValue> audits = new ArrayList<>();
		audits = DSConnection.getDefault().getRemoteTrade()
				.getAudit(AUDIT_TRADE_QUERY.replace("@ids", String.valueOf(trade.getLongId())), (String) null, null);

		return audits;

	}

	private ArrayList<TaskWorkflowConfig> getTransferWorkflow(long xferId, String oldStatus) {
		Log.debug(this, "Entry method getTransferWorkflow(long xferId, String oldStatus)");

		BOTransfer xfer = xfers.get(String.valueOf(xferId));
		String workflowType = BOTransferWorkflow.getWorkflowType(xfer);
		String productType = xfer.getProductType();
		LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), xfer.getProcessingOrg());

		Log.debug(this, "xfer=" + xfer);
		Log.debug(this, "workflowType=" + workflowType);
		Log.debug(this, "productType=" + productType);
		Log.debug(this, "po=" + po);

		ArrayList<TaskWorkflowConfig> wfs = null;

		try {

			wfs = BOCache.getPaymentWorkflow(DSConnection.getDefault(), productType, workflowType, po,
					Status.valueOf(oldStatus));

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		Log.debug(this, "Exit method getTransferWorkflow(long xferId, String oldStatus)");
		return wfs;
	}

}
