package calypsox.tk.util;

import calypsox.tk.bo.workflow.rule.SantAddOptimizerSendStatusAttributeCollateralRule;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR extends ScheduledTask
        implements SantCollateralOptimConstants {

    public static final long serialVersionUID = 1L;

    public static List<String> UPDATE_VALID_STATUSES = Arrays.asList("PRICED_NO_CALL", "PRICED_PAY", "PRICED_RECEIVE");

    public static final String FREQUENCY = "FREQUENCY";

    @Override
    public String getTaskInformation() {
        return "Add attribute status field in order to send entry to the optimizer";
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        StringBuffer scheduledTaskExecLogs = new StringBuffer();
        Task task = new Task();
        task.setObjectLongId(getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(getValuationDatetime());
        task.setUnderProcessingDatetime(getDatetime());
        task.setUndoTradeDatetime(getUndoDatetime());
        task.setDatetime(getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setSource(getType());

        boolean handlingOk = false;
        try {
            addMarginCallSendStatusAttribute(scheduledTaskExecLogs);

            handlingOk = scheduledTaskExecLogs.toString().length() == 0;
            if (!handlingOk) {
                task.setComment(scheduledTaskExecLogs.toString());
                task.setEventType("EX_" + BOException.EXCEPTION);
            } else {
                task.setEventType("EX_" + BOException.INFORMATION);
            }
            task.setCompletedDatetime(new JDatetime());
            task.setStatus(Task.NEW);

            TaskArray v = new TaskArray();
            v.add(task);
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0, null);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return handlingOk;
    }

    /**
     * ST Attributes Definition
     */
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(EXECUTION_MODE).domain(
                new ArrayList<String>(Arrays.asList(new String[]{EXECUTION_MODE_SOD, EXECUTION_MODE_REPROCESS}))));

        return attributeList;
    }

    /**
     * AAP
     *
     * @param scheduledTaskExecLogs
     */
    private void addMarginCallSendStatusAttribute(StringBuffer scheduledTaskExecLogs) {
        String actionToApply = "UPDATE";
        MarginCallReportTemplate template = new MarginCallReportTemplate();

        template.put(MarginCallReportTemplate.PROCESS_DATE, JDate.valueOf(getValuationDatetime(false)));
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES, Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION, Boolean.FALSE);

        // ExecutionContext context =
        // ExecutionContext.getInstance(ServiceRegistry.getDefaultContext(),
        // ServiceRegistry.getDefaultExposureContext(), template);

        // build margin call entries filter
        MarginCallConfigFilter mccFilter = new MarginCallConfigFilter();
        // Loads Margin Call Entries
        try {
            List<MarginCallEntryDTO> entriesList = getMCEntries(mccFilter);

            for (MarginCallEntryDTO mcEntryDTO : entriesList) {
                try {
                    if (isAccepted(mcEntryDTO, scheduledTaskExecLogs)) {

                        if (mcEntryDTO.getAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS) == null || ""
                                .equals(mcEntryDTO.getAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS))) {
                            // add and save optimizer send status
                            saveOptimizerSendStatus(mcEntryDTO, actionToApply, scheduledTaskExecLogs);
                        }
                    }
                } catch (Exception e) {
                    Log.error(SantAddOptimizerSendStatusAttributeCollateralRule.class.getName(), e);
                    scheduledTaskExecLogs.append("RemoteException while adding attribute on contract id "
                            + mcEntryDTO.getCollateralConfigId() + " : " + e.getMessage());
                }

            }
        } catch (RemoteException e1) {
            Log.error(this, "Error while trying to retrieve MarginCallEntries from Calypso");
            Log.error(this, e1); //sonar
        }
    }

    private List<MarginCallEntryDTO> getMCEntries(MarginCallConfigFilter mccFilter) throws RemoteException {
        List<MarginCallEntryDTO> entries = new ArrayList<MarginCallEntryDTO>();
        // load contractsIds
        List<Integer> contractIds = CollateralUtilities.getAllMarginCallContractIds(mccFilter);
        // load margin call entries
        if (!Util.isEmpty(contractIds)) {
            entries = CollateralManagerUtil.loadMarginCallEntriesDTO(contractIds, JDate.valueOf(this.getDatetime()));
        }
        return entries;
    }

    /**
     * Add optimize sending attributes
     *
     * @param mcEntryDTO
     */
    private void addOptimizerSendStatus(MarginCallEntryDTO mcEntryDTO) {
        if (mcEntryDTO != null) {
            mcEntryDTO.addAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS,
                    SantCollateralOptimConstants.OPTIMIZER_TO_BE_SENT_STATUS_VALUE);

            // TODO: delete with upgrade 1.6.3
            if (Util.isEmpty(mcEntryDTO.getCashPositions())) {
                mcEntryDTO.setCashPosition(new PreviousPositionDTO<CashPositionDTO>());
            }
        }
    }

    /**
     * save the entry with optimize sending attributes
     *
     * @param mcEntryDTO
     */
    private void saveOptimizerSendStatus(MarginCallEntryDTO mcEntryDTO, String actionToApply,
                                         StringBuffer scheduledTaskExecLogs) {
        if (mcEntryDTO != null) {

            addOptimizerSendStatus(mcEntryDTO);

            int entryId = 0;
            try {
                entryId = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer().save(mcEntryDTO,
                        actionToApply, TimeZone.getDefault());
                Log.info(ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR.class.getName(), "Entry with id " + entryId
                        + " successfully saved for the contract " + mcEntryDTO.getCollateralConfigId());

            } catch (RemoteException e) {
                Log.error(ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR.class.getName(), e);
                MarginCallEntryDTO reloadedEntryDTO = null;
                // TODO limit the second save just to the mismatch version
                // error
                try {
                    reloadedEntryDTO = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                            .loadEntry(mcEntryDTO.getId());

                    // check if status has not changed
                    if (UPDATE_VALID_STATUSES.contains(reloadedEntryDTO.getStatus())) {
                        // add optimizer send status
                        addOptimizerSendStatus(reloadedEntryDTO);

                        entryId = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                                .save(reloadedEntryDTO, actionToApply, TimeZone.getDefault());
                        Log.info(ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR.class.getName(),
                                "Entry with id " + entryId + " successfully saved for the contract "
                                        + mcEntryDTO.getCollateralConfigId());
                    }
                } catch (RemoteException re) {
                    Log.error(SantAddOptimizerSendStatusAttributeCollateralRule.class.getName(), re);
                    scheduledTaskExecLogs.append("RemoteException while adding attribute on contract id "
                            + reloadedEntryDTO.getCollateralConfigId() + " : " + re.getMessage());
                }
            }
        }
    }

    private boolean isAccepted(MarginCallEntryDTO marginCallEntry, StringBuffer scheduledTaskExecLogs) {
        CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                marginCallEntry.getCollateralConfigId());
        if (mcc == null) {
            Log.error(SantAddOptimizerSendStatusAttributeCollateralRule.class,
                    "Unable to get the margin call contract.");
            scheduledTaskExecLogs
                    .append("Unable to get the margin call contract on entry " + marginCallEntry.getId() + ".");
        }

        if (UPDATE_VALID_STATUSES.contains(marginCallEntry.getStatus())) {
            String frequency = mcc.getAdditionalField(FREQUENCY);
            try {
                if (!Util.isEmpty(frequency) && SantCollateralOptimUtil.acceptDateRuleBusinessDay(frequency,
                        marginCallEntry.getProcessDate())) {
                    if (!mcc.isExcludeFromOptimizer()) {
                        if (EXECUTION_MODE_SOD.equals(getAttribute(EXECUTION_MODE))) {
                            if (!EXCLUDE_FROM_SOD_OPTIMIZER_TRUE_VALUE
                                    .equalsIgnoreCase(mcc.getAdditionalField(EXCLUDE_FROM_SOD_OPTIMIZER))) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.error(SantAddOptimizerSendStatusAttributeCollateralRule.class.getName(), e);
                scheduledTaskExecLogs.append("Exception while testing frequency on entry " + marginCallEntry.getId()
                        + ": " + e.getMessage());
            }
        }
        return false;

    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        DSConnection ds = null;
        try {
            // Starts connection to DataServer.
            ds = ConnectionUtil.connect(args, "ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTRIBUTE");

            ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR st = new ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR();

            StringBuffer scheduledTaskExecLogs = new StringBuffer();
            st.addMarginCallSendStatusAttribute(scheduledTaskExecLogs);

        } catch (Exception e) {
            Log.error(ScheduledTaskSANT_OPTIMIZER_SEND_STATUS_ATTR.class, e); //sonar
        } finally {
            System.exit(0);
        }
    }
}
