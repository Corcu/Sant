/**
 * 
 */
package calypsox.tk.collateral.allocation.bean;

/**
 * @author aela
 *
 */
public class AllocImportErrorBean  extends CodeValueBean{
	
	protected ExternalAllocationBean allocBean;
	
	
	public AllocImportErrorBean() {
	}

	public AllocImportErrorBean(String errorMessage, ExternalAllocationBean allocBean) {
		this.allocBean = allocBean;
		setValue(errorMessage);
	}

	public AllocImportErrorBean(String errorCode, String errorMessage, ExternalAllocationBean allocBean) {
		this.allocBean = allocBean;
		setCode(errorCode);
		setValue(errorMessage);
	}

	/**
	 * @return the allocBean
	 */
	public ExternalAllocationBean getAllocBean() {
		return allocBean;
	}

	/**
	 * @param allocBean the allocBean to set
	 */
	public void setAllocBean(ExternalAllocationBean allocBean) {
		this.allocBean = allocBean;
	}
	
}
