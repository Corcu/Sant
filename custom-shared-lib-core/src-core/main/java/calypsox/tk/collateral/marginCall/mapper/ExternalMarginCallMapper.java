package calypsox.tk.collateral.marginCall.mapper;

import java.util.List;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;

import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;

public interface ExternalMarginCallMapper {

	public Trade mapMarginCallTrade(ExternalMarginCallBean mcBean,
			List<MarginCallImportErrorBean> messages) throws Exception;

	public boolean isValidMarginCall(ExternalMarginCallBean mcBean,
			MarginCall mc, List<MarginCallImportErrorBean> messages)
			throws Exception;

}
