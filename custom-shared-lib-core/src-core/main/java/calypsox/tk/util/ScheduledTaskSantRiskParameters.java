/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.util.riskparameters.SantRiskParameter;
import calypsox.tk.util.riskparameters.SantRiskParameterBuilder;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.*;

public class ScheduledTaskSantRiskParameters extends ScheduledTask {

    // START OA 27/11/2013
    // Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
    // InvalidClassExceptions.
    // Please refer to Serializable javadoc for more details
    private static final long serialVersionUID = 2547789651453L;

    // END OA OA 27/11/2013

    @Override
    public String getTaskInformation() {
        return "EOD contract snapshot for Risk Parameters Report";
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

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

        try {
            Vector<String> errorMsgs = new Vector<String>();
            performRiskParameter(ds, errorMsgs);
            if (errorMsgs.size() == 0) {
                task.setEventType("EX_" + BOException.INFORMATION);
            } else {
                task.setEventType("EX_" + BOException.EXCEPTION);
            }
            task.setCompletedDatetime(new JDatetime());
            task.setStatus(Task.NEW);

            TaskArray v = new TaskArray();
            v.add(task);
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0, null);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void performRiskParameter(DSConnection ds, Vector<String> errorMsgs) {
        List<SantRiskParameter> rpList = new ArrayList<SantRiskParameter>();
        Collection<CollateralConfig> contracts = null;
        try {

            final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
            contracts = srvReg.getCollateralDataServer().getAllMarginCallConfig();

            // GSM: Not working fine since new Core 1.5.6
            // @SuppressWarnings("deprecation")
            // contracts = getDSConnection().getRemoteReferenceData().getAllMarginCallConfig(0, 0);
        } catch (Exception e) {
            Log.error(this, "Cannnot load CONTRACTS", e);
            errorMsgs.add("Cannot load CONTRACTS - " + e.getMessage());
        }
        if (Util.isEmpty(contracts)) {
            return;
        }

        SantRiskParameterBuilder builder = new SantRiskParameterBuilder();
        JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        valDate = CollateralUtilities.getMCValDate(valDate);
        PricingEnv pe = AppUtil.loadPE(getPricingEnv(), new JDatetime(valDate));
        for (CollateralConfig contract : contracts) {
            if ("CLOSE".equals(contract.getAgreementStatus())) {
                continue;
            }
            SantRiskParameter rp = builder.build(contract, valDate, pe);
            rpList.add(rp);
        }

        try {
            SantReportingUtil.getSantRiskParameterService(getDSConnection()).save(rpList);
        } catch (RemoteException e) {
            Log.error(this, "Cannnot save risk parameters", e);
            errorMsgs.add("Cannot save risk parameters - " + e.getMessage());
        }
    }

}
