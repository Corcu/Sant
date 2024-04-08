package calypsox.tk.collateral.marginCall.reader;

import java.util.List;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;

public interface ExternalMarginCallReader {

	/**
	 * @param messages
	 * @return
	 * @throws Exception
	 */
	public List<? extends ExternalMarginCallBean> readMarginCalls(
			List<MarginCallImportErrorBean> errors) throws Exception;

	/**
	 * @param message
	 * @param messages
	 * @return
	 * @throws Exception
	 */
	public ExternalMarginCallBean readMarginCall(String message,
			List<MarginCallImportErrorBean> errors) throws Exception;

}
