/**
 * 
 */
package calypsox.tk.collateral.util.processor;

/**
 * @author aela
 *
 */
public interface ProcessExecutorLauncher {
	public boolean isProcessStillRunning();
	public void setExecutorFactory(ProcessExecutorFactory factory);
	public void start() throws Exception;
	public void shutDown();
}
