/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.interfaces;

/**
 * Reads the last quotes processed from EFS and stored in the context and stores the quotes in Calypso DB. In other
 * words, makes the context cache become persistance in Calypso's DB.
 * 
 * @author Guillermo Solano
 * @version 1.1, 12/06/20
 * 
 */
public interface QuotesDBPersistanceInterface {

	/**
	 * Reads from the cache (context) the last prices received from the webservice and stores the quotes in the
	 * quoteSet. From the context, get the last quotes read from EFS, and makes this data persistance (save on Calypso's
	 * DB).
	 */
	public void updateDatabase();

	/**
	 * @returns the execution time in seconds of the thread.
	 */
	public long getRunningTime();

	/**
	 * @return if the thread is alive (when enableThreading is true).
	 */
	public boolean isAlive();

	/**
	 * Stops the thread implementing this interface
	 */
	public void killUpdateDataseThread();

	/**
	 * @return status of the thread. Number of executions and timers
	 */
	public String getStatusInfo();

}
