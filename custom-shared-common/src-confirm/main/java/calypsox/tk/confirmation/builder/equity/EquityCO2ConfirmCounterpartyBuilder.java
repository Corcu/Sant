package calypsox.tk.confirmation.builder.equity;


import calypsox.tk.confirmation.builder.CalConfirmationCounterpartyBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;


public class EquityCO2ConfirmCounterpartyBuilder extends CalConfirmationCounterpartyBuilder {


    public EquityCO2ConfirmCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }


    @Override
    public String buildCptyAddress() {
        String address = cptyContact.getMailingAddress();
        String zipCode = cptyContact.getZipCode();
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
