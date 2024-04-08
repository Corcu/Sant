/**
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.beans;

/**
 * @author aela
 *
 */
public class AllocImportErrorBean  extends CodeValueBean{
	public AllocImportErrorBean() {
	}

	public AllocImportErrorBean(String errorMessage) {
		setValue(errorMessage);
	}

	public AllocImportErrorBean(String errorCode, String errorMessage) {
		setCode(errorCode);
		setValue(errorMessage);
	}
}
