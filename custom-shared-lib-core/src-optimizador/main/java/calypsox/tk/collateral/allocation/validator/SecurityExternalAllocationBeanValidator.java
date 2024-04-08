/**
 * 
 */
package calypsox.tk.collateral.allocation.validator;

import java.rmi.RemoteException;
import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocation;
import calypsox.tk.collateral.allocation.bean.SecurityExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImportConstants;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

/**
 * @author aela
 * 
 */
public class SecurityExternalAllocationBeanValidator extends
		ExternalAllocationBeanValidator {

	public SecurityExternalAllocationBeanValidator(MarginCallEntry entry, ExternalAllocationImportContext context) {
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

		// check that security is accepted by the contract
		isValid = isValid && checkISINAndEligibleSecurity(allocBean, messages);

		// check the security price
		isValid = isValid && checkAssetPrice(allocBean, messages);

		return isValid;
	}

	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	private boolean checkAssetPrice(SecurityExternalAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		if (allocBean.getAssetPrice() == null
				|| allocBean.getAssetPrice().doubleValue() <= 0.0) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_ISIN_PRICE_NOT_VAIDE,
					"Invalid security price : "
							+ (allocBean.getAssetPrice() == null ? "null"
									: allocBean.getAssetPrice().doubleValue()),allocBean));
			return false;
		}
		return true;
	}

	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	private boolean checkISINAndEligibleSecurity(
			SecurityExternalAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		List<String> sdfs = getMcc().getEligibilityFilterNames();
		boolean isEligibleSecurity = false;
		if (!Util.isEmpty(sdfs)) {
			Product p = getProductForISIN(allocBean, messages);
			if (p == null) {
				return false;
			}
			for (String sdf : sdfs) {
				StaticDataFilter realSDF = BOCache.getStaticDataFilter(
						DSConnection.getDefault(), sdf);
				if ((realSDF != null)
						&& realSDF.accept(null, p)) {
					isEligibleSecurity = true;
					break;
				}
			}
		}

		if (!isEligibleSecurity) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_ISIN_NOT_ELIGIBLE,
					"The security with ISIN code "
							+ (Util.isEmpty(allocBean.getAssetISIN()) ? ""
									: allocBean.getAssetISIN())
							+ " is not eligible to be used with the contract "
							+ getMcc().getName(),allocBean));

		}
		return isEligibleSecurity;
	}

	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	private Product getProductForISIN(SecurityExternalAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		Product sec = null;
		try {
			sec = DSConnection.getDefault().getRemoteProduct()
					.getProductByCode("ISIN", allocBean.getAssetISIN());
		}
		catch (RemoteException e) {
			Log.error(this, e);
		}

		if (sec == null) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_ISIN_NOT_FOUND,
					"Unable to find a security with the ISIN code "
							+ (Util.isEmpty(allocBean.getAssetISIN()) ? ""
									: allocBean.getAssetISIN()),allocBean));
			return null;
		}
		return sec;
	}
}
