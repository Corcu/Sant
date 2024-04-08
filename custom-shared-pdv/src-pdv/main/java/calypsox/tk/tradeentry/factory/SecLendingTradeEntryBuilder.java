package calypsox.tk.tradeentry.factory;

import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.tradeentry.factory.ValuationInfoProvider;

import calypsox.tk.secfinance.SecLendingTradeEntry;

public class SecLendingTradeEntryBuilder extends com.calypso.tk.tradeentry.factory.SecLendingTradeEntryBuilder {

	protected SecFinanceTradeEntry createSecFinanceTradeEntry(ValuationInfoProvider valuationInfoProvider,
			SecFinanceTradeEntryContext tradeEntryContext) {
		return new SecLendingTradeEntry(this.trade, valuationInfoProvider, tradeEntryContext);
	}
	
}
