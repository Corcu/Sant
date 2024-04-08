package calypsox.tk.swift.formatter;

import java.util.Vector;

import com.calypso.tk.bo.AccountStatement;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.FormatterIterator;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

/**
 * Iterator class for the MT535Korea.xml template
 * @author x957355
 *
 */
public class MT535KoreaIterator extends FormatterIterator {
	 protected BOMessage _message;
	     protected Trade _trade;
	     protected LEContact _sender;
	     protected LEContact _receiver;
	     protected BOTransfer _transfer;
	     protected Vector _rules;
	     protected PricingEnv _env;
	     protected DSConnection _dsCon;
	     protected int _count = 0;
	     protected TransferArray _transfers = null;
	/**  
	 * Init the iterator for the MT535Korea template
	 */
	@Override
	public void init(BOMessage message, Trade trade, LEContact sender, LEContact receiver, BOTransfer transfer,
			Vector transferRules, PricingEnv env, DSConnection dsCon) {

		String data = message.getAttribute("ExternalPosition");
		String SwiftQuantity = DomainValues.comment("KOREA.MT535fields", "SwiftQuantity");
		String [] externalPositions = data.split(";");
		
		Vector<AccountStatement> subStatements = new Vector<AccountStatement>();
		 for (String externalPos: externalPositions) {
			 AccountStatement acc = new AccountStatement();
			 double balance = Double.parseDouble(externalPos.split("-")[1]);
			 acc.setAccountId(getProductId(externalPos.split("-")[0],dsCon));
			 acc.setClosingBalance(balance);
			 acc.setAttribute("SwiftQuantity", SwiftQuantity); //UNIT for Quantity
			 
			 subStatements.add(acc);
			 
		 }
		 this.setIteratorSet(subStatements);
	}
	
	/**
	 * Obtains the product id by the ISIN code
	 * @param isin isin code of the product
	 * @param dsCon ds connection objet
	 * @return product id of the product.
	 */
	private int getProductId(String isin, DSConnection dsCon) {
		
		int id = 0;
		try {
			Product product = dsCon.getRemoteProduct().getProductByCode("ISIN", isin);
			if(product != null) {
				id = product.getId();
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Cannot obatain the product");
		}
		return id;
		
	}
	 
	 

}
