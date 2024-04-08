package calypsox.tk.collateral.marginCall.persistor;

import java.util.List;

import com.calypso.tk.core.Trade;

public interface ExternalMarginCallPersistor {

	/**
	 * @param trade
	 * @param messages
	 * @throws Exception
	 */
	public void persistEntry(List<Trade> tradeToSave, List<String> messages)
			throws Exception;

}
