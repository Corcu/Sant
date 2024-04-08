package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SantCheckPORefMessageRule implements WfMessageRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        String params = wc.getRuleParam(this.getClass().getSimpleName().replace("MessageRule", ""));
        if (Util.isEmpty(params))
            return false;

        final String ref = message.getAttribute("PORef");
        if (Util.isEmpty(ref)) {
            messages.add("PO Reference is empty.");
            return false;
        }


        boolean check = parseParams(params).stream().anyMatch(r -> Pattern.compile(r).matcher(ref).matches());
        if (!check) {
            messages.add(String.format("PO Reference %s does not match the pattern of system generated reference.", ref));
        }
        return check;
    }

    private Collection<String> parseParams(String input) {
        List<String> tokens = new ArrayList<>();
        int startPosition = 0;
        boolean isInQuotes = false;
        for (int currentPosition = 0; currentPosition < input.length(); currentPosition++) {
            if (input.charAt(currentPosition) == '\"') {
                isInQuotes = !isInQuotes;
            } else if (input.charAt(currentPosition) == ',' && !isInQuotes) {
                tokens.add(input.substring(startPosition, currentPosition));
                startPosition = currentPosition + 1;
            }
        }

        String lastToken = input.substring(startPosition);
        if (lastToken.equals(",")) {
            tokens.add("");
        } else {
            tokens.add(lastToken);
        }
        return tokens.stream().map(t -> t.trim().substring(1, t.length() - 1)).collect(Collectors.toList());
    }

    @Override
    public String getDescription() {
        return "True if Message PORef matches any of the patterns supplied in the rule parameters.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}
