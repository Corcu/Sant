package calypsox.tk.confirmation.builder;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public abstract class CalypsoConfirmationConcreteBuilder {

    protected BOTransfer boTransfer;
    protected Trade trade;
    protected BOMessage boMessage;

    private CalypsoConfirmationConcreteBuilder() {
    }

    public CalypsoConfirmationConcreteBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        this.trade = trade;
        this.boTransfer = boTransfer;
        this.boMessage = boMessage;
    }
}
