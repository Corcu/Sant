package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

public class SantUpdateTripartyReversedTradeRule implements WfTradeRule {

    private static final String KEY = "ReversedAllocationTrade";
    private static final String PREP = "PREP";
    private static final String SWIFT_TYPE = "MT569";

    @Override
    public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector paramVector1,
                         DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
                         Vector paramVector3) {

        return true;
    }

    @Override
    public String getDescription() {
        return "Update Reversed Trade";
    }

    @Override
    public boolean update(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector message,
                          DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
                          Vector paramVector3) {

        if (trade != null && !Util.isEmpty(trade.getKeywordValue(KEY)) && !Util.isEmpty(trade.getKeywordValue(PREP))) {

            JDatetime date = getPrepAsDate(trade.getKeywordValue(PREP));
            reformatReverseAllocationTradeKeyword(trade);
            if (date != null) {
                trade.setTradeDate(date);
                trade.setSettleDate(date.getJDate(TimeZone.getDefault()));
                message.add("Trade Reversed Updated");
            } else {
                message.add("Cannot update dates for Trade Reversed");
            }
        }

        return true;
    }

       /**
     * Get jadatetime form PREP TAG in diferents formats 98E,98C or 98A
     *
     * @param prep
     * @return {@link JDatetime}
     */
    public static JDatetime getPrepJDatetime(String prep) {
        SwiftFieldMessage dateField = new SwiftFieldMessage();
        dateField.setName("Preparation Date/Time");
        dateField.setTAG(":98E:");
        dateField.setValue(":PREP//" + prep);


        JDatetime jdateTime = getSwiftFieldMessageJDatetime(dateField, SWIFT_TYPE);
        if (jdateTime == null) {
            dateField.setTAG(":98C:");
            jdateTime = getSwiftFieldMessageJDatetime(dateField, SWIFT_TYPE);
        }

        if (jdateTime != null) {
            return jdateTime;
        }
        dateField.setTAG(":98A:");
        jdateTime = getSwiftFieldMessageJDatetime(dateField, SWIFT_TYPE);

        return jdateTime;
    }

    protected static JDatetime getSwiftFieldMessageJDatetime(SwiftFieldMessage dateField, String type) {
        if (dateField == null) {
            return null;
        }
        Object dateValue = null;
        try {
            dateValue = dateField.parse(type);
            if ((dateValue != null)) {
                if (dateValue instanceof JDatetime) {
                    return (JDatetime) dateValue;
                } else if (dateValue instanceof JDate) {
                    return new JDatetime((JDate) dateValue, TimeZone.getDefault());
                }
            }
        } catch (Exception e) {
            Log.error(SantUpdateTripartyReversedTradeRule.class, "Error parsing Date: " + dateField.getValue() + " Error: " + e);
        }
        return null;
    }

    private void reformatReverseAllocationTradeKeyword(Trade trade) {
        Long reversedAllocationTradeKeyWord = formatCommaSeparatedStringAsLong(trade.getKeywordValue(KEY));
        if (reversedAllocationTradeKeyWord != null) {
            trade.addKeywordAsLong(KEY, reversedAllocationTradeKeyWord);
        }
    }

    /**
     * @param string
     * @return
     */
    protected Long formatCommaSeparatedStringAsLong(String string) {
        Long result = null;
        if (!Util.isEmpty(string)) {
            try {
                NumberFormat format = NumberFormat.getInstance(Locale.US);
                Number number = format.parse(string);
                result = number.longValue();
            } catch (ParseException exc) {
                Log.error(this.getClass().getSimpleName(), exc.getMessage());
            }
        }
        return result;
    }

    public static JDatetime getPrepAsDate(String prepField){
           try {
                if (prepField != null && prepField.length() >= 8) {
                    String tagdate = prepField.substring(0, 8);
                    SimpleDateFormat formatter = new SimpleDateFormat(
                            "yyyyMMdd");
                    JDate prepJDate = JDate.valueOf(formatter.parse(tagdate));
                        return new JDatetime(prepJDate, 10, 00, 00, TimeZone.getDefault());
                }
            } catch (Exception e) {
                Log.error("Error getting "+ prepField + " as date", e);
            }


        return null;
    }
}
