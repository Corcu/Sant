/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory;

import static calypsox.engine.inventory.SantPositionConstants.BLOQUEO;
import static calypsox.engine.inventory.SantPositionConstants.THEORETICAL;

import java.util.Calendar;
import java.util.TimeZone;

import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.util.gdisponible.GDisponibleUtil;

/**
 * This class Builds the Trade to impact positions. These trades are of type
 * SimpleTransfer
 * 
 * @author Patrice Guerrido & Guillermo Solano
 * @version 1.1
 */
public class BOPositionAdjustmentTradeBuilder {

	// Constants
	private final static String REPO_KWD = "RepoTracking";
	private final static String SUSTITUBLE_KWD = "Substitutable";
	private final static String GD_KWD = "GestionDisponible";
	private final static String FALSE = "false";
	private final static String TRUE = "true";
	private final static String NONE = "NONE";
	private final static String PO = "ProcessingOrg";
	private final static String BUY_SELL = "Buy/Sell";
	private final static String SECURITY = "SECURITY";
	private final static String ACTUAL_POS_KWD = "ACTUAL_POS";
	private static final String BO_SYSTEM = "BO_SYSTEM";

	/** Message reference use for internally build message **/
	public String messageReference = null;

	public void setMessageReference(String messageReference) {
		this.messageReference = messageReference;
	}

	public String getMessageReference() {
		return this.messageReference;
	}

	/**
	 * Builds the specific trade from the positions Bean to impact the FI
	 * position
	 * 
	 * @param updatePosition
	 *            bean containing the date
	 * @return trade to be updated
	 */
	public Trade build(SantPositionBean updatePosition) {

		Trade trade = new Trade();
		// BOPositionAdjustment DOES NOT ALLOW Pledge
		SimpleTransfer product = new SimpleTransfer();
		trade.setProduct(product);
		// Trade data
		trade.setAccountNumber(updatePosition.getAccount().getName());
		trade.setInventoryAgent(updatePosition.getAgent().getCode());
		trade.addKeyword(REPO_KWD, FALSE);
		trade.addKeyword(SUSTITUBLE_KWD, FALSE);
		trade.addKeyword(BO_SYSTEM, GD_KWD);

		if (GDisponibleUtil.SANT_GD_MATURE_SEC_POS_REFERENCE.equals(getMessageReference())) {
			trade.addKeyword(GDisponibleUtil.KWD_IMPORT_SOURCE, GDisponibleUtil.SANT_GD_MATURE_SEC_POS_REFERENCE);
		}

		trade.setTraderName(NONE);
		trade.setSalesPerson(NONE);
		trade.setQuantity(updatePosition.getQuantity());
		trade.setBook(updatePosition.getBook());
		//AAP MIG 14.4
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(new JDatetime(updatePosition.getPositionDate(), TimeZone.getDefault()).getTime());
		cal.set(Calendar.AM_PM, Calendar.AM);
		cal.set(Calendar.HOUR, 2);
		trade.setTradeDate(new JDatetime(cal.getTime()));
		trade.setSettleDate(updatePosition.getPositionDate());
		trade.setAction(Action.NEW);
		trade.setCounterParty(updatePosition.getBook().getLegalEntity());
		trade.setRole(PO);
		trade.setAdjustmentType(BUY_SELL);
		trade.setTradeCurrency(updatePosition.getCurrency());
		trade.setSettleCurrency(updatePosition.getCurrency());
		trade.setEnteredUser(DSConnection.getDefault().getUser());
		// SimpleTransfer Product Information
		product.setSecurity(updatePosition.getSecurity());
		product.setFlowType(SECURITY);
		product.setOrdererRole(PO);
		product.setOrdererLeId(updatePosition.getBook().getLegalEntity().getId());
		product.setPrincipal(updatePosition.getSecurity().getPrincipal());

		final String posType = updatePosition.getPositionType();
		if (!posType.equals(BLOQUEO)) {

			trade.addKeyword(ACTUAL_POS_KWD, posType.equalsIgnoreCase(THEORETICAL) ? FALSE : TRUE);
		} else {// BLOQUEO

			trade.addKeyword(ACTUAL_POS_KWD, TRUE);

			if (trade.getQuantity() > 0) {
				product.setIsReturnB(true);
			}
			product.setIsPledgeMovementB(true);
		}

		return trade;
	}
}
