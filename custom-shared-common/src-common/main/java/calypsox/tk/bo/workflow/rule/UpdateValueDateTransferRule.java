package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.bo.workflow.rule.IValueDateControl;
import com.calypso.tk.bo.workflow.rule.NullValueDateControlHandler;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.TransferArray;

import java.util.Vector;

public class UpdateValueDateTransferRule implements WfTransferRule {
    IValueDateControl valueDateControlHandler = null;

    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    private IValueDateControl getValueDateControlHandler() {
        if (this.valueDateControlHandler == null) {
            try {
                Object handler = InstantiateUtil.getInstance("tk.bo.workflow.rule.BOMessageValueDateControlHandler", true, true);
                String msg;
                if (handler == null) {
                    msg = "Failed to load BOMessageValueDateControlHandler";
                    Log.info(this, msg);
                } else if (handler instanceof IValueDateControl) {
                    this.valueDateControlHandler = (IValueDateControl)handler;
                } else {
                    msg = "Failed to load BOMessageValueDateControlHandler: invalid instance " + handler;
                    Log.error(this, msg);
                }
            } catch (Exception var3) {
                Log.info(this, "Failed to load BOMessageValueDateControlHandler", var3);
            }

            if (this.valueDateControlHandler == null) {
                this.valueDateControlHandler = new NullValueDateControlHandler();
            }
        }

        return this.valueDateControlHandler;
    }

    public String getDescription() {
        String tmp = "Change the Settle Date of the Transfer based on currency settlement cut-off time.\rn\n Does not apply to DFP Security transfer";
        return tmp;
    }

    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (dbCon == null) {
            return true;
        } else if (transfer.isDDATransfer() && trade != null && "YES".equals(trade.getKeywordValue("AccountClosure"))) {
            return true;
        } else if ("DFP".equals(transfer.getDeliveryType()) && "SECURITY".equals(transfer.getTransferType())) {
            return true;
        } else if (this.getValueDateControlHandler().updateWithValueDateControl(transfer)) {
            return true;
        } else {
            Vector codeHol = new Vector();
            CurrencyDefault cd = LocalCache.getCurrencyDefault(transfer.getSettlementCurrency());
            if (cd != null && Util.isEmpty(codeHol)) {
                codeHol = cd.getDefaultHolidays();
            }

            JDate settleDate = null;
            if (cd != null && !Util.isEmpty(cd.getSettlementCutoffTime())) {
                settleDate = cd.getSettlementCutOff(new JDatetime());
            } else {
                Book book = BOCache.getBook(dsCon, transfer.getBookId());
                if (book == null) {
                    messages.add("Book not Found " + transfer.getBookId());
                    return false;
                }

                settleDate = book.getBusinessDate();
            }

            if (settleDate.before(transfer.getValueDate())) {
                return true;
            } else {
                Holiday hol = Holiday.getCurrent();
                if (!hol.isBusinessDay(settleDate, codeHol)) {
                    settleDate = hol.addBusinessDays(settleDate, codeHol, 1, true);
                }
                transfer.setSettleDate(settleDate);
                transfer.setValueDate(settleDate);
                if (transfer.getNettedTransfer()) {
                    TransferArray basicTransfers = transfer.getUnderlyingTransfers();
                    if (basicTransfers != null) {
                        for(int i = 0; i < basicTransfers.size(); ++i) {
                            BOTransfer basicXfer = (BOTransfer)basicTransfers.get(i);
                            basicXfer.setSettleDate(settleDate);
                            basicXfer.setValueDate(settleDate);
                        }
                    }
                }

                return true;
            }
        }
    }
}
