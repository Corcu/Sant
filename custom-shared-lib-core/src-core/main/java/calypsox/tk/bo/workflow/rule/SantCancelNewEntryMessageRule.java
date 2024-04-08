package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;
import java.util.Vector;

public class SantCancelNewEntryMessageRule implements WfMessageRule {

    private static final String ENTRY_ATTRIBUTE = "CANCELNEW";

    @Override
    public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, BOMessage paramBOMessage1,
                         BOMessage paramBOMessage2, Trade paramTrade, BOTransfer paramBOTransfer, Vector paramVector1,
                         DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
                         Vector paramVector3) {

        return true;
    }

    @Override
    public String getDescription() {

        return "Apply the action CANCELNEW to the entry related with the message";
    }

    @Override
    public boolean update(TaskWorkflowConfig paramTaskWorkflowConfig, BOMessage message, BOMessage paramBOMessage2,
                          Trade paramTrade, BOTransfer paramBOTransfer, Vector paramVector1, DSConnection dsCon, Vector paramVector2,
                          Task paramTask, Object paramObject, Vector paramVector3) {
        boolean result = false;

        if (message != null && !Util.isEmpty(message.getAttribute("marginCallEntryId"))) {
            int entryid = getEntryId(message);
            try {
                Log.info(this, "Loading entry: " + entryid + " from message: " + message.getLongId());
                MarginCallEntryDTO entry = ServiceRegistry.getDefault().getCollateralServer().loadEntry(entryid);
                if (entry != null) {
                    //add CANCELNEW attribute to the entry
                    entry.addAttribute(ENTRY_ATTRIBUTE, message.getLongId());
                    Log.info(this, "Apply action to the entry: " + entry.getId());
                    int id = ServiceRegistry.getDefault().getCollateralServer().save(entry, "CANCELNEW",
                            TimeZone.getDefault());
                    if (id > 0) result = true;
                } else {
                    Log.error(this, "Cannot find entry: " + entryid);
                }
            } catch (CollateralServiceException e) {
                Log.error(this, "Error with entry: " + entryid + "Error: " + e);

            }
        }

        return result;
    }


    private int getEntryId(BOMessage message) {
        Integer entryid = 0;
        try {
            entryid = Integer.parseInt(message.getAttribute("marginCallEntryId"));
        } catch (NumberFormatException e) {
            Log.error(this, "Cannot parse marginCallEntryId from message: " + message.getLongId(), e);
        }
        return entryid;
    }

}
