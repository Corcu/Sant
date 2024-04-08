package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/*
 * This rule checks if the trade has valid book, cpty, isin etc
 */
public class SantIsValidTradeRule implements WfTradeRule {

    public static final String ORIG_SOURCE_BOOK = "ORIG_SOURCE_BOOK";
    public static final String ORIG_SOURCE_CPTY = "ORIG_SOURCE_CPTY";
    public static final String ORIG_SOURCE_ISIN = "ORIG_SOURCE_ISIN";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector exceptions, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        String desc = "This rule checks if the trade has valid book, cpty, isin etc. \n";
        return desc;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection ds,
                          Vector exceptions, Task task, Object dbCon, Vector events) {
        Log.debug("SantIsValidTradeRule", "Update - Start");
        if (trade == null) {
            return false;
        }
        boolean isValid = true;

        if (trade.getCounterParty().getName().equals("NONE")) {
            String missingCpty = trade.getKeywordValue(ORIG_SOURCE_CPTY);
            String comment = "";
            if (!Util.isEmpty(missingCpty)) {
                comment = "Cpty " + missingCpty + " is missing in the system so saved with NONE.[SantIsValidTradeRule]";
            } else {
                comment = "Cpty missing in the system so saved with NONE.[SantIsValidTradeRule]";
            }
            task.setComment(comment);
            isValid = false;
        }

        if (trade.getBook().getName().equals("NONE")) {
            String missingBook = trade.getKeywordValue(ORIG_SOURCE_BOOK);
            String comment = "";
            if (!Util.isEmpty(missingBook)) {
                comment = "Book " + missingBook + " is missing in the system so saved with NONE.[SantIsValidTradeRule]";
            } else {
                comment = "Book missing in the system so saved with NONE.[SantIsValidTradeRule]";
            }

            if (isValid) {
                task.setComment(comment);
            } else {
                exceptions.add(buildTask(trade, comment));
            }

            isValid = false;
        }

        Repo repo = (Repo) trade.getProduct();
        String isin = repo.getSecurity().getSecCode("ISIN");
        if (Util.isEmpty(isin)) {
            String missingISIN = trade.getKeywordValue(ORIG_SOURCE_ISIN);
            String comment = "";
            if (!Util.isEmpty(missingISIN)) {
                comment = "ISIN " + missingISIN + " is missing in the system so saved with NONE.[SantIsValidTradeRule]";
            } else {
                comment = "ISIN missing in the system so saved with NONE.[SantIsValidTradeRule]";
            }

            if (isValid) {
                task.setComment(comment);
            } else {
                exceptions.add(buildTask(trade, comment));
            }

            isValid = false;
        }

        Log.debug("SantIsValidTradeRule", "Update - End");

        return isValid;
    }

    public BOException buildTask(Trade trade, String comment) {
        BOException exc = new BOException(trade.getLongId(), "SantIsValidTradeRule", comment, BOException.STATIC_DATA);
        exc.setObjectClassName("PSEventTrade");
        exc.setLinkTask(true);
        return exc;

        // Task task = new Task();
        // task.setObjectId(trade.getLongId());
        // task.setEventClass("PSEventTrade");
        // task.setNewDatetime(new JDatetime());
        // task.setUndoTradeDatetime(trade.getSettleDatetime());
        // task.setDatetime(new JDatetime());
        // task.setPriority(Task.PRIORITY_NORMAL);
        // task.setTradeLongId(trade.getLongId());
        // task.setStatus(Task.NEW);
        // task.setSource("SantIsValidTradeRule");
        // task.setComment(comment);
        // task.setEventType("EX_STATIC_DATA");
        // return task;
    }

}
