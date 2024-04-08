package calypsox.tk.report;

import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TaskReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;

//Project: MISSING_ISIN

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com >
 *
 */
public class SantMissingIsinReport extends TaskReport {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final String ROW_PROPERTY_TASK = "Task";
    public static final String ROW_PROPERTY_MISSING_ISIN = "MISSING_ISIN";

    /*
     * (non-Javadoc)
     * 
     * @see com.calypso.tk.report.Report#load(java.util.Vector)
     */
    @Override
    public ReportOutput load(Vector errors) {
        ReportOutput reportOutput = super.load(errors);

        if (reportOutput instanceof DefaultReportOutput) {
            TaskArray tasksToBeCompleted = new TaskArray();

            DefaultReportOutput defaultReportOutput = (DefaultReportOutput) reportOutput;
            ReportRow[] rows = defaultReportOutput.getRows();
            for (int iRow = 0; iRow < rows.length; iRow++) {
                ReportRow row = rows[iRow];
                Object rawTask = row.getProperty(ROW_PROPERTY_TASK);
                if (rawTask instanceof Task) {
                    Task task = (Task) rawTask;
                    addItem(row, task);
                    tasksToBeCompleted.add(task);
                }
            }

            markTasksAsCompleted(tasksToBeCompleted);
        }

        return reportOutput;
    }

    /**
     * Adds a SantMIssingIsinItem to the given row with the information from the
     * given task.
     * 
     * @param row
     *            The row to add the item to.
     * @param task
     *            The task to get the information from.
     */
    private void addItem(ReportRow row, Task task) {
        String comment = task.getComment();
        Map<String, String> attributes = SantMissingIsinUtil.getInstance()
                .getMapFromComment(comment);
        SantMissingIsinItem item = new SantMissingIsinItem();
        String isin = attributes
                .get(SantMissingIsinUtil.COMMENT_ATTRIBUTE_ISIN);
        item.setIsin(isin);
        String agent = attributes
                .get(SantMissingIsinUtil.COMMENT_ATTRIBUTE_AGENT);
        item.setAgent(agent);
        String counterparty = attributes
                .get(SantMissingIsinUtil.COMMENT_ATTRIBUTE_CPTY);
        item.setCpShortName(counterparty);
        String currentDate = attributes
                .get(SantMissingIsinUtil.COMMENT_ATTRIBUTE_DATE);
        item.setCurrentDate(currentDate);
        
        String contractid = attributes.get(SantMissingIsinUtil.COMMENT_ATTRIBUTE_CONTRACTID);
        SantMissingIsinUtil.getInstance().addContractInfo(item, contractid);

        row.setProperty(SantMissingIsinReport.ROW_PROPERTY_MISSING_ISIN, item);
    }

    /**
     * Marks the given tasks as COMPLETED.
     * 
     * @param tasks
     *            A TaskArray with all the tasks that should be completed.
     */
    private void markTasksAsCompleted(TaskArray tasks) {
        for (Task task : tasks) {
            task.setStatus(Task.COMPLETED);
        }

        try {
            DSConnection.getDefault().getRemoteBackOffice()
                    .saveAndPublishTasks(tasks, 0, null);
        } catch (CalypsoServiceException e) {
            String message = "Could not mark tasks as completed";
            Log.error(this, message, e);
        }
    }

}
