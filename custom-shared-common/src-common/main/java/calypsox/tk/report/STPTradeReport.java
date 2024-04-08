package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkFlowUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

import java.util.*;

public class STPTradeReport extends TradeReport {

    private static final String AUDIT_QUERY = "(entity_class_name IN ('Trade')) AND ( entity_id IN (@ids))";
    private static final String TASKS_QUERY = "trade_id IN (@ids)";
    private static final String STATUS_FIELD = "_status";
    private static final String CALYPSO_USER = "calypso_user";

    private Vector<AuditValue> audits;
    private TaskArray tasks;
    private List<AuditValue> auditValuesList;
    private List<Task> tasksList;
    private List<String> tradeIds;
    private HashMap<String, AuditValue> noSTPTradesId;
    private HashMap<Long, List<Task>> noSTPTradeTasks;

    @Override
    public ReportOutput load(Vector errorMsgs) {

        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

        tradeIds = new ArrayList<>();
        if (output != null) {
            ReportRow[] rows = output.getRows();
            for (int i = 0; i < rows.length; i++) {
                ReportRow row = rows[i];
                Trade trade = row.getProperty(ReportRow.TRADE);
                if (trade != null) {
                    tradeIds.add(String.valueOf(trade.getLongId()));
                }
            }

            try {

                noSTPTradesId = new HashMap<>();
                auditValuesList = new ArrayList<>();

                //PREVENT ERROR OF LIMIT 1000 IDS ON CRITERIA IN
                int idCount = tradeIds.size();
                int pages = idCount / 1000;
                String ids = "";
                for (int i = 0; i <= pages; i++) {
                    ids = String.join(",", tradeIds.subList(i * 1000, idCount > 1000 ? (i + 1) * 1000 : i * 1000 + idCount));
                    audits = DSConnection.getDefault().getRemoteTrade().getAudit(AUDIT_QUERY.replace("@ids", ids), (String) null, null);
                    auditValuesList.addAll(audits);

                    idCount -= 1000;
                }

                FieldModification field;
                String fieldName;
                String oldStatus;
                for (AuditValue audit : auditValuesList) {
                    field = audit.getField();
                    fieldName = field.getName();

                    if (!CALYPSO_USER.equalsIgnoreCase(audit.getUserName()) && STATUS_FIELD.equalsIgnoreCase(fieldName)) {
                        oldStatus = field.getOldValue();

                        ArrayList<TaskWorkflowConfig> wfs = WorkFlowUtil.getTradeWorkflow(audit.getEntityId(), Status.valueOf(oldStatus));
                        if (wfs != null) {
                            for (TaskWorkflowConfig wf : wfs) {
                                if (wf.getUseSTPB()) {
                                    noSTPTradesId.put(String.valueOf(audit.getEntityId()), audit);
                                }
                            }
                        }
                    }
                }

                if (noSTPTradesId.size() > 0) {

                    tasksList = new ArrayList<>();

                    //PREVENT ERROR OF LIMIT 1000 IDS ON CRITERIA IN
                    idCount = noSTPTradesId.size();
                    pages = idCount / 1000;

                    List<String> l = new ArrayList<String>(noSTPTradesId.keySet());
                    for (int i = 0; i <= pages; i++) {

                        ids = String.join(",", l.subList(i * 1000, idCount > 1000 ? (i + 1) * 1000 : i * 1000 + idCount));
                        tasks = DSConnection.getDefault().getRemoteBO().getTasks(TASKS_QUERY.replace("@ids", ids), null);
                        tasksList.addAll(tasks.toArrayList());
                        idCount -= 1000;
                    }
                    noSTPTradeTasks = new HashMap<>();
                    if (tasksList != null && tasksList.size() > 0) {
                        for (Task task : tasksList) {
                            List<Task> tasks = noSTPTradeTasks.get(task.getTradeId());
                            if (tasks == null) {
                                tasks = new ArrayList<>();
                            }
                            tasks.add(task);
                            noSTPTradeTasks.put(task.getTradeId(), tasks);
                        }
                    }
                }

            } catch (CalypsoServiceException e) {
                //TODO: CREATE LOGS
                e.printStackTrace();
            }

            setIsNotSTPProperties(rows);
        }
        return output;
    }

    private void setIsNotSTPProperties(ReportRow[] rows) {
        Trade trade = null;
        AuditValue audit = null;
        for (ReportRow row : rows) {
            trade = row.getProperty(ReportRow.TRADE);
            audit = noSTPTradesId.get(String.valueOf(trade.getLongId()));

            if (audit != null) {

                row.setProperty("STP", false);
                List<Task> tradeTasks = noSTPTradeTasks.get(trade.getLongId());
                if (tradeTasks != null && tradeTasks.size() > 0) {
                    for (Task task : tradeTasks) {

                        int wfConfigId = task.getTaskWorkflowConfigId();

                        TaskWorkflowConfig wfc = BOCache.getTaskWorkflowConfig(DSConnection.getDefault(), wfConfigId);

                        if (wfc != null && audit.getUserName().equalsIgnoreCase(task.getOwner())
                                && audit.getAction().toString().equalsIgnoreCase(wfc.getPossibleAction().toString())
                                && audit.getField().getOldValue().equalsIgnoreCase(wfc.getStatus().toString())) {
                            row.setProperty("COMMENT", task.getComment());
                            row.setProperty("ACTION_PERFORMED", audit.getAction().toString());
                            row.setProperty("PREVIOUS_STATUS", wfc.getStatus().getStatus());
                            row.setProperty("RESULTING_STATUS", wfc.getResultingStatus());
                            row.setProperty("TASK_OWNER", task.getOwner());
                            row.setProperty("MODIFICATION_DATE", task.getDatetime());
                        }
                    }
                }
            }
        }
    }
}
