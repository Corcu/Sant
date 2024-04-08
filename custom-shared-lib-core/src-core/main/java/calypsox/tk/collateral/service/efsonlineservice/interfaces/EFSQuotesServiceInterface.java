package calypsox.tk.collateral.service.efsonlineservice.interfaces;

import calypsox.tk.collateral.service.efsonlineservice.EFSException;

import com.isban.efs2.webservice.Efs2Response;

/**
 * Reads the isins subscriptions from the context. Generates the appropiate request xml and afterwards calls the
 * webservice. When it recieved the response, builds the quotes result and stores it in the context.
 * 
 * @author Guillermo Solano
 * @version 1.1, 12/06/2013
 * 
 */
public interface EFSQuotesServiceInterface {

	/**
	 * Calls the web service. First generates the XML petition from the last quotes subscription available on the
	 * context, generates the XML message. Then calls the webservice and finally processes the response.
	 */
	public void startEfsWebService();

	/**
	 * makes the call to the EFS Webservice using their configuration parameters.
	 * 
	 * @param xml
	 *            with the input message with the format specified in the DDS.
	 * @return a valid response from the EFS system
	 * @throws EFSException
	 */
	public Efs2Response callEFSWebService(final String xml) throws EFSException;

	/**
	 * @returns the execution time in seconds of the thread.
	 */
	public long getRunningTime();

	/**
	 * @return if the thread is alive (when enableThreading is true).
	 */
	public boolean isAlive();

	/**
	 * Stops the webservice thread
	 */
	public void killEfsWebService();

	/**
	 * @return status of the thread. Number of executions and timers
	 */
	public String getStatusInfo();

}
