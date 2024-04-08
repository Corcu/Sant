package calypsox.tk.bo.accounting.keyword;

import java.util.Vector;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.accounting.keyword.AccountKeyword;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.AccountTranslation;
import com.calypso.tk.service.DSConnection;

public class AutoCcyAccountKeyword implements AccountKeyword{

	@SuppressWarnings("rawtypes")
	@Override
	public String fill(BOPosting posting, AccountTranslation accTrans, Trade trade, BOTransfer transfer, DSConnection dsCon,Vector exceptions) {
		if(posting != null && posting.getCurrency()!=null)
			return posting.getCurrency();
		if(transfer!=null)
			return transfer.getSettlementCurrency();
		return null;
	}

}
