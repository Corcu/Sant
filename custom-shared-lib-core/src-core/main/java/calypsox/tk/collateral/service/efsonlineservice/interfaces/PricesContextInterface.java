/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.interfaces;

import java.util.List;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanIN;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT;

/**
 * @author Guillermo
 *
 */
public interface PricesContextInterface{

	
	/**
	 * @return the bonds and equities subscription list
	 * 
	 */
	public List<QuoteBeanIN> getQuotesSubscription();
	
	/**
	 * @return the quotesResponses (the last one on the context).
	 */
	public  List<QuoteBeanOUT> getQuotesResponse();
	
	/**
	 * @param the bondsQuotes to set. These are the bonds subscriptions to be sent
	 * 			to the WS of EFS.
	 */
	public void setBondsQuotes(final List<QuoteBeanIN> bondsQuotes);

	/**
	 * @param equitiesQuotes the equitiesQuotes to set. These are the equities subscriptions to be sent
	 * 			to the WS of EFS.
	 */
	public void setEquitiesQuotes(final List<QuoteBeanIN> equitiesQuotes);
	
	/**
	 * @param instrumentQuotes to be subcribe. Internally makes the difference of bonds
	 * 	or equities subscriptions.
	 */
	public void setInstrumentsQuotes(final List<QuoteBeanIN> instrumentQuotes);


	/**
	 * @param quotesResponse the quotes to be set (last one read from EFS throught the WebService)
	 */
	public void setQuotesResponse(List<QuoteBeanOUT> quotesResponse);
	

}
