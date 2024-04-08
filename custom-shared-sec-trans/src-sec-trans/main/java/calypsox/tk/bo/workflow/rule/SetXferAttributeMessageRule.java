package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class SetXferAttributeMessageRule implements WfMessageRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return transfer != null;
    }

    @Override
    public String getDescription() {
        return "Set attributes of indexed to the message transfer.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        String param = wc.getRuleParam(this.getClass().getSimpleName().replace("MessageRule", ""));
        Action action = getAction(param);
        List<Pair<String, String>> attr = getAttributes(param);

        if (!Util.isEmpty(attr)) {

            try {

                if (attr.size() == 1 && Action.UPDATE.equals(action)) {

                    long[] ids = new long[]{transfer.getLongId()};

                    if (dbCon == null) {
                        dsCon.getRemoteBO().saveTransfersAttribute(ids, attr.get(0).first(), attr.get(0).second());
                    } else {
                        BOTransferSQL.saveAttributes(ids, attr.get(0).first(), attr.get(0).second(), (Connection) dbCon);
                    }
                } else {
                    BOTransfer toUpdate = (BOTransfer) transfer.clone();
                    toUpdate.setAction(action);

                    attr.forEach(a -> toUpdate.setAttribute(a.first(), a.second()));
                    if (dbCon == null) {
                        dsCon.getRemoteBO().save(toUpdate, 0L, null);
                    } else {
                        BOTransferSQL.save(toUpdate, (Connection) dbCon);
                    }
                }
            } catch (Exception e) {
                messages.add(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
                Log.error(this, e);
            }
        }

        return true;
    }

    private List<Pair<String, String>> getAttributes(String param) {
        String attrs = getParam(param, "Attributes", null);
        if (!Util.isEmpty(attrs)) {
            List<String> attr = Util.stringToList(attrs);
            return attr.stream().filter(a -> !Util.isEmpty(a) && !a.trim().equals("")).map(a -> {
                String[] kv = a.split("=");
                if (!Util.isEmpty(kv) && !Util.isEmpty(kv[0])) {
                    return new Pair<>(kv[0].trim(), kv.length < 2 || Util.isEmpty(kv[1]) || kv[1].trim().equals("") ? null : kv[1].trim());
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    //FORMAT S Action=UPDATE,Attributes:ProcessingStatus=XYX,AttrName2=AttrValue2
    private Action getAction(String param) {
        return Action.valueOf(getParam(param, "Action", "UPDATE"));
    }

    private String getParam(String param, String paramName, String defaultVal) {
        String[] actionAndAttr = param.split(",");
        return Arrays.stream(actionAndAttr).filter(s -> {
            if (!Util.isEmpty(s)) {
                String[] kv = s.split(":");
                return kv.length == 2 && paramName.equalsIgnoreCase(kv[0].trim()) && !Util.isEmpty(kv[1]) && !kv[1].trim().equals("");
            }
            return false;
        }).map(s -> s.split(":")[1].trim()).findFirst().orElse(defaultVal);

    }


}
