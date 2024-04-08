package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Vector;


public class PositionConciliationMT564MessageRule implements WfMessageRule {


    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String position = message.getAttribute("positionFromMT564");
        if (position != null && message.getTradeLongId() > 0){
            boolean negative = false;
            DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance(Locale.forLanguageTag("ES"));
            try {
                if (position.startsWith("N")){
                    negative = true;
                    position = position.substring(1);
                    }
                double positionDouble = format.parse(position.trim()).doubleValue();
                Trade tradeCABOOK = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
                if (tradeCABOOK.getProduct().getProductClass().equalsIgnoreCase("CA")){
                    double quantity = tradeCABOOK.getQuantity();
                    String isActive = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "CAConciliationAbsoluteValue");
                    if (!Util.isEmpty(isActive) && Boolean.parseBoolean(isActive)){
                        return (Math.abs(quantity) == Math.abs(positionDouble));
                    }
                    if (negative){
                        return (quantity == -positionDouble);
                    } else {
                        return (quantity == positionDouble);
                    }
                }
            } catch (Exception e) {
                Log.error(this, "Message has no linked Trade id or attribute positionFromMT566 can not be converted to double");
                return true;
            }
        }
        return true;
    }


    @Override
    public String getDescription() {
        return "If Calypso position equals to MT566 position, returns true";
    }


    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }


}
