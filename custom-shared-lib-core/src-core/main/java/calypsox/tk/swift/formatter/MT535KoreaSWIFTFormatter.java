package calypsox.tk.swift.formatter;

import java.util.Vector;

import com.calypso.tk.bo.AccountStatement;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.swift.formatter.MT535SWIFTFormatter;

public class MT535KoreaSWIFTFormatter extends MT535SWIFTFormatter {
	
	@Override
	public String parseMESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
		
		return "CITI" + String.valueOf(System.currentTimeMillis());
	}
	
	public String parseACCOUNTID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {
	
		
		String account = message.getAttribute("Account");
		return  account != null ? account.toUpperCase() : "";
	}

	public String parseISIN_CODE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
			BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {

		if (this.getIteratorObject() == null) {
			throw new MessageFormatException("Not Iterator Object found ");
		} else {
			AccountStatement subStatement = (AccountStatement) this.getIteratorObject();

			Product security = BOCache.getExchangedTradedProduct(dsCon, subStatement.getAccountId());
			if (security == null) {
				return "";
			} else {
				String value = null;
				if (security instanceof Security) {
					security = ((Security) security).getSecurity();
				}
				if (!Util.isEmpty(security.getSecCode("ISIN"))) {
					value = "ISIN " + security.getSecCode("ISIN");

				} else {
					value = "";
				}
				return value;
			}
		}
	}
}


