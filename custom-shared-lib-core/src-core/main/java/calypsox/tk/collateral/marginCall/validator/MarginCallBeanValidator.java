package calypsox.tk.collateral.marginCall.validator;

import java.util.List;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCall;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;

public interface MarginCallBeanValidator {
	/**
	 * @param bean
	 * @return
	 */
	public boolean validate(ExternalMarginCall bean,
			List<MarginCallImportErrorBean> messages);
}
