package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventProcessTransfer;
import com.calypso.tk.service.DSConnection;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import static calypsox.tk.util.swiftparser.MT541MessageProcessor.PROCESSING_STATUS;
import static com.calypso.tk.core.Util.isEmpty;

public class SantPublishProcessTransferEvMessageRule implements WfMessageRule {

    private static final List<String> DEFAULT_ENGINES = Collections.singletonList("MessageEngine");

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return transfer != null;
    }

    @Override
    public String getDescription() {
        return "Publishes PSEventProcessTransfer to a specific engine.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        String processingStatus = message.getAttribute(PROCESSING_STATUS);
        if (!isEmpty(processingStatus)) {
            try {
                BOTransfer updatedTransfer = message.getTransferLongId() > 0
                        ? dbCon == null ? dsCon.getRemoteBO().getBOTransfer(message.getTransferLongId()) : BOTransferSQL.getTransfer(message.getTransferLongId(), (Connection) dbCon)
                        : null;
                if (updatedTransfer == null) {
                    messages.add(String.format("Cannot find message transfer by message transfer id %s", message.getTransferLongId()));
                    return false;
                }
                Map<String, List<String>> params = parseParams(wc.getRuleParam(getRuleName()), updatedTransfer);

                if (params.get("ProcessingStatus").contains(processingStatus)) {

                    Action action = Action.valueOf(params.get("Action").get(0));
                    BOTransfer newXfer = updatedTransfer;

                    if (!action.equals(updatedTransfer.getAction())) {

                        newXfer = (BOTransfer) updatedTransfer.clone();
                        newXfer.setAction(action);

                    }
                    PSEventProcessTransfer event = new PSEventProcessTransfer(newXfer, trade, updatedTransfer);
                    event.setAction(action);
                    event.setEngineNames(params.get("Engines").toArray(new String[]{}));
                    events.add(event);
                }
            } catch (Exception e) {
                Log.error(this, e);
                messages.add(String.format("%s, %s", e.getClass(), e.getMessage()));
                return false;
            }
        }
        return true;
    }

    public Map<String, List<String>> parseParams(String ruleParam, BOTransfer xfer) {
        //format ProcessingStatus={IBRC/ALTB|IBRC/ALTC},Action={AMEND},Engines={MessageEngine}
        Map<String, List<String>> result = new HashMap<String, List<String>>() {{
            put("ProcessingStatus", Collections.singletonList("IBRC/ALTC"));
            put("Action", Collections.singletonList(xfer.getAction().toString()));
            put("Engines", DEFAULT_ENGINES);
        }};
        if (!isEmpty(ruleParam)) {
            String[] pairs = trim(ruleParam.split(","));

            for (String pair : pairs) {
                if (pair != null) {
                    if (!isEmpty(pair)) {
                        String[] keyAndVals = trim(pair.split("="));
                        if (keyAndVals.length != 2) {
                            Log.error(SantPublishProcessTransferEvMessageRule.class.getName(), "Misformatted rule parameters ProcessingStatus={IBRC/ALTB|IBRC/ALTC},Action={AMEND},Engines={MessageEngine] expected.");
                            return result;
                        }
                        if (isEmpty(keyAndVals[0]) || isEmpty(keyAndVals[1]) || !keyAndVals[1].startsWith("{") || !keyAndVals[1].endsWith("}")) {
                            Log.error(SantPublishProcessTransferEvMessageRule.class.getName(), "Misformatted rule parameters ProcessingStatus={IBRC/ALTB|IBRC/ALTC},Action={AMEND},Engines={MessageEngine] expected.");
                            return result;
                        } else {
                            String[] valueStr = trim(keyAndVals[1].substring(1, keyAndVals[1].length() - 1).split("\\|"));
                            List<String> vals = Arrays.stream(valueStr).filter(Objects::nonNull).map(String::trim).filter(v -> !isEmpty(v)).collect(Collectors.toList());
                            result.put(keyAndVals[0], vals);
                        }

                    }
                }
            }

        }

        return result;
    }

    private String[] trim(String[] array) {
        return array == null ? null : Arrays.stream(array).map(s -> s == null ? null : s.trim()).collect(Collectors.toList()).toArray(new String[]{});
    }


    private String getRuleName() {
        return this.getClass().getSimpleName().replace("MessageRule", "");
    }
}