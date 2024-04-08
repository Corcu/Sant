package calypsox.tk.report.carteras;

import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CarterasTaskHandler {

    JDatetime exportedTime;
    CarterasTaskPersistor persistor;

    public CarterasTaskHandler(JDatetime exportedTime, boolean enableTasks) {
        this.exportedTime = Optional.ofNullable(exportedTime).orElse(new JDatetime());
        this.persistor=new CarterasTaskPersistor(enableTasks);
    }

    public boolean isSentToCarteras(Trade trade) {
        return !isFirstTimeExport(trade);
    }

    public boolean isFirstTimeExport(Trade trade) {
        boolean isFirstTime = true;
        Task t = persistor.getLastCarterasTask(trade.getLongId());
        if (t != null) {
                isFirstTime = false;
                Log.debug(this, "Trade was already exported on " + t.getDatetime());
        }
        return isFirstTime;
    }

    public boolean isModifExportNeeded(Trade trade) {
        boolean isExportNeeded = false;
        Task t = persistor.getLastCarterasTask(trade.getLongId());
        if (t != null) {
            TradePartenonBuilder partenonBuilder=new TradePartenonBuilder(trade);
            String partenon=partenonBuilder.buildFullPartenon();
            if(t.getComment()!=null&&!t.getComment().equalsIgnoreCase(partenon)) {
                isExportNeeded = true;
            }
        }
        return isExportNeeded;
    }

    public String getLastCarterasPartenon(Trade trade) {
        String lastPartenon="";
        Task t = persistor.getLastCarterasTask(trade.getLongId());
        if (t != null) {
           lastPartenon=t.getComment();
        }
        return lastPartenon;
    }

    public void publishTaskIfNotExists(final Trade trade) {
        if(isFirstTimeExport(trade)||isModifExportNeeded(trade)) {
            persistor.publishTaskIfNotExists(trade,this.exportedTime);
        }

    }
}


