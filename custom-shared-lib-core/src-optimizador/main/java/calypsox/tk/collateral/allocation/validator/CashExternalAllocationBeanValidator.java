/**
 * 
 */
package calypsox.tk.collateral.allocation.validator;

import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocation;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.bean.SecurityExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportConstants;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.refdata.CollateralConfigCurrency;

/**
 * @author aela
 * 
 */
public class CashExternalAllocationBeanValidator extends
		ExternalAllocationBeanValidator {

	public CashExternalAllocationBeanValidator(MarginCallEntry entry, ExternalAllocationImportContext context) {
		super(entry, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.allocation.validator.AllocationBeanValidatorInterface
	 * #
	 * validate(calypsox.tk.collateral.allocation.bean.ExternalAllocationInterface
	 * , java.util.List)
	 */
	@Override
	public boolean validate(ExternalAllocation bean,
			List<AllocImportErrorBean> messages) {

		SecurityExternalAllocationBean allocBean = (SecurityExternalAllocationBean) bean;

		boolean isValid = super.validate(allocBean, messages);
		// check currency
		isValid = isValid && checkEligibleCurrency(allocBean, messages);

		return isValid;
	}
	
	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	private boolean checkEligibleCurrency(ExternalAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		boolean isEligibleCurrency = false;
		List<CollateralConfigCurrency> currencies = getMcc().getEligibleCurrencies();
		if (!Util.isEmpty(currencies)) {
			for (CollateralConfigCurrency ccy : currencies) {
				if (ccy.getCurrency().equals(allocBean.getAssetCurrency())) {
					isEligibleCurrency = true;
					break;
				}
			}
		}
		if (!isEligibleCurrency) {
			messages.add(new AllocImportErrorBean(
					ExternalAllocationImportConstants.ERR_CCY_NOT_ELIGIBLE,
					"The currency " + allocBean.getAssetCurrency()
							+ " is not eligible to be used with the contract "
							+ getMcc().getName(),allocBean));
		}
		return isEligibleCurrency;
	}

	
}
