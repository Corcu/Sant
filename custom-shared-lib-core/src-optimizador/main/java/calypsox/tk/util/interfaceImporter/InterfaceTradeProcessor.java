package calypsox.tk.util.interfaceImporter;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.util.bean.InterfaceTradeBean;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

import calypsox.util.InterfaceTradeAndPLMarks;
import calypsox.util.TradeImportTracker;

/**
 * Save trades with their MTM information (call to the DataServer)
 *
 * @author aela
 *
 */
public class InterfaceTradeProcessor extends ImportExecutor<InterfaceTradeBean, InterfaceTradeAndPLMarks> {

	private InterfaceTradeMapper tradeMapper = null;

	public final static String CONUK = "CONUK";

	/**
	 * Constructor
	 *
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param context
	 */
	public InterfaceTradeProcessor(Map<String, SantTradeContainer> tradesCacheMap,BlockingQueue<InterfaceTradeBean> inWorkQueue,
								   BlockingQueue<InterfaceTradeAndPLMarks> outWorkQueue, ImportContext context) {

		super(inWorkQueue, outWorkQueue, context);
		this.tradeMapper = new InterfaceTradeMapper(tradesCacheMap,context);

	}

	/**
	 * Returns the Trade & PL Marks of the trade from the tradeBean
	 */
	@Override
	public InterfaceTradeAndPLMarks execute(InterfaceTradeBean tradeBean) throws Exception {

		if (tradeBean == null) {
			return null;
		}

		int nbLegs = (tradeBean.getLegTwo() == null ? 1 : 2);
		Trade trade = null;
		InterfaceTradeAndPLMarks tradeWithPlMarks = null;

		try {
			trade = this.tradeMapper.map(tradeBean);
			if(isCloned(trade)){
				trade.setExternalReference(CONUK+trade.getExternalReference());
			}
			// GSM: 15/07/2013. Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
			if (this.tradeMapper.excludeFilter(trade)) {
				// exclusion log
				final int errorType = TradeImportTracker.KO_EXCLUDED;
				this.context.getTradeImportTracker().addExclusion(tradeBean);
				this.context.getTradeImportTracker().incrementKOImports(nbLegs, errorType);
			}

			// trade must not be excluded, check if is a valid trade
			else if (this.tradeMapper.isValid(trade, tradeBean)) {

				tradeWithPlMarks = this.tradeMapper.getTradeAndPlMarks(trade, tradeBean);

			} else { // not valid, check if error or warning reason

				int errorType = TradeImportTracker.KO_ERROR;
				if (!tradeBean.isErrorChecks() && tradeBean.isWarningChecks()) {
					errorType = TradeImportTracker.KO_WARNING;
				}
				this.context.getTradeImportTracker().incrementKOImports(nbLegs, errorType);
			}

		} catch (Exception e) { // unknown error

			Log.error(this, e);
			this.context.getTradeImportTracker().addError(tradeBean, 5, " Bad record format");
			this.context.getTradeImportTracker().incrementKOImports(nbLegs, TradeImportTracker.KO_ERROR);
		}

		return tradeWithPlMarks;
	}

	/**
	 * if the thread must continue
	 */
	@Override
	public boolean getHasToContinue() {

		return this.context.isFileReaderRunning();

	}

	/**
	 * Stops de process
	 */
	@Override
	protected void stopProcess() {
		this.context.stopTradeMapperProcess();
	}

	/**
	 * Method that allow to know if this trade is cloned or not
	 * @param trade
	 * @return
	 */
	public boolean isCloned(Trade trade){
		boolean result = false;

		if (null != trade && null != trade.getKeywordValue("BO_REFERENCE")) {
			if (trade.getKeywordValue("BO_REFERENCE").contains(CONUK)
					&& !trade.getExternalReference().contains(CONUK)) {
				result = true;
			}
		}
		return result;
	}

}