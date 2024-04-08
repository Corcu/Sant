package calypsox.tk.util.interfaceImporter;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.util.bean.InterfaceTradeBean;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.InterfaceTradeAndPLMarks;
import calypsox.util.TradeImportStatus;
import calypsox.util.TradeImportTracker;
import calypsox.util.TradeInterfaceUtils;

/**
 * Read
 * 
 * @author aela
 * 
 */
public class InterfaceTradePersistor extends ImportExecutor<InterfaceTradeAndPLMarks, InterfaceTradeAndPLMarks> {

	private List<InterfaceTradeAndPLMarks> persistenceBuffer = null;
	//AAP MIG14.4
//	private RemoteSantCollateralService remoteSantColService = null;

	private int tradeToSaveBufferSize = 5;

	public InterfaceTradePersistor(BlockingQueue<InterfaceTradeAndPLMarks> inWorkQueue,
			BlockingQueue<InterfaceTradeAndPLMarks> outWorkQueue, ImportContext context) {
		super(inWorkQueue, outWorkQueue, context);
		this.persistenceBuffer = new ArrayList<InterfaceTradeAndPLMarks>(this.tradeToSaveBufferSize);
		//AAP MIG14.4
//		this.remoteSantColService = (RemoteSantCollateralService) DSConnection.getDefault()
//				.getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);

	}

	public InterfaceTradePersistor(BlockingQueue<InterfaceTradeAndPLMarks> inWorkQueue,
			BlockingQueue<InterfaceTradeAndPLMarks> outWorkQueue, ImportContext context, int persistorBufferSize) {
		this(inWorkQueue, outWorkQueue, context);
		this.tradeToSaveBufferSize = persistorBufferSize;
	}

	@Override
	public InterfaceTradeAndPLMarks execute(InterfaceTradeAndPLMarks item) throws Exception {

		if (item == null) {
			return null;
		}
		this.persistenceBuffer.add(item);
		// save the trade with its PLMarks
		if (this.persistenceBuffer.size() >= this.tradeToSaveBufferSize) {

			saveTradeswithPlMarks(this.persistenceBuffer);
			this.persistenceBuffer.clear();

		}
		return null;
	}

	@Override
	public void finishPendingWork() {
		try {
			Log.debug(TradeInterfaceUtils.LOG_CATERGORY, getExecutorName() + "***********>end finishing pending work");
			saveTradeswithPlMarks(this.persistenceBuffer);
		} catch (RemoteException e) {
			Log.error(this, e);
		} finally {
			this.persistenceBuffer.clear();
		}
	}

	protected void saveTradeswithPlMarks(List<InterfaceTradeAndPLMarks> tradesToSave) throws RemoteException {

		if (Util.isEmpty(tradesToSave)) {
			return;
		}

		long start = System.currentTimeMillis();
		HashMap<Integer, InterfaceTradeBean> lineContents = new HashMap<Integer, InterfaceTradeBean>();
		List<InterfaceTradeAndPLMarks> notNullTradesToSave = new ArrayList<InterfaceTradeAndPLMarks>();

		for (InterfaceTradeAndPLMarks tradePlMark : tradesToSave) {

			if (tradePlMark != null) {
				lineContents.put(tradePlMark.getTradeBean().getLineNumber(), tradePlMark.getTradeBean());
				notNullTradesToSave.add(tradePlMark);
			}
		}

		if (Util.isEmpty(notNullTradesToSave)) {
			return;
		}
		// call service to save the PLMarks for the CollateralExposure trade
		Map<Integer, List<TradeImportStatus>> saveResults = CollateralUtilities
				.saveTradesWithPLMarks(notNullTradesToSave);

		if ((saveResults != null) && (saveResults.size() > 0)) {

			for (Integer lineNumber : saveResults.keySet()) {

				InterfaceTradeBean tradeBean = lineContents.get(lineNumber);
				List<TradeImportStatus> savedTradesStatus = saveResults.get(lineNumber);

				if (!Util.isEmpty(savedTradesStatus)) {
					int tradeNbLegs = 1;
					if (tradeBean != null) {
						tradeNbLegs = (tradeBean.getLegTwo() == null ? 1 : 2);
					}
					if (TradeImportTracker.isThereAnyError(savedTradesStatus)) {
						for (TradeImportStatus error : savedTradesStatus) {
							error.setTradeBean(tradeBean);
							this.context.getTradeImportTracker().addError(error);
						}
						this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs,
								TradeImportTracker.KO_ERROR);
					} else {
						TradeImportStatus ok = savedTradesStatus.get(0);
						ok.setRowBeingImportedNb(lineNumber);
						this.context.getTradeImportTracker().addOK(ok);
						this.context.getTradeImportTracker().incrementOKImports(tradeNbLegs);
					}
				}
			}
		}
		long end = System.currentTimeMillis();
		Log.debug(TradeInterfaceUtils.LOG_CATERGORY, getExecutorName() + "***********>end saving "
				+ notNullTradesToSave.size() + " trades in " + (end - start));
	}

	/**
	 * @return the hasToContinue
	 */
	@Override
	public boolean getHasToContinue() {
		return this.context.isTradeMapperRunning();
	}

	/**
	 * Stops this process
	 */
	@Override
	protected void stopProcess() {
		this.context.stopTradePersistorProcess();
	}

	public List<InterfaceTradeAndPLMarks> getPersistenceBuffer() {
		return this.persistenceBuffer;
	}

	public void setPersistenceBuffer(List<InterfaceTradeAndPLMarks> persistenceBuffer) {
		this.persistenceBuffer = persistenceBuffer;
	}

	public int getTradeToSaveBufferSize() {
		return this.tradeToSaveBufferSize;
	}

	public void setTradeToSaveBufferSize(int tradeToSaveBufferSize) {
		this.tradeToSaveBufferSize = tradeToSaveBufferSize;
	}
}