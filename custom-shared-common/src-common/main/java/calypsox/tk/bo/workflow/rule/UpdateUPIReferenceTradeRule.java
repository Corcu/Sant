package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class UpdateUPIReferenceTradeRule implements WfTradeRule {

    private static final String MX_SECURITY_DISPLAY_KW = "Mx_SecurityDisplayLabel";
    private static final String UPI_REFERENCE_KW = "UPI_REFERENCE";

    private static final String UPI_REFERENCE_CONFIG_DV = "UPIReferenceConfig";

    private String prefix;
    private String valueWithPrefix;
    private String valueWithoutPrefix;

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Rule to check a prefix of Mx_SecurityDisplay_Label Keyword and set a value on UPI_REFERENCE Keyword";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        if (trade != null && isConfigured()) {
            String keywordValue = trade.getKeywordValue(MX_SECURITY_DISPLAY_KW);
            if (keywordValue != null && keywordValue.startsWith(prefix)) {
                trade.addKeyword(UPI_REFERENCE_KW, valueWithPrefix);
            } else {
                trade.addKeyword(UPI_REFERENCE_KW, valueWithoutPrefix);
            }
        }

        return true;
    }

    private boolean isConfigured() {
        boolean configured = false;

        Vector<String> configDomainValue = LocalCache.getDomainValues(DSConnection.getDefault(), UPI_REFERENCE_CONFIG_DV);

        if (configDomainValue != null) {
            prefix = configDomainValue.firstElement();
            String comment = LocalCache.getDomainValueComment(DSConnection.getDefault(), UPI_REFERENCE_CONFIG_DV, prefix);

            if (comment != null && !comment.isEmpty() && comment.split(";").length == 2) {
                valueWithPrefix = comment.split(";")[0];
                valueWithoutPrefix = comment.split(";")[1];
                configured = true;
            } else {
                Log.error(this, "Check domain value comment content. It's must be 'value_to_set_if_start_with_prefix;value_to_set_if_don't_start_with_prefix'");
            }
        } else {
            Log.error(this, "Check domain value configuration. DV= " + UPI_REFERENCE_CONFIG_DV);
        }
        return configured;
    }
}
