/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.interfaces;

import java.rmi.RemoteException;

import calypsox.tk.collateral.service.efsonlineservice.EFSException;

import com.calypso.tk.core.JDate;

/**
 * Generates a list with all the relevant positions from where to extract the bonds and equities (which have an impact
 * and are relevant). It extracts from these positions the ISIN and the CCY for each product and finally puts a copy in
 * the context.
 * 
 * @author Guillermo Solano
 * @version 1.1, 12/06/2013
 * 
 */
public interface BoISINProcessingInterface {

	/**
	 * Call the main Method to read the last bonds and prices from the database and write a set of quotes subscription
	 * on the context.
	 */
	public void readProductsFromDatabasePositions();

	// /////////////////////////////////////////////////////////
	// / METHOD 1 TO GATHER SECURITIES: THROUGH POSITIONS /////
	// ///////////////////////////////////////////////////////
	/**
	 * generates a list with all the relevant positions from where to extract the bonds and equities (which have an
	 * impact and are relevant). In other words, returns all the positions different from zero for products of type
	 * bonds and equities.
	 * 
	 * @param date
	 *            to be search for the positions
	 * @throws EFSException
	 */

	public void gatherBOPositions(final JDate today) throws RemoteException, EFSException;

	/**
	 * @return true if positions have been processed and the quotes isin and ccy have been extracted; false if the
	 *         dataList was empty (gatherBOPositions have to be called first).
	 * 
	 */
	public boolean retrieveISINsListFromPositions();

	// /////////////////////////////////////////////////////////
	// / METHOD 2 TO GATHER SECURITIES: THROUGH SEC. IDS //////
	// ///////////////////////////////////////////////////////
	/**
	 * @return a list with all the securities IDs from where to extract the bonds and equities. In other words, returns
	 *         all securities ids from positions different from zero for products of type bonds and equities.
	 * @param date
	 *            to be search for the positions
	 * @throws EFSException
	 */
	public void gatherBOSecuritiesIDs(final JDate today) throws EFSException;

	/**
	 * @return a list with all the securities IDs from where to extract the bonds and equities. In other words, returns
	 *         all securities ids from positions different from zero for products of type bonds and equities.
	 * @param date
	 *            to be search for the positions
	 * @throws EFSException
	 */
	public boolean retrieveISINsFromSecuritiesMap();

	/**
	 * From the list of bonds and equities that are relevant, this method updates the context with the final
	 * subscription list.
	 * 
	 * @throws EFSException
	 */
	public void updateContextSubscriptionList() throws EFSException;

	/**
	 * if the thread is running, this will stop it (stop signal)
	 */
	public void killUpdatingBOPostionsThread();

	/**
	 * @returns the execution time in seconds of the thread.
	 */
	public long getRunningTime();

	/**
	 * @return if the thread is alive (when enableThreading is true).
	 */
	public boolean isAlive();

	/**
	 * @return status of the thread. Number of executions and timers
	 */
	public String getStatusInfo();

}
