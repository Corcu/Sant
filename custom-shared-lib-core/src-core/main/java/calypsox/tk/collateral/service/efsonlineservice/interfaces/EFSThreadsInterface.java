/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.interfaces;

/**
 * @author Guillermo Solano
 */
public interface EFSThreadsInterface {

	/**
	 * Main method of ThreadController, contains the main logic to be executed under the thread. All threads implement
	 * directly this method to allow the thread call from the super class
	 */
	public void runThreadMethod();

}
