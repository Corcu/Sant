package calypsox.engine.im.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import calypsox.engine.im.export.QEFJMSMessageWrapper;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import calypsox.engine.im.SantInitialMarginBaseEngine;
import calypsox.engine.im.errorcodes.SantInitialMarginCalypsoErrorCodeEnum;
import calypsox.engine.im.simulator.input.SantInitialMarginSimulatorImInput;
import calypsox.engine.im.simulator.output.SantInitialMarginSimulatorOutput;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.util.SantReportingUtil;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.DFA_INPUT_SIMULATED_FIELDS;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationLogic;

public class SantInitialMarginSimulatorEngine extends SantInitialMarginBaseEngine {

	/**
	 * Name of the service
	 */
	public static final String ENGINE_NAME = "SANT_InitialMargin_Simulator";

	/*
	 * 
	 * 
	 */
	@Override
	protected synchronized void init(EngineContext engineContext) {
		super.init(engineContext);
		setEngineName(ENGINE_NAME);
	}
	
//	private static SimpleDateFormat sdf_HHmm = new SimpleDateFormat("HHmm");

	public SantInitialMarginSimulatorEngine(final DSConnection dsCon, final String hostName,
			final int esPort) {
		super(dsCon, hostName, esPort);
	}

	@Override
	public String getEngineName() {
		return ENGINE_NAME;
	}

	@Override
	protected boolean isAcceptedEvent(PSEvent psEvent) {
		return false;
	}

	@Override
	public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
		return false;
	}

	@Override
	public List<QEFJMSMessageWrapper> handleOutgoingJMSMessage(PSEvent event, List<Task> tasks) throws Exception {
		return null;
	}

	@Override
	protected JMSQueueAnswer importMessage(String message, List<Task> tasks) throws Exception {
		return null;
	}

	@Override
	public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
		JMSQueueAnswer answer = null;

		if (externalMessage == null) {
			return true;
		}

		// Display the message
		Log.info(this, externalMessage.getText());

		SantInitialMarginSimulatorImInput inputReceived = new SantInitialMarginSimulatorImInput();
		SantInitialMarginCalypsoErrorCodeEnum formatterResult = inputReceived.parseInfo(externalMessage);

		// if msg could be parsed correctly
		if (formatterResult.getCode() == 0) {
			// process the request
			int matchedContractId = simulateTrade(inputReceived);

			try {
				// send answer to QEF
				SantInitialMarginSimulatorOutput output = new SantInitialMarginSimulatorOutput();
				output.setContractId(matchedContractId);
				output.setContractCcy(inputReceived.getCurrency());

				final JMSQueueMessage jmsMessage = (JMSQueueMessage) externalMessage;

				answer = new JMSQueueAnswer();
				answer.setText(output.generateOutput());
				answer.setCorrelationId(jmsMessage.getCorrelationId());
				answer.setReference(jmsMessage.getReference());

				sendAnswer(answer);
			} catch (Exception ex) {
				StringBuilder msg = new StringBuilder("Simulation could not finish properly. Request: ");
				msg.append(externalMessage.getText()).append('\n');
				msg.append(ex.getMessage());

				Log.error(this, msg.toString());
			}
		}
		// TODO need to publish Task when not parsed correctly??

		// List<Task> tasks = TaskErrorUtil.getTaskErrors(TaskErrorUtil.EnumOptimProcessType.OPTIMIZER_MARGIN_CALL,
		// externalMessage);
		// if (!Util.isEmpty(tasks)) {
		// Log.system(SantInitialMarginExportEngine.class.getName(),
		// "Publishing task from message: " + externalMessage.getText());
		// publishTask(tasks);
		// Log.debug(SantInitialMarginExportEngine.class.getName(), "Task(s) published: " + tasks.toString());
		// }
		return true;
	}

	private int simulateTrade(SantInitialMarginSimulatorImInput inputReceived) {
		int mccId = 0;

		// parser to input map
		Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap = TradeCollateralizationLogic.getMap(inputReceived);

		// generate fake trade to simulate the collateralization degree
		final Trade simulatedTrade = TradeCollateralizationLogic.buildTradeFromInputMap(inputMap, null);

		final List<CollateralConfig> suitableMrgContractList = findSuitableCollateralContractsForTrade(simulatedTrade,
				inputMap);
 
		if (suitableMrgContractList.size() == 1) {
			mccId = suitableMrgContractList.get(0).getId();
		}

		return mccId;
	}

	/*
	 * In case a MC contract is found (only one and is suitable) it will be returned
	 * 
	 * @param trade trade to be checked if it matches a suitable MC contract
	 * 
	 * @return List of suitable Contracts. If everything is ok, it should appear one contract in the list.
	 */
	private static List<CollateralConfig> findSuitableCollateralContractsForTrade(final Trade trade,
			final Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap) {

		ArrayList<CollateralConfig> eligibleCollateralConfigs = null;
		final LegalEntity poLe = BOCache.getLegalEntity(DSConnection.getDefault(),
				inputMap.get(DFA_INPUT_SIMULATED_FIELDS.PROCESSING_ORG));
		final int PO_id = poLe.getId();

		// Log.debug("TradeCollateralizationLogic", "1. getMrgCallContractForTrade -> Trade BO id = "
		// + getTradeBOKeyword(trade) + ". Trade BO System" + getTradeSystemKeyword(trade));

		if (trade == null) {
			Log.error(TradeCollateralizationLogic.class,
					"No trade received as parameter in getMrgCallContractForTrade method.");
			return null;
		}
		// start
		try {

			eligibleCollateralConfigs = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
					.getEligibleCollateralConfigs(trade, PO_id);

		} catch (final Exception e) {
			// Log.error(TradeCollateralizationLogic.class,
			// "Error finding MCContract for the trade BO: " + getTradeBOKeyword(trade), e);
			return null;
		}
		Log.debug("TradeCollateralizationLogic", "2. getMrgCallContractForTrade -> end");

		return eligibleCollateralConfigs;
	}

}
