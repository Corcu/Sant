package calypsox.tk.upload.uploader;

import java.sql.Connection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import calypsox.tk.upload.mapper.MurexTransferAgentMapper;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.SDISelector;
import com.calypso.tk.bo.SDISelectorUtil;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.UploadBOException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.services.UploaderKeyManager;



public class UploadCalypsoTradeTransferAgent extends com.calypso.tk.upload.uploader.UploadCalypsoTradeTransferAgent {

	public static final String TRANSFER_PAYOUT_KW = "TransferPayOut";
	public static final String TRANSFER_PAYOUT_KW_BORROW = "Borrow";
	public static final String TRANSFER_PAYOUT_KW_LEND = "Lend";
	
	String uniqueKey = "";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	private SettleDeliveryInstruction getSDI(LegalEntity agent, TransferAgent transferAgent, CalypsoTrade calypsoTrade,
			String payReceive) {
		JDate settleDate = this.trade.getSettleDate();

		TradeTransferRule rule = new TradeTransferRule();
		rule.setPayReceive(payReceive);
		if (this.trade.getBook() != null) {
			rule.setPayerLegalEntityId(this.trade.getBook().getLegalEntity().getId());
		}
		rule.setPayerLegalEntityRole(LegalEntity.PROCESSINGORG);
		if (this.trade.getBook() != null) {
			rule.setReceiverLegalEntityId(this.trade.getBook().getLegalEntity().getId());
		}
		rule.setReceiverLegalEntityRole(LegalEntity.PROCESSINGORG);
		rule.setTransferCurrency(transferAgent.getCurrency());
		rule.setSettlementCurrency(transferAgent.getCurrency());
		rule.setProductType(transferAgent.getType());
		rule.setTransferType(transferAgent.getFlowType());

		if (transferAgent.getSecurity() != null) {
			rule.setSecurityId(transferAgent.getSecurity().getId());
		}

		SDISelector sdiSelector = SDISelectorUtil.find(this.trade, rule);
		if (sdiSelector == null) {
			return null;
		}

		LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), rule.getPayerLegalEntityId());

		Vector exceptions = new Vector();
		Vector sdiVector = null;

		try {
			if (le != null) {

				sdiVector = sdiSelector.getValidSDIList(this.trade, rule, settleDate, le.getCode(),
						rule.getPayerLegalEntityRole(), exceptions, true, DSConnection.getDefault());

			}
		} catch (Exception var13) {
			Log.error(this, var13);
		}
		if (!Util.isEmpty(sdiVector)) {
			Iterator sdiIterator = sdiVector.iterator();
			while (sdiIterator.hasNext()) {
				SettleDeliveryInstruction fromSDI = (SettleDeliveryInstruction) sdiIterator.next();

				if (agent == null || (fromSDI.getAgent() != null && fromSDI.getAgent().getPartyId() == agent.getId())) {
					return fromSDI;
				}
			}
		}

		return null;
	}
	
	public LegalEntity getAgent(boolean isToSdi) throws Exception {
		String transferPayOut = trade.getKeywordValue(TRANSFER_PAYOUT_KW);
		if(transferPayOut==null) {
			transferPayOut=TRANSFER_PAYOUT_KW_BORROW;
		}
		if((isToSdi && transferPayOut.equals(TRANSFER_PAYOUT_KW_BORROW)) || (!isToSdi && transferPayOut.equals(TRANSFER_PAYOUT_KW_LEND))) {
			String agent = trade.getKeywordValue(MurexTransferAgentMapper.TRANSFER_TRIPARTY_AGENT_KW);
			LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), agent);
			if(le==null)
				throw new Exception("Agent not found " + agent);
			return le;
		}
		return null;
	}

	public Product uploadTradeProduct(CalypsoObject object, Vector<BOException> errors, Connection dbConnection) {

		TransferAgent transferAgent = (TransferAgent) super.uploadTradeProduct(object, errors, dbConnection);
	
		// Automatic assignation of SDI
		cleanSDIAssignationExceptions(errors);

		if (this.calypsoTrade != null) {
			this.uniqueKey = UploaderKeyManager.getUniqueKey(this.calypsoTrade);
		}

		long tradeId = 0L;
		if (this.calypsoTrade.getTradeId() != null && this.calypsoTrade.getTradeId().toString().length() != 0
				&& this.calypsoTrade.getTradeId() != 0L) {
			tradeId = this.calypsoTrade.getTradeId();
		}

		if (transferAgent.getToSdiId() == 0) {
			try {
				LegalEntity agent = getAgent(true);
				SettleDeliveryInstruction toSDI = getSDI(agent, transferAgent, (CalypsoTrade) object, SettleDeliveryInstruction.S_RECEIVE);
				if (toSDI == null) {
					errors.add(ErrorExceptionUtils.createException("21001", "To Settle Delivery Instruction", "02033",
							agent.getCode(), tradeId, this.uniqueKey));
				} else {
					transferAgent.setToSdiId(toSDI.getId());
				}
			} catch (Exception e) {
				errors.add(ErrorExceptionUtils.createException("21001", "To Settle Delivery Instruction : "+e.getMessage(), "02033",
						null, tradeId, this.uniqueKey));
			}
		}
		
		if (transferAgent.getFromSdiId() == 0) {
			try {
				LegalEntity agent = getAgent(false);
				SettleDeliveryInstruction fromSDI = getSDI(agent, transferAgent, (CalypsoTrade) object, SettleDeliveryInstruction.S_RECEIVE);
				if (fromSDI == null) {
					errors.add(ErrorExceptionUtils.createException("21001", "From Settle Delivery Instruction", "02032",
							agent.getCode(), tradeId, this.uniqueKey));
				} else {
					transferAgent.setFromSdiId(fromSDI.getId());
				}
			} catch (Exception e) {
				errors.add(ErrorExceptionUtils.createException("21001", "From Settle Delivery Instruction :"+e.getMessage(), "02032",
						null, tradeId, this.uniqueKey));
			}
		}

		return transferAgent;
	}

	public void cleanSDIAssignationExceptions(Vector<BOException> exceptions) {
		ListIterator<BOException> it = exceptions.listIterator();
		while (it.hasNext()) {
			BOException excpt = it.next();
			if (excpt instanceof UploadBOException) {
				UploadBOException uploadException = (UploadBOException) excpt;
				if (uploadException.getTypeCode().equals("21001") && (uploadException.getMsgCode().equals("02033")
						|| uploadException.getMsgCode().equals("02032"))) {
					it.remove();
				}
			}
		}
	}

}

