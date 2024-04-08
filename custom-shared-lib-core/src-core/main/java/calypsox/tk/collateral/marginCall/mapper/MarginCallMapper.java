package calypsox.tk.collateral.marginCall.mapper;

import java.util.List;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.marginCall.validator.ExternalMarginCallBeanValidator;

import com.calypso.tk.product.MarginCall;

public class MarginCallMapper extends AbstractExternalMarginCallMapper {

	/**
	 * @param context
	 */
	public MarginCallMapper(ExternalMarginCallImportContext context) {
		super(context);
	}

	@Override
	public boolean isValidMarginCall(ExternalMarginCallBean mcBean,
			MarginCall mc, List<MarginCallImportErrorBean> messages)
			throws Exception {
		return (new ExternalMarginCallBeanValidator(mc, context).validate(
				mcBean, messages));
	}

}
