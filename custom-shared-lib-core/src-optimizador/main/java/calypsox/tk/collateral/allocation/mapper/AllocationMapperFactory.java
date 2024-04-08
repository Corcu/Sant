/**
 * 
 */
package calypsox.tk.collateral.allocation.mapper;

import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.pdv.importer.PDVConstants;

/**
 * 
 * Factory to get allocation mapper from the being imported alloc bean
 * 
 * @author aela
 * 
 */
public class AllocationMapperFactory {

	private static AllocationMapperFactory instance = null;
	private  ExternalAllocationImportContext context = null;
	
	/**
	 * @param context
	 */
	public AllocationMapperFactory(ExternalAllocationImportContext context) {
		this.context = context;
	}

	/**
	 * @param context
	 * @return
	 */
	public static synchronized AllocationMapperFactory getInstance(ExternalAllocationImportContext context) {
		if (instance == null) {
			instance = new AllocationMapperFactory(context);
		}
		return instance;
	}

	/**
	 * @param allocBean
	 * @return
	 */
	public ExternalAllocationMapper getAllocationMapper(
			ExternalAllocationBean allocBean) {
		if(allocBean !=null) {
			if (PDVConstants.PDV_COLLAT_TYPE.COLLAT_CASH.name().equals(allocBean.getCollateralType())) {
				return new CashAllocationMapper(context);
			} else if(PDVConstants.PDV_COLLAT_TYPE.COLLAT_SECURITY.name().equals(allocBean.getCollateralType())) {
				return new SecurityAllocationMapper(context);
			}
		}
		return null;
	}


}
