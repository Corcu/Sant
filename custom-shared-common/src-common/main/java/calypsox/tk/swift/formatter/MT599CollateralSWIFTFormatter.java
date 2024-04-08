package calypsox.tk.swift.formatter;

import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.swift.formatter.MT599ChasingSWIFTFormatter;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.TradeArray;

import calypsox.tk.bo.swift.SantanderSwiftUtil;
import calypsox.util.binding.CustomBindVariablesUtil;

/**
 * SWIFTFormatter class for MT599Collateral template
 * @author x957355
 *
 */
public class MT599CollateralSWIFTFormatter extends MT599ChasingSWIFTFormatter {

	private static final String MSG_INITIATION = "Initiation";
	private static final String MSG_ADD = "Add";
	private static final String MSG_RETURN = "Return";

	 /**
     * Return the MessageAttribute TRN.
     *
     * @param message       a message.
     * @param trade         a trade
     * @param sender        sender
     * @param rec           receiver
     * @param transferRules transferRules
     * @param transfer      transfer
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return the message attribute TRN
     */
	@SuppressWarnings("rawtypes")
	public String parseSANT_MSG_TRN(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		return SantanderSwiftUtil.parseSANT_MSG_TRN(message, sender, rec, transfer, dsCon);
	}

	/**
	 * Return message type (Initiation, Add, Return)
     * @param message       a message.
     * @param trade         a trade
     * @param sender        sender
     * @param rec           receiver
     * @param transferRules transferRules
     * @param transfer      transfer
	 * @param dsCon ds connection
	 * @return message type (Initiation, Add, Return)
	 */
	public String parseMT599_TYPEE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final DSConnection dsCon) {

		MarginCall mc = (MarginCall) trade.getProduct();
		JDate date = trade.getSettleDate();
		date = date.addBusinessDays(-1, trade.getBook().getHolidays());
		if (mc.getBuySell(trade) == -1) {
			if(message.getSubAction() != null && message.getSubAction().equals(Action.CANCEL)) {
				return MSG_ADD;
			}
			return MSG_RETURN;
		} else {
			if(message.getSubAction() != null && message.getSubAction().equals(Action.CANCEL)) {
				return MSG_RETURN;
			}
			if (null != mc.getSecurity()) {
				InventorySecurityPosition secPos = getInventoryPosition(trade, mc, date,  dsCon);

				if (null != secPos ){

					TradeArray tarr = getTradesForInventory(secPos, trade, dsCon);
					if (secPos.getTotal() == 0) {
						if (tarr != null && tarr.size() > 1) {
							return MSG_ADD; // Is not the first MarginCall trade of the day
						} else {
							return MSG_INITIATION;
						}

					} else {
						return MSG_ADD;
					}
				} else {
					secPos = getInventoryPosition(trade, mc, trade.getSettleDate(), dsCon); //Settle date position
					if(secPos != null) {
						TradeArray tarr = getTradesForInventory(secPos, trade, dsCon);
						if(tarr != null && tarr.size() > 1) {
							return MSG_ADD;
						}
					}
					return MSG_INITIATION;
				}
			}

		}
		return null;

	}

	/**
	 * Return wich party will pay the fee
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return wich party will pay the fee
	 */
	public String parseFEE_LEVY(final BOMessage message, final Trade trade, final LEContact sender, final LEContact rec,
			final Vector transferRules, final BOTransfer transfer, final String format, final DSConnection dsCon) {

		MarginCall mc = (MarginCall) trade.getProduct();
		MarginCallConfig mcc = null;
		String result = "";
		if (mc != null) {
			mcc = mc.getMarginCallConfig();
		}
		if (mcc != null) {
			result = mcc.getAdditionalField("MT599_FEE_LEVY");

		}
		return result;

	}

	/**
	 * Return the Pledgee Name
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return pledgee name
	 */
	public String parsePLEDGEE_NAME(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {

		int sdiId;
		if(message.getSubAction() != null && message.getSubAction().equals(Action.CANCEL)) {
			
		}

		sdiId = transfer.getReceiverSDId();
		return getName(sdiId, dsCon);

	}

	/**
	 * Return the pledgee account
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return pledgee account
	 */
	public String parsePLEDGEE_ACCOUNT(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		
		int sdiId;
		if(message.getSubAction() != null && message.getSubAction().equals(Action.CANCEL)) {
			sdiId = transfer.getPayerSDId();
		} else {
			sdiId = transfer.getReceiverSDId();
		}

		
		return getAccount(sdiId, dsCon);
	}

	/**
	 * Return the pledgor name
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return pledgor name
	 */
	public String parsePLEDGOR_NAME(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		int sdiId;

		sdiId = transfer.getPayerSDId();
		return getName(sdiId, dsCon);

	}

	/**
	 * Return the pledgor account
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return pledgor account
	 */
	public String parsePLEDGOR_ACCOUNT(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		int sdiId;
		if(message.getSubAction() != null && message.getSubAction().equals(Action.CANCEL)) {
			sdiId = transfer.getReceiverSDId();
		} else {
			sdiId = transfer.getPayerSDId();
		}
		return getAccount(sdiId, dsCon);
	}

	/**
	 * Return the formatted field of the matturity date of the product
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return
	 */
	public String parsePRODUCT_MATURITYDATE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {

		String date = super.parsePRODUCT_MATURITYDATE(message, trade, sender, rec, transferRules, transfer, dsCon);
		if (date.isEmpty()) {
			return "20991231";
		} else {
			return date;
		}

	}

	/**
	 * Return the formatted field of the Product ISIN Code of the security
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return ISIN Code of the security
	 */
	public String parseISIN_CODE(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		MarginCall mc = (MarginCall) trade.getProduct();

		if (mc != null && mc.getSecurity() != null) {
			return mc.getSecurity().getSecCode("ISIN");
		}
		return null;

	}

	/**
	 * Return the RETURN_BANKRUPTCY field
	 * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
	 * @return Collateral return to pledgor in case of pledgee bankruptcy (Yes/No)

	 */
	public String parseRETURN_BANKRUPTCY(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		String result = "";
		MarginCallConfig mcc = null;
		MarginCall mc = (MarginCall) trade.getProduct();
		if (mc != null) {
			mcc = mc.getMarginCallConfig();
		}
		if (mcc != null) {
			result = mcc.getAdditionalField("MT599_RETURN_IN_BANKRUPTCY");
		}
		return result;
	}
	
	
	/**
	 * Return the formatted MT599 Add and Return Counterparty field
	 * @param message
	 * @param trade
	 * @param sender
	 * @param rec
	 * @param transferRules
	 * @param transfer
	 * @param format
	 * @param dsCon
	 * @return Counterparty field
	 */
	public String parseCOUNTERPARTY(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {
		
		MarginCall mc = (MarginCall) trade.getProduct();
		
			if(mc.getBuySell(trade) == -1) {
				return parsePLEDGEE_NAME(message, trade, sender, rec, transferRules, transfer, format, dsCon);
			}
			return parsePLEDGOR_NAME(message, trade, sender, rec, transferRules, transfer, format, dsCon);
		
	}

	/**
	 * Return the Settlement Delivery Instruction
	 * @param sdiId Identifier of the SDI
	 * @param dsCon ds connection
	 * @return SettlementDeliveryInstruction object
	 */
	public static SettleDeliveryInstruction getSDI(int sdiId, DSConnection dsCon) {

		SettleDeliveryInstruction sdi = null;
		try {
			sdi = dsCon.getRemoteReferenceData().getSettleDeliveryInstruction(sdiId);
		} catch (CalypsoServiceException e) {

			Log.error("Error geting SDI", e);
		}
		return sdi;

	}

	/**
	 * Return the inventory position of the MarginCall trade the day before, if exixts
	 * @param trade MarginCall trade
	 * @param mc MarginCall object
	 * @param dsCon ds connection
	 * @return the inventory position of the MarginCall trade the day before, if exixts
	 */
	private InventorySecurityPosition getInventoryPosition(Trade trade, MarginCall mc, JDate date, DSConnection dsCon) {

		
		StringBuilder sbWhere = new StringBuilder();
		InventorySecurityPosition secPos = null;

		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil
				.createNewBindVariable(mc.getSecurity().getId());

		sbWhere.append("POSITION_TYPE = 'THEORETICAL' AND");
		sbWhere.append(" DATE_TYPE = 'SETTLE' AND INTERNAL_EXTERNAL = 'MARGIN_CALL' AND CONFIG_ID = 0 ");
		sbWhere.append(" AND SECURITY_ID = ? AND BOOK_ID = ? ");
		sbWhere.append("AND MCC_ID = ? AND POSITION_DATE <= ? ");
		sbWhere.append(" ORDER BY POSITION_DATE ASC ");

		CustomBindVariablesUtil.addNewBindVariableToList(trade.getBookId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(mc.getMarginCallId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(date, bindVariables);

		try {
			InventorySecurityPositionArray invSecPosVec = dsCon.getRemoteBackOffice().getInventorySecurityPositions("", sbWhere.toString(), bindVariables);


			if ((null != invSecPosVec) && !invSecPosVec.isEmpty()) {
				 secPos = invSecPosVec.get(invSecPosVec.size() - 1);
				
				}
			return secPos;
		} catch (CalypsoServiceException e) {

			Log.error("Error getting inventory position", e);
			return null;
		}

	}

	/**
	 * Return a Trade array with all the trades created in the inventory int the trade settle date
	 * @param inventory InventorySecurityPosition
	 * @param trade trade object
	 * @param dsCon ds connection
	 * @return trade array
	 */
	private TradeArray getTradesForInventory(InventorySecurityPosition inventory, Trade trade, DSConnection dsCon) {
		StringBuilder sbWhere = new StringBuilder();

		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(Product.MARGINCALL);
		sbWhere.append("TRADE.PRODUCT_ID=PRODUCT_DESC.PRODUCT_ID AND ");
		sbWhere.append("PRODUCT_DESC.PRODUCT_TYPE=? AND ");
		sbWhere.append(" TRADE.CPTY_ID = ? AND ");
		sbWhere.append("TRADE.BOOK_ID=? AND PRODUCT_DESC.UND_SECURITY_ID=?  ");
		sbWhere.append("AND TRADE.SETTLEMENT_DATE = ? AND TRADE.TRADE_STATUS != ? ");
		sbWhere.append("AND TRADE.PRODUCT_ID=PRODUCT_SIMPLEXFER.PRODUCT_ID  AND PRODUCT_SIMPLEXFER.LINKED_ID = ?");
		
		CustomBindVariablesUtil.addNewBindVariableToList(trade.getCounterParty().getEntityId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getBookId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getProduct().getId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(trade.getSettleDate(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(Status.CANCELED, bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getMarginCallConfigId(), bindVariables);

		try {
			return dsCon.getRemoteTrade().getTrades("TRADE, PRODUCT_DESC, PRODUCT_SIMPLEXFER", sbWhere.toString(), null,
					bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error getting pledged trades.", e);
			return TradeArray.EMPTY_TRADE_ARRAY;
		}
	}

	/**
	 * Return the Beneficiary Name of the SDI
	 * @param sdiId DSI identifier
	 * @param dsCon ds connection
	 * @return Beneficiary Name of the SDI
	 */
	private String getName(int sdiId, DSConnection dsCon) {
		SettleDeliveryInstruction sdi = null;
		String name = "";
		if (String.valueOf(sdiId) != null && !String.valueOf(sdiId).equals("") && !String.valueOf(sdiId).equals("0")) {

			sdi = getSDI(sdiId, dsCon);
			if (sdi != null) {
				name = sdi.getBeneficiaryName();
			}
		}
		return name;
	}
	
	/**
	 * Return the Agent account of the SDI
	 * @param sdiId DSI identifier
	 * @param dsCon ds connection
	 * @return Agent account of the SDI
	 */
	private String getAccount(int sdiId, DSConnection dsCon) {
		SettleDeliveryInstruction sdi = null;
		String accName = "";
		if (String.valueOf(sdiId) != null && !String.valueOf(sdiId).equals("") && !String.valueOf(sdiId).equals("0")) {

			sdi = getSDI(sdiId, dsCon);

			if (sdi != null) {
				accName = sdi.getAgentAccount();
			}
		}

		return accName;
	}
}
