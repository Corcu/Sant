package calypsox.tk.confirmation.builder.equity;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;


public class EquityVCO2ConfirmCounterpartyBuilder extends EquityCO2ConfirmCounterpartyBuilder {
	public EquityVCO2ConfirmCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

	public String buildRetirement() {

		return trade.getBook().getLegalEntity().getId() == cpty.getId() ? "0" : "1";
	}
}
