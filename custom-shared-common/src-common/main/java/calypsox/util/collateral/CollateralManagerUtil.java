/*
 *
 * Copyright (c) ISBAN: Ingeniera de Software Bancario, S.L.
 * All rights reserved.
 *
 */
/**
 *
 */
package calypsox.util.collateral;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallEntryFactory;
import com.calypso.tk.collateral.command.ACTION;
import com.calypso.tk.collateral.command.CollateralExecutor;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralContext;
import com.calypso.tk.refdata.ContextUserAction;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * Utility class for collateral management services
 *
 * @author aela
 *
 */
public class CollateralManagerUtil {
    /**
     * Load the margin call entry
     *
     * @param collateralConfigId
     * @param processingDate
     * @param valuatioDate
     * @param errors
     * @return
     * @throws RemoteException
     */
    public static MarginCallEntry loadMCEntry(final int mcEntryId,
                                              final ExecutionContext executionContext, final List<String> messages) {

        MarginCallEntryDTO mcEntryDTO = null;
        try {
            mcEntryDTO = ServiceRegistry.getDefault(DSConnection.getDefault())
                    .getCollateralServer().loadEntry(mcEntryId);
        } catch (final RemoteException e) {
            Log.error(Log.CALYPSOX, e);
            return null;
        }

        if (mcEntryDTO != null) {
            final ExecutionContext clone = executionContext.clone();
            //MIG_V14
            clone.setProcessDate(mcEntryDTO.getProcessDate());
            return MarginCallEntryFactory.getInstance(clone, null).createMarginCallEntry(
                    mcEntryDTO, messages);
        }

        return null;
    }

    /**
     * Load the margin call entry for the given contract and for the given
     * processing date. If the entry does not exist, then create one.
     *
     * @param collateralConfigId
     * @param processingDate
     * @param valuatioDate
     * @param errors
     * @return
     * @throws RemoteException
     */
    public static MarginCallEntry loadEntry(final int collateralConfigId,
                                            final ExecutionContext executionContext, final List<String> errors)
            throws RemoteException {
        // set the filter to load entries
        final List<Integer> contractIds = new ArrayList<Integer>();
        contractIds.add(collateralConfigId);
        final List<MarginCallEntry> entries = loadEntries(contractIds,
                executionContext, errors);
        if (Util.isEmpty(entries)) {
            errors.add("No Margin call entry found for the contract " + collateralConfigId);
            return null;
        } else if (entries.size() > 1) {
            errors.add("More than one entry found for the contract "
                    + collateralConfigId);
            return null;
        }
        return entries.get(0);
    }


    /**
     * @param collateralConfigId
     * @param processDate
     * @param errors
     * @return
     * @throws RemoteException
     */
    public static MarginCallEntry loadEntry(final int collateralConfigId,
                                            JDate processDate, final List<String> errors)
            throws RemoteException {
        // get the execution context
        ExecutionContext executionContext = getDefaultExecutionContext();
        // set the processing date criteria
        executionContext.getFilter().setProcessDate(processDate);
        // set the filter to load entries
        final List<Integer> contractIds = new ArrayList<Integer>();
        contractIds.add(collateralConfigId);
        final List<MarginCallEntry> entries = loadEntries(contractIds,
                executionContext, errors);
        if (Util.isEmpty(entries)) {
            errors.add("No Margin call entry found for the contract " + collateralConfigId);
            return null;
        } else if (entries.size() > 1) {
            errors.add("More than one entry found for the contract "
                    + collateralConfigId);
            return null;
        }
        return entries.get(0);
    }


    /**
     * Load the margin call entries for the given contracts and for the given
     * processing date.
     *
     * @param collateralConfigId
     * @param processingDate
     * @param valuatioDate
     * @param errors
     * @return
     * @throws RemoteException
     */
    public static List<MarginCallEntry> loadEntries(
            final List<Integer> contractIds,
            final ExecutionContext executionContext, final List<String> errors) {
        // set the filter to load entries
        MarginCallConfigFilter mccFilter = executionContext.getFilter();
        if (mccFilter == null) {
            mccFilter = new MarginCallConfigFilter();
        }
        // set the contracts ids to the filter
        if (!Util.isEmpty(contractIds)) {
            mccFilter.setContractIds(contractIds);
        }

        // The intraday context must be modified to load the eod contract
        // to prevent the entries from being saved, set the user action to null
        // (temporarily)
        final ExecutionContext clonedExecCtx = executionContext.clone();
        clonedExecCtx.setUserAction(null);
        // load the entries
        final CollateralManager colManager = getCollateralManager(clonedExecCtx);
        final List<MarginCallEntry> entries = colManager.createEntries(mccFilter,
                errors);

        // build the loaded entries
        final CollateralExecutor exec = colManager.getExecutor(
                ACTION.LOAD_BUILD, entries);
        exec.execute();

        return entries;
    }


    /**
     * @param contractIds
     * @param processDate
     * @param errors
     * @return
     * @throws RemoteException
     */
    public static List<MarginCallEntry> loadEntries(
            final List<Integer> contractIds,
            final JDate processDate, final List<String> errors)
            throws RemoteException {

        ExecutionContext executionContext = getDefaultExecutionContext();
        executionContext.setProcessDate(processDate);

        return loadEntries(contractIds, executionContext, errors);
    }

    /**
     * Get an instance of the collateral manager using the given context
     *
     * @param executionContext
     * @return an instance of the collateral manager using the given context
     */
    public static CollateralManager getCollateralManager(
            final ExecutionContext executionContext) {
        return CollateralManager.getInstance(executionContext);
    }

    /**
     * Calculate the given margin call entry
     *
     * @param entry
     * @param execContext
     * @param errors
     */
    public static void calculateEntry(final MarginCallEntry entry,
                                      final ExecutionContext execContext, final List<String> errors) {
        final List<MarginCallEntry> entries = new ArrayList<MarginCallEntry>();
        entries.add(entry);
        calculateEntries(entries, execContext, errors);
    }

    /**
     * Calculate the given entries, this will only calculate the margin calls
     * without saving the entries into the database
     *
     * @param entries margin call entries to calculate
     * @param execContext calculation context
     * @param errors
     */
    public static void calculateEntries(final List<MarginCallEntry> entries,
                                        final ExecutionContext execContext, final List<String> errors) {
        if (Util.isEmpty(entries)) {
            return;
        }

        final List<String> tempMessages = new ArrayList<String>();
        for (final MarginCallEntry entry : entries) {
            tempMessages.clear();
            if (entry != null) {
                // calculate the enrty
                try {
                    entry.calculate(tempMessages);
                    if (!Util.isEmpty(tempMessages)) {
                        if (entry.getPricingStatus().equals(
                                "PRICED WITH ERRORS")) {
                            errors.add(entry.getCollateralConfig().getName()
                                    + " not Priced, errors below.");
                            errors.addAll(tempMessages);
                            errors.add(""); // Empty line
                            continue;
                        } else {
                            errors.add(entry.getCollateralConfig().getName()
                                    + " Priced successfully, info messages below.");
                            errors.addAll(tempMessages);
                            errors.add(""); // Empty line
                        }
                    }

                } catch (final Exception e) {
                    Log.error(CollateralManagerUtil.class, e);
                }
            }

        }
    }

    /**
     * Save the given entries using the collateral context user action to apply
     * the final workflow action
     *
     * @param entries margin call entries to save
     * @param execContext save context
     * @param errors
     */
    public static void saveEntriesByUserAction(
            final List<MarginCallEntry> entries,
            final ExecutionContext execContext, final List<String> errors) {

        saveEntriesByUserAction(entries, execContext.getUserAction(), errors);
    }

    /**
     * Save the given entries using the collateral context user action to apply
     * the final workflow action
     *
     * @param entries margin call entries to save
     * @param execContext save context
     * @param errors
     */
    public static void saveEntriesByUserAction(
            final List<MarginCallEntry> entries, final String userAction,
            final List<String> errors) {
        if (Util.isEmpty(entries)) {
            return;
        }
        final List<String> tempMessages = new ArrayList<String>();
        for (final MarginCallEntry entry : entries) {
            tempMessages.clear();
            if (entry != null) {
                // save the entry
                String actionToApply = null;
                try {

                    // Get, from the collateral context, the wfw action
                    // to apply for the calculation
                    final ContextUserAction ctxUserAction = ServiceRegistry
                            .getDefaultContext().getUserAction(entry,
                                    userAction);
                    if (ctxUserAction != null) {
                        actionToApply = ctxUserAction.getWorkflowAction();
                    }

                    if (Util.isEmpty(actionToApply)) {
                        errors.add("No available workflow action from status "
                                + entry.getStatus() + " for contract "
                                + entry.getCollateralConfig().getName());
                        continue;
                    }

                    final int entryId = ServiceRegistry
                            .getDefault(DSConnection.getDefault())
                            .getCollateralServer()
                            .save(entry.toDTO(), actionToApply,
                                    TimeZone.getDefault());
                    Log.info(CollateralManagerUtil.class, "New entry with id "
                            + entryId + " successfully saved for the contract "
                            + entry.getCollateralConfig().getName());
                } catch (final Exception e) {
                    errors.add("Can not apply worflow action " + actionToApply);
                    Log.error(CollateralManagerUtil.class, e);
                    continue;
                }
            }
        }
    }

    /**
     * Save the given entries using the collateral context user action to apply
     * the final workflow action
     *
     * @param entries margin call entries to save
     * @param execContext save context
     * @param errors
     */
    public static void saveEntries(final List<MarginCallEntry> entries,
                                   final String wfwAction, final List<String> errors) {
        if (Util.isEmpty(entries) || Util.isEmpty(wfwAction)) {
            return;
        }

        final List<String> tempMessages = new ArrayList<String>();
        for (final MarginCallEntry entry : entries) {
            tempMessages.clear();
            if (entry != null) {
                // save the entry
                try {
                    final int entryId = ServiceRegistry
                            .getDefault(DSConnection.getDefault())
                            .getCollateralServer()
                            .save(entry.toDTO(), wfwAction,
                                    TimeZone.getDefault());
                    Log.info(CollateralManagerUtil.class, "New entry with id "
                            + entryId + " successfully saved for the contract "
                            + entry.getCollateralConfig().getName());
                } catch (final Exception e) {
                    errors.add("Can not apply worflow action " + wfwAction
                            + " for the mc entry " + entry.getId());
                    Log.error(CollateralManagerUtil.class, e);
                    continue;
                }
            }
        }
    }

    /**
     * Save the given entry using the collateral context user action to apply
     * the final workflow action
     *
     * @param entries margin call entry to save
     * @param execContext save context
     * @param errors
     */
    public static void saveEntry(final MarginCallEntry entry,
                                 final String wfwAction, final List<String> errors) {
        if ((entry == null) || Util.isEmpty(wfwAction)) {
            return;
        }

        if (entry != null) {
            // save the entry
            try {
                final int entryId = ServiceRegistry
                        .getDefault(DSConnection.getDefault())
                        .getCollateralServer()
                        .save(entry.toDTO(), wfwAction, TimeZone.getDefault());

                Log.info(CollateralManagerUtil.class, "New entry with id "
                        + entryId + " successfully saved for the contract "
                        + entry.getCollateralConfig().getName() + ", action = "
                        + wfwAction);
            } catch (final Exception e) {
                errors.add("Can not apply worflow action " + wfwAction
                        + " for the mc entry " + entry.getId());
                Log.error(CollateralManagerUtil.class, e);
            }
        }

    }

    /**
     *
     * Load the entry with a margin call Entry id
     *
     * @param mcEntryId
     * @param messages
     * @param executionContext
     * @return
     */
    public MarginCallEntry loadEntry(final int mcEntryId,
                                     final List<String> messages, final ExecutionContext executionContext) {

        MarginCallEntryDTO mcEntryDTO = null;
        try {
            mcEntryDTO = ServiceRegistry.getDefault(DSConnection.getDefault())
                    .getCollateralServer().loadEntry(mcEntryId);
        } catch (final RemoteException e) {
            Log.error(Log.CALYPSOX, e);
            return null;
        }

        final MarginCallEntry mcEntry = MarginCallEntryFactory.getInstance(executionContext, null).createMarginCallEntry(mcEntryDTO, messages);

        return mcEntry;
    }

    /**
     * Calculate and save the given margin call entries
     *
     * @param entries
     * @param execContext
     * @param errors
     */
    public static void calculateAndSaveEntries(
            final List<MarginCallEntry> entries,
            final ExecutionContext execContext, final List<String> errors) {
        if (Util.isEmpty(entries)) {
            return;
        }
        calculateEntries(entries, execContext, errors);
        if (!Util.isEmpty(errors)) {
            return;
        }
        saveEntriesByUserAction(entries, execContext, errors);
    }

    /**
     * Calculate and save the given margin call entries
     *
     * @param entries
     * @param execContext
     * @param errors
     */
    public static void calculateAndSaveEntry(final MarginCallEntry entry,
                                             final ExecutionContext execContext, final List<String> errors) {

        if (entry == null) {
            return;
        }

        List<MarginCallEntry> entries = new ArrayList<MarginCallEntry>();
        entries.add(entry);
        calculateEntries(entries, execContext, errors);
        if (!Util.isEmpty(errors)) {
            return;
        }
        saveEntriesByUserAction(entries, execContext, errors);
    }

    /**
     * @return
     */
    public static MarginCallReportTemplate getDefaultMarginCallTemplate() {
        // init the execution context from a template
        final MarginCallReportTemplate template = (MarginCallReportTemplate) ReportTemplate
                .getReportTemplate("MarginCall");
        if (template != null) {
            //template.put(MarginCallReportTemplate.PROCESS_DATE, processingDate);
            template.put(MarginCallReportTemplate.CONTRACT_IDS, "");
            template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES,
                    Boolean.FALSE);
            template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION,
                    Boolean.FALSE);
            //MIG V16 CONST
            //template.put(MarginCallReportTemplate.POSITION_BREAKDOWN,
            template.put("Positions breakdowns",
                    Arrays.asList(""));
        }
        return template;
    }

    /**
     * @return
     */
    public static ExecutionContext getDefaultExecutionContext() {
        ExecutionContext context = ExecutionContext.getInstance(
                ServiceRegistry.getDefaultContext(),
                ServiceRegistry.getDefaultExposureContext(), getDefaultMarginCallTemplate());
        MarginCallConfigFilter mccFilter = context.getFilter();
        if (mccFilter == null) {
            mccFilter = new MarginCallConfigFilter();
        }
        mccFilter.setContractIds(new ArrayList<Integer>());
        mccFilter.setUseValuationFrequency(false);

        return context;
    }


    /**
     * @param contactIds
     * @param processDate
     * @param contextId
     * @return
     * @throws CollateralServiceException
     */
    public static List<MarginCallEntryDTO> loadMarginCallEntriesDTO(List<Integer> contactIds, JDate processDate, int contextId) throws CollateralServiceException {
        return ServiceRegistry.getDefault().getCollateralServer()
                .loadEntries(contactIds, processDate, contextId, false);
    }

    /**
     * @param contactIds
     * @param processDate
     * @param contextId
     * @return
     * @throws CollateralServiceException
     */
    public static List<MarginCallEntryDTO> loadMarginCallEntriesDTO(List<Integer> contactIds, JDate processDate, int contextId, boolean lightweight) throws CollateralServiceException {
        return ServiceRegistry.getDefault().getCollateralServer()
                .loadEntries(contactIds, processDate, contextId, lightweight);
    }

    /**
     * @param contactIds
     * @param processDate
     * @return
     * @throws CollateralServiceException
     */
    public static List<MarginCallEntryDTO> loadMarginCallEntriesDTO(List<Integer> contactIds, JDate processDate) throws CollateralServiceException {
        return loadMarginCallEntriesDTO(contactIds, processDate, ServiceRegistry.getDefaultContext().getId());
    }


    /**
     * @param contactIds
     * @param processDate
     * @return
     * @throws CollateralServiceException
     */
    public static List<MarginCallEntryDTO> loadMarginCallEntriesDTO(List<Integer> contactIds, JDate processDate, boolean lightweight) throws CollateralServiceException {
        return loadMarginCallEntriesDTO(contactIds, processDate, ServiceRegistry.getDefaultContext().getId(), lightweight);
    }

    /**
     * @param mcFilter
     * @param context
     * @return
     * @throws CollateralServiceException
     */
    public static List<CollateralConfig> loadCollateralConfigs(MarginCallConfigFilter mcFilter, CollateralContext context) throws CollateralServiceException {
        return ServiceRegistry.getDefault().getCollateralDataServer()
                .getMarginCallConfigs(mcFilter, context);
    }

    /**
     * @param mcFilter
     * @return
     * @throws CollateralServiceException
     */
    public static List<CollateralConfig> loadCollateralConfigs(MarginCallConfigFilter mcFilter) throws CollateralServiceException {
        return loadCollateralConfigs(mcFilter, ServiceRegistry.getDefaultContext());
    }

}
