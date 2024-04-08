/**
 * 
 */
package calypsox.tk.bo.document;

/**
 * @author fperezur
 *
 */
public class JMSSLBKPlusDocumentSender extends JMSKPlusDocumentSender {

	/**
	 * The constant CONFIG_NAME_SLB_KONDOR_PLUS
	 */
	private static final String CONFIG_NAME_SLB_KONDOR_PLUS = "SantanderSLBKondorPlus";
	
	/* (non-Javadoc)
	 * @see calypsox.tk.bo.document.JMSKPlusDocumentSender#getConfigName()
	 */
	@Override
	public String getConfigName() {
		return CONFIG_NAME_SLB_KONDOR_PLUS;
	}



	
}
