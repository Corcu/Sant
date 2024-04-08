package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.AccountingBook;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;

public class SecLendingCreHandler extends com.calypso.tk.bo.accounting.SecLendingCreHandler {

	public static final String MARGINCALL_CONTRACT_ID_ATTR = "MarginCallContractId";

	public void fillAttributes(BOCre cre, Trade trade, PSEvent event, AccountingEventConfig eventConfig, AccountingRule rule, AccountingBook accountingBook) {
		SecLending secLending = (SecLending)trade.getProduct();
		if(cre.getEventType()==null || !cre.getEventType().equals(BOCreConstantes.SECLENDING_FEE))
				cre.addAttribute(MARGINCALL_CONTRACT_ID_ATTR, String.valueOf(secLending.getMarginCallContractId(trade)));
				cre.addAttribute(BOCreConstantes.CONTRAT_ATT, String.valueOf(secLending.getMarginCallContractId(trade)));
		super.fillAttributes(cre, trade, event, eventConfig, rule, accountingBook);
	}

}
