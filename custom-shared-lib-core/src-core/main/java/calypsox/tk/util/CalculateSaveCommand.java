/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
/**
 *
 */
package calypsox.tk.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.CashPosition;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.PreviousPosition;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.command.ExecutionProfile;
import com.calypso.tk.collateral.command.impl.BaseCollateralCommand;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralContext;
import com.calypso.tk.refdata.ContextUserAction;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.TimeZone;

/**
 * @author aela
 *
 */
public class CalculateSaveCommand extends BaseCollateralCommand {
    protected MarginCallEntry entry;//Sonar
    protected List<CalculationTracker> processTracks;//Sonar
    protected DSConnection ds;//Sonar
    // BAU 5.2.0 - Temporary attribute to enable netted position fix
    protected String enableFix;//Sonar

    private CollateralContext collateralContext;

    public CalculateSaveCommand(ExecutionContext context) {
        super(context);
    }

    // BAU 5.2.0 - Temporary attribute to enable netted position fix
    public CalculateSaveCommand(ExecutionContext context, MarginCallEntry entry,
                                List<CalculationTracker> processTracks, String enableFix) {
        super(context);
        this.entry = entry;
        this.ds = DSConnection.getDefault();
        this.processTracks = processTracks;
        this.enableFix = enableFix;
    }

    public CalculateSaveCommand(ExecutionContext context, MarginCallEntry entry,
                                List<CalculationTracker> processTracks, String enableFix, CollateralContext collateralContext) {
        super(context);
        this.entry = entry;
        this.ds = DSConnection.getDefault();
        this.processTracks = processTracks;
        this.enableFix = enableFix;
        this.collateralContext = collateralContext;//Optimization 13/11/2017
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.collateral.command.CollateralCommand#execute(java.util.List)
     */
    @Override
    public void execute(List<String> errors) {

        if (this.collateralContext == null) {
            this.collateralContext = ServiceRegistry.getDefaultContext();//Optimization 13/11/2017
        }
        errors.clear();
        if (this.entry != null) {
            // calculate the enrty
            try {
                this.entry.calculate(errors);
                if (!Util.isEmpty(errors)) {
                    this.processTracks.add(new CalculationTracker(this.entry.getCollateralConfigId(),
                            CalculationTracker.KO, errorsToString(errors)));
                }

            } catch (Exception e) {
                this.processTracks.add(new CalculationTracker(this.entry.getCollateralConfigId(),
                        CalculationTracker.KO, e.getMessage()));
                Log.error(this, e);
            }
            // save the entry
            String actionToApply = null;
            try {

                // Get, from the collateral context, the wfw action
                // to apply for the calculation
                ContextUserAction ctxUserAction = this.collateralContext
                        .getUserAction(this.entry, "Price");//Optimization 13/11/2017
                if (ctxUserAction != null) {
                    actionToApply = ctxUserAction.getWorkflowAction();
                }

                if (Util.isEmpty(actionToApply)) {
                    this.processTracks.add(new CalculationTracker(this.entry.getCollateralConfigId(),
                            CalculationTracker.KO, "No available workflow action from status " + this.entry.getStatus()
                            + " for contract " + this.entry.getCollateralConfigId()));
                    return;
                }

                // BAU 5.2.0 - insert 0 cash position to fix save bug + Temporary attribute to enable netted position
                // fix
                // TODO: delete with module upgrade
                if (!Util.isEmpty(this.enableFix) && this.enableFix.equals(Boolean.toString(true))) {
                    if (Util.isEmpty(this.entry.toDTO().getCashPositions())) {
                        this.entry.setCashPosition(new PreviousPosition<CashPosition>(null, null));
                    }
                }
                int entryId = ServiceRegistry.getDefault(this.ds).getCollateralServer()
                        .save(this.entry.toDTO(), actionToApply, TimeZone.getDefault());
                Log.info(
                        this,
                        "New entry with id " + entryId + " successfully saved for the contract "
                                + this.entry.getCollateralConfigId());
                this.processTracks.add(new CalculationTracker(this.entry.getCollateralConfigId(),
                        CalculationTracker.OK, "New entry with id " + entryId + " successfully saved for the contract "
                        + this.entry.getCollateralConfigId()));
            } catch (Exception e) {
                this.processTracks.add(new CalculationTracker(this.entry.getCollateralConfigId(),
                        CalculationTracker.KO, "Can not apply worflow action " + actionToApply));
                Log.error(this, e);
            }
        }
        // appendErrors(scheduledTaskExecLogs, errors);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.collateral.command.CollateralCommand#getDescription()
     */
    @Override
    public String getDescription() {
        return "Custom job: calculate and save an entry";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.collateral.command.CollateralCommand#getExecutionProfile()
     */
    @Override
    public ExecutionProfile getExecutionProfile() {
        return ExecutionProfile.HIGH_CONCURRENCY;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.collateral.command.CollateralCommand#getType()
     */
    @Override
    public TYPE getType() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Append the list of errors to the given StrigBuffer
     *
     * @param scheduledTaskExecLogs
     * @param errors
     */
    private String errorsToString(List<String> errors) {
        StringBuffer errorsString = new StringBuffer("");
        if ((errors != null) && (errors.size() > 0)) {
            for (int i = 0; i < errors.size(); i++) {
                if (errorsString.length() > 0) {
                    errorsString.append(", ");
                }
                errorsString.append(errors.get(i));
            }
        }
        return errorsString.toString();
    }
}
