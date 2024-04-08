package calypsox.tk.confirmation.builder.bond;

import calypsox.tk.confirmation.builder.CalConfirmationProcessingOrgBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class BondConfirmProcessingOrgBuilder extends CalConfirmationProcessingOrgBuilder {


    public BondConfirmProcessingOrgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    public String buildAlertSettlementModelName(){
        return Optional.ofNullable(this.cptyContact).map(LEContact::getSwift)
                .map(bic->bic.replace("XXX",""))
                .orElse("");
    }
}
