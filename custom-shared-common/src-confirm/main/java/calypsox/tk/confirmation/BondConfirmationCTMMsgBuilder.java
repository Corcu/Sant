package calypsox.tk.confirmation;

import calypsox.tk.confirmation.builder.bond.ctm.CTMBondConfirmCounterpartyBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class BondConfirmationCTMMsgBuilder extends BondSpotConfirmationMsgBuilder{

    public BondConfirmationCTMMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        this.cptyDataBuilder=new CTMBondConfirmCounterpartyBuilder(boMessage,boTransfer,trade);
    }
}
