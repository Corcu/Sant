package calypsox.tk.confirmation.builder.equity;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;


public class EquityVCO2ConfirmationEventDataBuilder extends EquityCO2ConfirmationEventDataBuilder {


    public EquityVCO2ConfirmationEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
	}


    public String buildInstrumentSubType() {
        return "VCC " + trade.getKeywordValue("Mx_Product_SubType");
    }


}
