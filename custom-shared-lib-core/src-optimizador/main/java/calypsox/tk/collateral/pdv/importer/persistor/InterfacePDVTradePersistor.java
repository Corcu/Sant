package calypsox.tk.collateral.pdv.importer.persistor;

import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.tk.util.interfaceImporter.ImportContext;
import calypsox.tk.util.interfaceImporter.InterfaceTradePersistor;
import calypsox.util.*;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class InterfacePDVTradePersistor extends InterfaceTradePersistor {


	public InterfacePDVTradePersistor(
			BlockingQueue<InterfaceTradeAndPLMarks> inWorkQueue,
			BlockingQueue<InterfaceTradeAndPLMarks> outWorkQueue,
			ImportContext context, int persistorBufferSize) {
		super(inWorkQueue, outWorkQueue, context, persistorBufferSize);
	}

	protected void saveTradeswithPlMarks(
			List<InterfaceTradeAndPLMarks> tradesToSave) throws RemoteException {

		if (Util.isEmpty(tradesToSave)) {
			return;
		}

		long start = System.currentTimeMillis();
		HashMap<Integer, InterfaceTradeBean> lineContents = new HashMap<Integer, InterfaceTradeBean>();
		List<InterfaceTradeAndPLMarks> notNullTradesToSave = new ArrayList<InterfaceTradeAndPLMarks>();

		for (InterfaceTradeAndPLMarks tradePlMark : tradesToSave) {
			if (!Util.isEmpty(tradePlMark.getTradeBean().getDeliveryType())) {
				// Add PDV Keywords
				tradePlMark.getTrade().addKeyword(PDVUtil.DVP_FOP_TRADE_KEYWORD,
						PDVUtil.getDeliveryType(tradePlMark.getTrade().getQuantity() < 0 ? PDVUtil.EnumProductType.SECURITY_LENDING : PDVUtil.EnumProductType.SECURITY_BORROWING,
								tradePlMark.getTradeBean().getDeliveryType()));
			}
			tradePlMark.getTrade().addKeyword(PDVUtil.IS_FINANCEMENT_TRADE_KEYWORD, tradePlMark.getTradeBean().getIsFinancement());

			// Add MC_CONTRACT_NUMBER, if many add nothing
			List<CollateralConfig> eligibleMarginCallConfigs = PDVUtil.getCollateralConfig(tradePlMark.getTrade());
			if (eligibleMarginCallConfigs != null && eligibleMarginCallConfigs.size() == 1) {
				CollateralConfig mcc = eligibleMarginCallConfigs.get(0);
				if (mcc != null) {
					tradePlMark.getTrade().addKeyword(PDVConstants.MC_CONTRACT_NUMBER_TRADE_KEYWORD, mcc.getId());
				}
			} else if (!Util.isEmpty(tradePlMark.getTrade().getKeywordValue(PDVConstants.MC_CONTRACT_NUMBER_TRADE_KEYWORD))) {
				tradePlMark.getTrade().addKeyword(PDVConstants.MC_CONTRACT_NUMBER_TRADE_KEYWORD, "");
			}
			
			lineContents.put(tradePlMark.getTradeBean().getLineNumber(),
					tradePlMark.getTradeBean());
			notNullTradesToSave.add(tradePlMark);
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
				List<TradeImportStatus> savedTradesStatus = saveResults
						.get(lineNumber);

				if (!Util.isEmpty(savedTradesStatus)) {
					int tradeNbLegs = 1;
					if (tradeBean != null) {
						tradeNbLegs = (tradeBean.getLegTwo() == null ? 1 : 2);
					}
					if (TradeImportTracker.isThereAnyError(savedTradesStatus)) {
						for (TradeImportStatus error : savedTradesStatus) {
							error.setTradeBean(tradeBean);
							this.context.getTradeImportTracker()
									.addError(error);
						}
						this.context.getTradeImportTracker()
								.incrementKOImports(tradeNbLegs,
										TradeImportTracker.KO_ERROR);
					} else {
						TradeImportStatus ok = savedTradesStatus.get(0);
						ok.setRowBeingImportedNb(lineNumber);
						this.context.getTradeImportTracker().addOK(ok);
						this.context.getTradeImportTracker()
								.incrementOKImports(tradeNbLegs);
					}
				}
			}
		}
		long end = System.currentTimeMillis();
		Log.debug(TradeInterfaceUtils.LOG_CATERGORY, getExecutorName()
				+ "***********>end saving " + notNullTradesToSave.size()
				+ " trades in " + (end - start));
	}
}
