package calypsox.tk.collateral.marginCall.bean;

public class MarginCallImportErrorBean extends CodeValueBean {
	protected ExternalMarginCallBean mcBean;

	public MarginCallImportErrorBean() {
	}

	public MarginCallImportErrorBean(String errorMessage,
			ExternalMarginCallBean mcBean) {
		this.mcBean = mcBean;
		setValue(errorMessage);
	}

	public MarginCallImportErrorBean(String errorCode, String errorMessage,
			ExternalMarginCallBean mcBean) {
		this.mcBean = mcBean;
		setCode(errorCode);
		setValue(errorMessage);
	}

	/**
	 * @return the mcBean
	 */
	public ExternalMarginCallBean getAllocBean() {
		return mcBean;
	}

	/**
	 * @param mcBean
	 *            the mcBean to set
	 */
	public void setAllocBean(ExternalMarginCallBean mcBean) {
		this.mcBean = mcBean;
	}
}
