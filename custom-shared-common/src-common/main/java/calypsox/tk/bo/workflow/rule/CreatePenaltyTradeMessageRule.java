package calypsox.tk.bo.workflow.rule;

import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.BOTransferUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.service.DSConnection;

/**
 * @author aalonsop
 */
public class CreatePenaltyTradeMessageRule extends com.calypso.tk.bo.workflow.rule.CreatePenaltyTradeMessageRule {

	private final static String oCptyId = "OriginalCounterpartyId";
	private final static String oCptyName = "OriginalCounterpartyName";
	private final static String CSDR_MANUAL_CONTINGENCY = "CSDRContingencyDailyPenalty";
	private final static String PENALTY_DATE = "PenaltyDate";


	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		if (!"PENA".equals(message.getAttribute("Message_Function"))) {
			return true;
		}

		updatePENAMsg(message, trade, transfer, dsCon, dbCon);

		if (checkManualContingencyAttributes(message, trade, transfer)) {
			if (message.getAttribute(CSDR_MANUAL_CONTINGENCY) == null
					|| !message.getAttribute(CSDR_MANUAL_CONTINGENCY).equals("Y")) { // No contingency MT537
				String penaltyRef = message.getAttribute("PenaltyRef");
				String penaltyComRef = message.getAttribute("PenaltyComRef");
				long[] idsTrades;
				try {
					// Check if exists penalty operation with the current PenaltyRef
					idsTrades = dsCon.getRemoteTrade().getTradeIdsByKeywordNameAndValue("PenaltyRef", penaltyRef);

					// There are penalty operations with this PenaltyRef --> FIRST COMPROBATION
					if (idsTrades.length != 0) {
						Long idOpContingency = null;
						for (long id : idsTrades) {
							if (idOpContingency != null) {
								break;
							}
							Trade penaltyOp = dsCon.getRemoteTrade().getTrade(id);
							// Penalty Operation has keyword CSDRContingencyDailyPenalty equals Y --> SECOND
							// COMPROBATION
							if (penaltyOp.getKeywordValue(CSDR_MANUAL_CONTINGENCY) != null	// HAS CONTINGENCY OP BEFORE
									&& penaltyOp.getKeywordValue(CSDR_MANUAL_CONTINGENCY).equals("Y") && !penaltyOp.getStatus().equals(Status.S_CANCELED)) {
								idOpContingency = id;
							}
						}
						if (idOpContingency != null) {  // Penalty operation has the keyword value Y OR Penalty Operation Before
							try {
								Trade tradeOp = dsCon.getRemoteTrade().getTrade(idOpContingency);
								if(tradeOp.getKeywordValue("PenaltyRef").equals(penaltyRef)
										|| tradeOp.getKeywordValue("PenaltyComRef").equals(penaltyComRef)) {		// CHECK PENALTY REF or PENALTY COM REF

								tradeOp = (Trade) tradeOp.cloneIfImmutable();
									tradeOp = (Trade) tradeOp.cloneIfImmutable();
									tradeOp.setAction(Action.CANCEL); // Cancel contingency trade
									dsCon.getRemoteTrade().save(tradeOp);

									boolean result = super.update(wc, message, oldMessage, trade, transfer, messages,
											dsCon, excps, task, dbCon, events); // Create last penalty operation


									return result;

								}
							} catch (Exception e) {
								Log.error(this, e);
								return false;
							}
						}
					}
				}

				catch (CalypsoServiceException e) {
					Log.error(this, e);
				}
			}
		}
		return super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
	}

	void updatePENAMsg(BOMessage message, Trade trade, BOTransfer transfer, DSConnection dsCon, Object dbCon) {
		if (isPENA(message)) {
			Trade reloadedTrade = getTradeIfNull(trade, message);
			if (reloadedTrade != null && reloadedTrade.getProduct() instanceof SimpleTransfer) {
				addOriginalCptyAttrFromTrade(message, reloadedTrade);
			} else {
				addOriginalCptyAttrFromNettedTransfer(message, transfer, dsCon, dbCon);
			}
		}
	}

	private Trade getTradeIfNull(Trade trade, BOMessage message) {
		Trade resTrade = trade;
		long tradeId = Optional.ofNullable(message).map(BOMessage::getTradeLongId).orElse(0L);
		if (resTrade == null && tradeId > 0L) {
			try {
				resTrade = TradeSQL.getTrade(tradeId);
			} catch (PersistenceException exc) {
				Log.error(this, exc.getCause());
			}
		}
		return resTrade;
	}

	/**
	 * Applies to external penalty trades only
	 * 
	 * @param message
	 * @param trade
	 */
	private void addOriginalCptyAttrFromTrade(BOMessage message, Trade trade) {
		addOriginalCptyAttrToMsg(message, trade.getKeywordValue(oCptyId), trade.getKeywordValue(oCptyName));
	}

	private boolean checkManualContingencyAttributes(BOMessage message, Trade trade, BOTransfer transfer) {
		if (message.getAttribute("Message_Function") != null
				&& message.getAttribute("Message_Function").equals("PENA")) { // Is penal
			if (message.getAttribute("Frequency_Indicator") != null
					&& message.getAttribute("Frequency_Indicator").equals("DAIL")) {
				if (message.getAttribute("Penalty_Reason") != null
						&& message.getAttribute("Penalty_Reason").contains("NEWP")) { // Creation messag{ // Is daily
					return true;
				}
			}
		}
		return false;
	}

	private void addOriginalCptyAttrToMsg(BOMessage message, String oCptyIdValue, String oCptyNameValue) {
		message.setAttribute(oCptyId, oCptyIdValue);
		message.setAttribute(oCptyName, oCptyNameValue);
	}

	private void addOriginalCptyAttrFromNettedTransfer(BOMessage boMessage, BOTransfer xfer, DSConnection dsCon,
			Object dbCon) {
		if (boMessage.getTransferLongId() > 0L) {
			if (xfer == null) {
				xfer = BOTransferUtil.getTransfer(boMessage, dsCon, dbCon);
			}
			if (isMultitradeXferNet(xfer)) {
				LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), xfer.getOriginalCptyId());
				if (le != null) {
					addOriginalCptyAttrToMsg(boMessage, String.valueOf(le.getId()), le.getCode());
				}
			}
		}
	}

	private boolean isMultitradeXferNet(BOTransfer xfer) {
		return xfer != null && xfer.getNettedTransfer() && xfer.getTradeLongId() == 0L;
	}

	boolean isPENA(BOMessage message) {
		boolean res = false;
		String pena = "PENA";
		String msgFunction = "Message_Function";
		if (pena.equals(message.getAttribute(msgFunction))) {
			res = true;
		}
		return res;
	}

}
