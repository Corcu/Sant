package calypsox.tk.upload.uploader.util;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.FXBased;
import com.calypso.tk.refdata.AuditFilter;
import com.calypso.tk.refdata.AuditFilterUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;
import static calypsox.tk.report.FXPLMarkReportStyle.getOtherMultiCcyTrade;

import java.util.Vector;

public class UploadBondMultiCCyUtil {

    public static boolean isDualCcy(Trade trade) {
        return trade.getKeywordValue("Dual_CCY") != null &&
                trade.getKeywordValue("Dual_CCY").equals("true");
    }

    public static boolean isNotInPending(Trade trade) {
        return !trade.getStatus().toString().equals("PENDING_DUAL_CCY");
    }

    public static boolean isAmend(Trade trade) {
        return trade.getAction().toString().equals("AMEND");
    }

    public static boolean isCancel(Trade trade) {
        return trade.getAction().toString().equals("CANCEL");
    }

    public static Trade getOldTrade(Trade trade) {
        String extRef = trade.getExternalReference();
        if (!extRef.isEmpty() && extRef != null) {
            try {
                TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTradesByExternalRef(extRef);
                return !trades.isEmpty() ? trades.get(0) : null;
            } catch (CalypsoServiceException e) {
                Log.error(UploadBondMultiCCyUtil.class, "Could not save the exception task.");
            }
        }
        return null;
    }

    public static boolean hasSettleTransfer(Trade trade, DSConnection ds) {
        long tradeId = 0;
        if(trade.getProduct() instanceof Bond) {
            tradeId = trade.getLongId();
        }
        else{
           tradeId = getOtherMultiCcyTrade(trade).getLongId();
        }
        try {
            TransferArray transfers = ds.getRemoteBackOffice().getBOTransfers(trade.getLongId());
            for (BOTransfer transfer:transfers){
                if(transfer.getTransferType().equals("PRINCIPAL") && transfer.getStatus().toString().equals("SETTLED")){
                    return true;
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(UploadBondMultiCCyUtil.class, e);
        }
        return false;
    }



    public static boolean acceptChanges(Trade trade, String product) {
        Trade oldTrade = getOldTrade(trade);
        AuditFilter filter = AuditFilterUtil.findByName(getFilterName(product));
        return filter.accept("NOT_IN", oldTrade, trade);
    }

    public static String addError(Trade trade, Vector<BOException> errors, String product, boolean isEconomicChanges) {
        BOException ex = new BOException();
        String description = "";
        if(isEconomicChanges) {
            description = "Some changes are related with economic fields. Please check '" + getFilterName(product) +
                    "' audit filter to ensure which fields are not valid";
            errors.add(ErrorExceptionUtils.createException("21001", "Economic Data", "10239", description, trade.getLongId(), trade.getExternalReference()));
        }
        else{
            errors.add(ErrorExceptionUtils.createException("21001", "Cancel not Applied", "10239", "Cancel could not apply because the trade has transfer in SETTLED",
                    trade.getLongId(), trade.getExternalReference()));
        }
        ex.setTradeLongId(trade.getLongId());
        errors.add(ex);
        return description;
    }

    private static String getFilterName(String product){
        String filterName="";
        switch (product) {
            case "Bond":
                filterName = "Bond Partenon Change";
                break;
            case "FX":
                filterName = "FX Change";
                break;
        }
        return filterName;
    }

    public static void beforeSaveUtil(CalypsoObject calypsoObject, Vector<BOException> errors, Trade trade, String product) {
        addDualCcyTkw(trade);
        if (isDualCcy(trade) &&
                isNotInPending(trade)) {
            if (isAmend(trade) && !acceptChanges(trade, product)) {
                String description = addError(trade, errors, product, true);
                generateTask(DSConnection.getDefault(), trade, "EX_DUAL_CCY_ECONOMIC_CHANGES", description);
            }
            if(isCancel(trade) && hasSettleTransfer(trade, DSConnection.getDefault())){
                String description = addError(trade, errors, product, false);
                generateTask(DSConnection.getDefault(), trade, "EX_DUAL_CCY_NOT_CANCELLED", "");
            }
        }
    }

    private static void generateTask(DSConnection ds, Trade trade, String eventType, String description){
        Task taskException = new Task();
        taskException.setStatus(Task.NEW);
        taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        taskException.setEventType(eventType);
        taskException.setComment(description);
        taskException.setTradeId(trade.getLongId());
        taskException.setBookId(trade.getBookId());
        taskException.setPriority(Task.PRIORITY_HIGH);
        TaskArray task = new TaskArray();
        task.add(taskException);
        try {
            ds.getRemoteBackOffice().saveAndPublishTasks(task,0L,null);
        }
        catch (CalypsoServiceException e) {
            Log.error(UploadBondMultiCCyUtil.class, "Could not save the exception task.");
        }
    }

    private static void addDualCcyTkw(Trade trade){
        if(trade.getProduct() instanceof FXBased) {
            trade.addKeyword("Dual_CCY","true");
        }
    }
}
