package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.corporateaction.CASwiftCodeDescription;
import com.calypso.tk.product.corporateaction.CASwiftEventCode;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class GenerateCARefConTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Add Swift_Event_Code + CAReference on keyword CARefConci";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (null != trade && trade.getProduct() instanceof CA) {
            String swiftEventCodeName = Optional.ofNullable(((CA) trade.getProduct()).getSwiftEventCode())
                    .map(CASwiftEventCode::getSwiftCodeDescription)
                    .map(CASwiftCodeDescription::toString)
                    .orElse("");
            int id = trade.getProduct().getId();
            if(!Util.isEmpty(swiftEventCodeName)){
                trade.addKeyword("CARefConci",swiftEventCodeName+id);
            }
        }
        return true;
    }
}
