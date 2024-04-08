package calypsox.tk.confirmation.builder.bond;


import calypsox.tk.confirmation.builder.repo.RepoConfirmEventDataBuilder;
import calypsox.tk.confirmation.handler.BondCalConfirmationEventHandler;
import calypsox.tk.confirmation.handler.CalypsoConfirmationEventHandler;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author dmenendd
 */
public class BondConfirmEventDataBuilder extends RepoConfirmEventDataBuilder {

    public BondConfirmEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    @Override
    protected CalypsoConfirmationEventHandler getEventTypeHandler(BOMessage boMessage){
        return new BondCalConfirmationEventHandler(boMessage);
    }

    @Override
    public String buildInstrumentType() {
        return "Fixed Income Forward";
    }

}
