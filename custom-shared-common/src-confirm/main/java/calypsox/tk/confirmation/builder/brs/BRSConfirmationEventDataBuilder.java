package calypsox.tk.confirmation.builder.brs;

import calypsox.tk.confirmation.builder.CalConfirmationEventDataBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class BRSConfirmationEventDataBuilder extends CalConfirmationEventDataBuilder {


    public BRSConfirmationEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    public String buildInstrumentType() {
        return "Total Return Swap";
    }

    @Override
    public String buildOperId() {
        return confirmationEventInfo.getMxCurrentContractId();
    }

    public String buildOriginalOperId() {
        return confirmationEventInfo.getMxRootContractId();
    }

}
