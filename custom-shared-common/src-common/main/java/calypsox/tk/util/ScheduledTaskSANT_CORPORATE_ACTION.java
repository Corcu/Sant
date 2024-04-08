package calypsox.tk.util;


import com.calypso.tk.risk.util.AnalysisProgressUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.*;
import java.util.Vector;


public class ScheduledTaskSANT_CORPORATE_ACTION extends ScheduledTaskCORPORATE_ACTION {


    public Vector<String> _messages;


    @Override
    protected String handleProcess(DSConnection ds, TaskArray tasks) {
        String exec = null;
        try {
            if (this.isBackValueProcess()) {
                exec = ScheduledTaskCA_BACKVALUE.delegateProcessRequest(this, ds, tasks);
            } else if (this.isOnlyCAProcess()) {
                exec = ScheduledTaskCA_GENERATE.delegateProcessRequest(this, ds, tasks);
            } else if (this.isOnlyTradeProcess()) {
                exec = ScheduledTaskSANT_RF_CA_APPLY.delegateProcessRequest(this, ds, tasks);
            } else {
                exec = ScheduledTaskCA_GENERATE.delegateProcessRequest(this, ds, tasks);
                if (exec == null) {
                    exec = ScheduledTaskCA_APPLY.delegateProcessRequest(this, ds, tasks);
                }
            }
        } catch (Exception var5) {
            exec = "Error processing ScheduledTaskSANT_CORPORATE_ACTION";
            AnalysisProgressUtil.logError(exec, var5);
        }
        return exec;
    }


    private boolean isBackValueProcess() {
        return this.getBooleanAttribute("BACK VALUE PROCESS", false);
    }


    private boolean isOnlyCAProcess() {
        return this.getBooleanAttribute("Only CA Product Process", false);
    }


}
