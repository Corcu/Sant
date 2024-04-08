package calypsox.tk.confirmation.builder.equity;


import java.util.Optional;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LEContact;

import calypsox.tk.confirmation.builder.CalConfirmationCounterpartyBuilder;


public class EquityETFConfirmCounterpartyBuilder extends CalConfirmationCounterpartyBuilder {


    public EquityETFConfirmCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }


    @Override
    public String buildCptyAddress() {
		String address = Optional.ofNullable(cptyContact).map(LEContact::getMailingAddress).orElse("");
		String zipCode = Optional.ofNullable(cptyContact).map(LEContact::getZipCode).orElse("");

        if(Util.isEmpty(zipCode) && !Util.isEmpty(address)){
            return address;
        }
        else if(!Util.isEmpty(zipCode) && !address.contains(zipCode.trim())){
            return address + " " + zipCode;
        }
        else {
           return address;
        }
    }


}
