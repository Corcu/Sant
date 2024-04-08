package calypsox.tk.collateral.allocation.validator;

import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocation;

/**
 * @author aela
 *
 */
public interface AllocationBeanValidator {

	/**
	 * @param bean
	 * @return
	 */
	public boolean validate(ExternalAllocation bean, List<AllocImportErrorBean> messages);

}
