package calypsox.tk.confirmation;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class BondFwdConfirmationMsgBuilder extends BondConfirmationMsgBuilder{

    public BondFwdConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }
}
