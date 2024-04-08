package calypsox.util;

import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author acd
 */
public class TradeSaverThread implements Callable<Long> {
    private Trade trade;
    Action action;
    ConcurrentLinkedQueue<String> errorList;
    public TradeSaverThread(Trade trade, Action action, ConcurrentLinkedQueue<String> errorList) {
        this.trade = trade;
        this.action = action;
        this.errorList = errorList;
    }

    @Override
    public Long call() {
        DSConnection aDefault = DSConnection.getDefault();
        if(null!=trade){
            try {

                Trade clonedTrade = (Trade)trade.cloneIfImmutable();
                clonedTrade.setAction(action);

                if(TradeWorkflow.isTradeActionApplicable(clonedTrade, action, aDefault, null)){
                    return aDefault.getRemoteTrade().save(clonedTrade);
                }else {
                    addError("Cannot apply action "+action+" : " + trade.getLongId());
                }

            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving trade: " + e.getMessage());
                addError("Cannot apply action "+action+" to the trade. " + e.getMessage());
            } catch (CloneNotSupportedException e) {
                Log.error(this.getClass().getSimpleName(),"Error cloning trade: " + e.getMessage());
            }
        }
        return -1L;
    }

    private void addError(String error){
        Optional.ofNullable(errorList).ifPresent(list -> {
            list.add(error);
        });
    }
}
