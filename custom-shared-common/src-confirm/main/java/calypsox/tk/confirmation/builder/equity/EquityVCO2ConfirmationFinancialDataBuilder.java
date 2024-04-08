package calypsox.tk.confirmation.builder.equity;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

public class EquityVCO2ConfirmationFinancialDataBuilder extends EquityCO2ConfirmationFinancialDataBuilder {
	public EquityVCO2ConfirmationFinancialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
		super(boMessage, boTransfer, trade);
	}

	public String buildVccNumber() {
		return String.valueOf(trade.getQuantity());
	}

	public String buildVccPurchPrice() {
		return String.valueOf(trade.getNegociatedPrice());
	}

	public String buildVccPurchCurr() {
		return trade.getTradeCurrency();
	}
}
