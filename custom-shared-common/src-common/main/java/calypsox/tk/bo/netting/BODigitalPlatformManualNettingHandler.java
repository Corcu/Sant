package calypsox.tk.bo.netting;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.apps.navigator.PSEventHandler;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ComparatorNettedTransfer;
import com.calypso.tk.bo.NettingConfig;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.SerializationException;
import com.calypso.tk.core.SortShell;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSException;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

public class BODigitalPlatformManualNettingHandler {

	protected TransferArray getNettedTransfers(long transferLongId) {
		TransferArray myTransfers = null;
		try {
			myTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(transferLongId);
		} catch (Exception e) {
			Log.error(this, e);
		}
		if (myTransfers == null)
			return new TransferArray();
		TransferArray validTransfers = new TransferArray();
		for (int i = 0; i < myTransfers.size(); i++) {
			BOTransfer transfer = myTransfers.elementAt(i);
			if (!Status.isCanceled(transfer.getStatus()))

				validTransfers.add(transfer);
		}
		return validTransfers;
	}

	@SuppressWarnings("unchecked")
	private boolean isNettable(TransferArray origTransfers, String nettingType, ArrayList<String> errors) {
		StringBuffer sb = new StringBuffer();
		if (nettingType.equals("None"))
			return true;
		HashMap<String, String> keys = BOCache.getNettingConfig(DSConnection.getDefault(), nettingType);

		TransferArray transfers = new TransferArray();
		for (int i = 0; i < origTransfers.size(); i++) {
			BOTransfer t = origTransfers.get(i);
			if (t.getNettedTransfer()) {

				transfers.add(getNettedTransfers(t.getLongId()).toVector());
			} else {
				transfers.add(t);
			}
		}
		if (!validateCrossSecurityNettingZeroQty(origTransfers, transfers))
			return false;

		transfers = new TransferArray(SortShell.sort(transfers.toVector(), new ComparatorNettedTransfer()));

		BOTransfer bot = transfers.get(0).initialize(keys);
		bot.setNettingType(nettingType);
		for (int j = 0; j < transfers.size(); j++) {
			BOTransfer bot2 = transfers.get(j);
			try {
				bot2 = (BOTransfer) bot2.clone();
				bot2.setNettingType(nettingType);
			} catch (Exception e) {
				Log.error(this, e);
			}
			boolean isNettable = bot.checkKey(bot2, keys);
			boolean isSimpleNetting = !Util.isEmpty(bot2.getAttribute("SimpleNetting"));
			if (!isNettable || isSimpleNetting) {
				sb.append("Following transfer(s) have not been netted ");
				if (!isNettable) {
					sb.append("due to inconsistency in netting keys:\n");
					sb.append(getTransferDetails(bot) + "\n");
				} else {
					sb.append("because they are eligible for SimpleNetting:\n");
				}
				sb.append(getTransferDetails(bot2) + "\n");
				sb.append("The entire group is ignored.\n");
				if (!isNettable) {
					sb.append("Please check your subheading columns contains at least the netting keys:\n - ");
					@SuppressWarnings("rawtypes")
					Vector<?> v = new Vector(keys.values());
					sb.append(Util.collectionToString(v, "\n - "));
				}
				Log.error(this, sb.toString());
				errors.add(sb.toString());
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean netTransfers(TransferArray transfers, TransferArray allTransfers, String nettingType,
			boolean isExcludeSameSide, String comment, ArrayList<String> errors) {

		if (transfers.size() == 0) {
			return false;
		}
		if (transfers.size() == 1) {
			BOTransfer bot = transfers.get(0);
			Log.warn(this, "Transfer " + bot.getLongId()
					+ " is the only transfer of its group. Netted transfer will be not created. ");
			errors.add("Transfer " + bot.getLongId()
					+ " is the only transfer of its group. Netted transfer will be not created. ");
			return false;
		}
		transfers = checkPermission(transfers, Action.ASSIGN);
		if (transfers == null || transfers.size() == 0)

			return false;
		Vector<String> nonSplitTransfers = null;

		boolean isNone = false;
		boolean isRollOver = false;
		if (nettingType.equals("RollOver"))
			isRollOver = true;

		if (nettingType.equals("None")) {
			isNone = true;
			boolean containsNonNetted = false;
			for (int k = 0; k < transfers.size(); k++) {
				BOTransfer tr = transfers.get(k);
				if (!tr.getNettedTransfer())
					containsNonNetted = true;
			}
			if (containsNonNetted) {
				Log.warn(this, "Netting type is None, one of the selected transfers is non netted. ");
				errors.add("Netting type is None, one of the selected transfers is non netted. ");
			}
		}
		if (!isNettable(transfers, nettingType, errors)) {
			return false;
		}
		if (isExcludeSameSide) {
			String msg = checkSameSide(transfers);
			if (!Util.isEmpty(msg)) {
				Log.error(this, msg);
				errors.add(msg);
				return false;
			}
		}

		Vector<BOTransfer> myTransfers = transfers.toVector();
		myTransfers = SortShell.sort(myTransfers, new ComparatorNettedTransfer());
		transfers = new TransferArray(myTransfers);
		TransferArray clonedTransfers = null;
		try {
			clonedTransfers = (TransferArray) transfers.clone();
		} catch (CloneNotSupportedException ex) {
			Log.error(this, ex);
			clonedTransfers = new TransferArray();
		}
		String ss = "";
		if (!isNone) {
			for (int k = 0; k < clonedTransfers.size(); k++) {
				BOTransfer tr = clonedTransfers.get(k);
				if (!tr.getSettleDate().equals(tr.getValueDate()))
					if (Util.isEmpty(ss)) {
						ss = String.valueOf(tr.getLongId());
					} else {
						ss = ss + "," + String.valueOf(tr.getLongId());
					}
			}
			if (!Util.isEmpty(ss)) {
				Log.warn(this, "Value Date different than Settle Date on Transfers: " + ss
						+ ".\nDo you want still to perform netting ?");
			}
		}
		long groupId = 0L;
		String oldNettingType = nettingType;
		if (!isNone) {
			groupId = getGroupId(clonedTransfers, nettingType);
			if (groupId > 0L)
				nettingType = nettingType + "/" + groupId;

		}

		long forceTransferLongId = 0L;
		if (!isNone && !isRollOver) {
			try {
				BOTransfer bot = clonedTransfers.get(0);
				String botNettingType = bot.getNettingType();
				bot.setNettingType(oldNettingType);
				bot.setPairOffFrom(comment);
				HashMap<String, String> keys = BOCache.getNettingConfig(DSConnection.getDefault(), nettingType);
				BOTransfer target = bot.initialize(keys);

				bot.setNettingType(botNettingType);
				TransferArray eligibleTransfers = getEligibleTransfers(target, keys);
				TransferArray eligibleFounds = new TransferArray();
				if (eligibleTransfers != null && eligibleTransfers.size() > 0)
					for (int k = 0; k < eligibleTransfers.size(); k++) {
						BOTransfer eli = eligibleTransfers.get(k);

						if (clonedTransfers.indexOf(eli) < 0)

							if (!Status.isCanceled(eli.getStatus())) {

								String eliNettingType = eli.getNettingType();
								int index = eliNettingType.lastIndexOf('/');
								if (index > 0)
									eliNettingType = eliNettingType.substring(0, index);

								String newNettingType = oldNettingType;
								index = newNettingType.lastIndexOf('/');
								if (index > 0)
									newNettingType = newNettingType.substring(0, index);
								if (eliNettingType.equals(newNettingType)) {

									String initNettingType = eli.getNettingType();
									eli.setNettingType(oldNettingType);
									if (eli.checkKey(target, keys)) {

										eli.setNettingType(initNettingType);
										Trade trade = null;
										if (eli.getTradeLongId() != 0L)
											trade = DSConnection.getDefault().getRemoteTrade()
													.getTrade(eli.getTradeLongId());

										if (isActionPossible(eli, trade, Action.AMEND, bot)) {

											eligibleFounds.add(eli);
											break;
										}
									}
								}
							}
					}
				if (eligibleFounds != null && eligibleFounds.size() > 0) {
					BOTransfer eli = eligibleFounds.get(0);
					Log.warn(this, "The following Netted Transfer " + eli.getLongId() + "with Netting Type "
							+ eli.getNettingType() + "\nmay be updated for this group of Transfers.");

					groupId = getGroupId(eligibleTransfers, nettingType);
					if (groupId > 0L)
						nettingType = oldNettingType + "/" + groupId;

				}
			} catch (Exception e) {
				Log.error(this, "Error checking Existing Transfers", e);
				errors.add("Error checking Existing Transfers");
				return false;
			}
		} else if (isRollOver && groupId == 0L) {

			BOTransfer bot = clonedTransfers.get(0);
			groupId = bot.getLongId();
			nettingType = nettingType + "/" + groupId;

		}
		boolean securityCashMix = false;
		boolean securityFound = false;
		boolean cashFound = false;
		for (int i = 0; i < clonedTransfers.size(); i++) {
			BOTransfer clonedBot = clonedTransfers.get(i);
			if (clonedBot.getTransferType().equals("SECURITY")) {
				securityFound = true;
			} else {
				cashFound = true;
			}
		}
		if (cashFound && securityFound)
			securityCashMix = true;

		Vector<Long> errorTransfers = new Vector();
		int j;
		for (j = 0; j < clonedTransfers.size(); j++) {
			BOTransfer clonedBot = clonedTransfers.get(j);
			BOTransfer transfer = null;
			Trade trade = null;
			try {
				transfer = (BOTransfer) clonedBot.clone();
			} catch (Exception e) {
				Log.error(this, e);
			}
			if (transfer.getTradeLongId() != 0L)
				try {
					trade = DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());
				} catch (Exception e) {
					Log.error(this, e);

				}
			if (transfer.getNettedTransfer()) {
				BOTransfer origBot = transfers.get(j);
				String s1 = "";
				String s2 = "";
				HashMap<String, String> keys1 = BOCache.getNettingConfig(DSConnection.getDefault(),
						origBot.getNettingType());
				if (keys1 != null)
					s1 = origBot.buildKey(keys1);
				HashMap<String, String> keys2 = BOCache.getNettingConfig(DSConnection.getDefault(), nettingType);
				if (keys2 != null) {
					String oriNettingType = transfer.getNettingType();
					transfer.setNettingType(nettingType);
					s2 = transfer.buildKey(keys2);

					transfer.setNettingType(oriNettingType);
				}
				if (!origBot.getNettingType().equals(transfer.getNettingType()) || !s1.equals(s2)
						|| (securityCashMix && !origBot.getTransferType().equals("SECURITY"))) {
					transfer.setAction(Action.CANCEL);

				} else if (transfer.getNettingType().equals(nettingType)) {

					transfer.setAction(Action.CANCEL);

				}
			}
			if (!BOTransferWorkflow.isTransferActionApplicable(transfer, trade, transfer.getAction(),
					DSConnection.getDefault(), null))
				errorTransfers.addElement(Long.valueOf(transfer.getLongId()));

		}
		if (errorTransfers.size() > 0) {
			Log.error(this, "The following Transfers cannot be assigned:\n" + errorTransfers);
			errors.add("The following Transfers cannot be assigned:\n" + errorTransfers);
			return false;
		}
		for (j = 0; j < clonedTransfers.size(); j++) {
			BOTransfer clonedBot = clonedTransfers.get(j);
			if (errorTransfers.indexOf(Long.valueOf(clonedBot.getLongId())) < 0) {

				BOTransfer origBot = transfers.get(j);
				clonedBot.setParentLongId(clonedBot.getLongId());
				clonedBot.setEnteredUser(DSConnection.getDefault().getUser());
				clonedBot.setLongId(0L);
				clonedBot.setNettingType(nettingType);
				clonedBot.setStatus(Status.S_NONE);
				clonedBot.setAction(Action.NEW);
				origBot.setAction(Action.ASSIGN);
				origBot.setEnteredUser(DSConnection.getDefault().getUser());
				if (j == clonedTransfers.size() - 1 && Defaults.getBooleanProperty("PAIR_OFF_AUTO_PROCESS", false)) {
					clonedBot.setAttribute("PairOffAutoProcess", "PairOffAutoProcess");
				} else {
					clonedBot.setAttribute("PairOffAutoProcess", null);
				}
				if (securityCashMix) {
					if (!clonedBot.getTransferType().equals("SECURITY"))
						clonedBot.setAttribute("SecurityCashMix", "true");

				} else if (clonedBot.getNettedTransfer()
						&& clonedBot.getNettingType().equals(origBot.getNettingType())) {

					clonedBot.setAttribute("SecurityCashMix", "true");
					if (forceTransferLongId != 0L)
						clonedBot.setAttribute("ForceNettingId", String.valueOf(forceTransferLongId));

				}
				TransferArray transferArray = new TransferArray();
				transferArray.add(clonedBot);
				clonedBot.setPairOffFrom(comment);
				clonedBot.setPairOffTo(null);
				try {
					if (transferArray != null && transferArray.size() > 0)
						origBot.setPairOffTo(comment);

					Vector events = DSConnection.getDefault().getRemoteBO().splitTransfers(origBot, transferArray);
					if (PSEventHandler.getPSConnection() != null)
						PSEventHandler.getPSConnection().publish(events);

					allTransfers.remove(origBot);
				} catch (PSException e) {
					Log.error(this, (Throwable) e);
				} catch (SerializationException e) {
					Log.error(this, (Throwable) e);

				} catch (RemoteException e) {
					Log.error(this, e);
					if (nonSplitTransfers == null)
						nonSplitTransfers = new Vector();

					String transferDetails = getTransferDetails(origBot);
					nonSplitTransfers.add(transferDetails);
				}
			}
		}
		if (nonSplitTransfers != null) {
			Log.error(this,
					"Following transfers have not been netted due to errors :\n" + nonSplitTransfers.toString());
			return false;
		}
		return true;
	}

	private boolean validateCrossSecurityNettingZeroQty(TransferArray selectedTransfers, TransferArray underlyings) {
		boolean isValid = true;
		if (underlyings != null) {
			Map<Integer, Double> securityQtyMap = new HashMap<>();
			for (BOTransfer transfer : underlyings) {
				if (!"SECURITY".equals(transfer.getTransferType()))

					continue;
				int key = transfer.getProductId();
				Double qty = securityQtyMap.get(Integer.valueOf(key));
				if (qty == null)
					qty = Double.valueOf(0.0D);

				int xferSign = "RECEIVE".equals(transfer.getPayReceive()) ? 1 : -1;
				qty = Double.valueOf(qty.doubleValue() + Math.abs(transfer.getSettlementAmount()) * xferSign);
				securityQtyMap.put(Integer.valueOf(key), qty);
			}
			if (securityQtyMap.size() > 1) {
				isValid = false;
				boolean isCrossSecurityNettingPossible = true;
				for (Double secQty : securityQtyMap.values()) {
					if (secQty.doubleValue() != 0.0D) {
						isCrossSecurityNettingPossible = false;
						break;
					}
				}
				if (isCrossSecurityNettingPossible) {
					Log.warn(this, "Cross security netting is not allowed (security amount is not zero).");
				}
			}
		}
		return isValid;
	}

	private String checkSameSide(TransferArray transfers) {
		for (int i = 0; i < transfers.size(); i++) {
			BOTransfer transfer1 = transfers.get(i);
			for (int j = i + 1; j < transfers.size(); j++) {

				BOTransfer transfer2 = transfers.get(j);
				if (!isSameSide(transfer1, transfer2))
					return null;
			}
		}
		String msg = "Selected transfers are same side. They cannot be paired off.";
		return msg;
	}

	public boolean isSameSide(BOTransfer transfer1, BOTransfer transfer2) {
		if (transfer1.getTransferType().equals(transfer2.getTransferType())
				&& !transfer1.getPayReceive().equals(transfer2.getPayReceive()))
			return false;

		if (!transfer1.getTransferType().equals("SECURITY") && !transfer2.getTransferType().equals("SECURITY")
				&& !transfer1.getPayReceive().equals(transfer2.getPayReceive()))
			return false;

		if (!transfer1.getTransferType().equals(transfer2.getTransferType())
				&& (transfer1.getTransferType().equals("SECURITY") || transfer2.getTransferType().equals("SECURITY"))
				&& transfer1.getPayReceive().equals(transfer2.getPayReceive()))
			return false;
		return true;
	}

	private String getTransferDetails(BOTransfer transfer) {
		StringBuffer transferDetails = new StringBuffer("Transfer #");
		transferDetails.append(transfer.getLongId() + ": ");
		transferDetails.append(transfer.getEventType() + " ");
		transferDetails.append(transfer.getSettlementCurrency() + " ");
		transferDetails.append(transfer.getSettlementAmount() + " ");
		LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getInternalAgentId());
		if (le != null)
			transferDetails.append(le.getCode());

		return transferDetails.toString();
	}

	public Long applyActionAssignCommand(TransferArray allTransfers, String nettingType, ArrayList<String> errors) {
		boolean isOk = false;
		TransferArray transfers = new TransferArray();
		for (BOTransfer boTransfer : allTransfers) {
			
			transfers.add(boTransfer);
		}
		if (netTransfers(transfers, allTransfers, nettingType, false, null, errors)) {
			isOk = true;
		}
		
		if (isOk) {
			try {
				BOTransfer xfer = transfers.get(0);
				TransferArray ta = DSConnection.getDefault().getRemoteBO().getBOTransfers(xfer.getTradeLongId(),false);
				for (BOTransfer boTransfer : ta) {
					if(boTransfer.getParentLongId() == xfer.getLongId()) {
						return boTransfer.getNettedTransferLongId();
					}
				}
				
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error getting xfer.");
			}
		}
		return 0l;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected TransferArray checkPermission(TransferArray transfers, Action action) {
		Vector<String> messages = new Vector();
		for (int i = 0; i < transfers.size(); i++) {
			BOTransfer transfer = transfers.get(i);
			Vector vv = BOTransferWorkflow.getBOTransferActions(transfer, DSConnection.getDefault());
			if (vv == null || vv.indexOf(action.toString()) == -1) {
				messages.addElement("Action " + action + " not available for Transfer" + transfer.getLongId());

			} else if (!AccessUtil.isAuthorized("Transfer", transfer.getProductType(), transfer.getStatus().toString(),
					action.toString())) {
				messages.addElement("Transfer Access Permission denied to apply " + action + " to Status "
						+ transfer.getStatus() + " for Transfer " + transfer.getLongId());

			} else {
				transfer.setAction(action);
			}
		}
		if (!Util.isEmpty(messages)) {
			Log.error(this, messages.toString());
		}
		return transfers;
	}

	boolean checkAccessPermission(BOTransfer transfer, String statusType, String productType, Status status,
			Action action) {
		Book book = BOCache.getBook(DSConnection.getDefault(), transfer.getBookId());
		if (book != null && !AccessUtil.checkPermission(book, false))
			return false;
		return AccessUtil.isAuthorized(statusType, productType, status.toString(), action.toString(), "ALL");
	}

	boolean checkAccessPermission(String ptype, Status status, Action action, String type) {
		return AccessUtil.isAuthorized(type, ptype, status.toString(), action.toString());
	}

	private long getGroupId(TransferArray transfers, String nettingType) {
		long lastGroup = 0L;
		boolean isSameGroupFound = false;

		int index = nettingType.lastIndexOf('/');
		if (index > 0)
			nettingType = nettingType.substring(0, index);

		for (int i = 0; i < transfers.size(); i++) {
			BOTransfer bot = transfers.get(i);
			String initNettingType = bot.getNettingType();
			String boNet = null;

			index = initNettingType.lastIndexOf('/');
			if (index > 0) {
				boNet = initNettingType.substring(0, index);
			} else {
				boNet = initNettingType;
			}
			if (nettingType.equals(boNet)) {
				isSameGroupFound = true;
				long botGroup = bot.getNettingGroup();
				if (botGroup > lastGroup)
					lastGroup = botGroup;

			}
		}
		if (lastGroup > 0L || (lastGroup == 0L && isSameGroupFound))

			lastGroup++;

		return lastGroup;
	}

	protected boolean isActionPossible(BOTransfer transfer, Trade trade, Action action, BOTransfer underTransfer) {
		if (!underTransfer.getNettedTransfer())
			transfer.setUnderlyingTransfer(underTransfer);
		boolean applicable = BOTransferWorkflow.isTransferActionApplicable(transfer, trade, action,
				DSConnection.getDefault(), null);
		transfer.setUnderlyingTransfer(null);
		return applicable;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected TransferArray getEligibleTransfers(BOTransfer transfer, HashMap keys) {
		List<CalypsoBindVariable> bindVariables = new ArrayList<>();
		String w = "netted_transfer = ? ";
		bindVariables.add(new CalypsoBindVariable(16, Boolean.valueOf(true)));

		if (keys.get("TradeId") != null) {
			w = w + " AND (trade_id = ? )";
			bindVariables.add(new CalypsoBindVariable(3000, Long.valueOf(transfer.getTradeLongId())));
		}
		if (keys.get("ProductFamily") != null) {
			w = w + " AND (product_family =  ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getProductFamily()));
		}
		if (keys.get("ProductId") != null || transfer.getTransferType().equals("SECURITY")) {
			w = w + " AND (product_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getProductId())));
		}
		if (keys.get("ProductType") != null) {
			w = w + " AND (product_type = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getProductType()));
		}
		if (transfer.getTransferType().equals("SECURITY")) {
			w = w + " AND (transfer_type = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getTransferType()));

		} else if (keys.get("TransferType") != null) {
			w = w + " AND (transfer_type = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getTransferType()));

		}
		if (keys.get("SettlementCurrency") != null || !transfer.getTransferType().equals("SECURITY")) {
			w = w + " AND (amount_ccy = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getSettlementCurrency()));
		}
		if (keys.get("TradeCurrency") != null) {
			w = w + " AND (trade_ccy = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getTradeCurrency()));
		}
		if (keys.get("PayReceive") != null) {
			w = w + " AND (payreceive_type =  ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getPayReceive()));
		}
		if (keys.get("ValueDate") != null) {
			w = w + " AND (value_date = ? )";
			bindVariables.add(new CalypsoBindVariable(3001, transfer.getValueDate()));
		}
		if (keys.get("RealSettleDate") != null) {
			w = w + " AND (settle_date = ? )";
			bindVariables.add(new CalypsoBindVariable(3001, transfer.getSettleDate()));
		}
		int exteLeId = 0;
		if (keys.get("ParentNetting") != null) {
			LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getExternalLegalEntityId());
			if (le != null && le.getParentId() != 0) {
				exteLeId = le.getParentId();
			} else {
				exteLeId = transfer.getExternalLegalEntityId();
			}
		} else {
			exteLeId = transfer.getExternalLegalEntityId();
		}
		if (keys.get("ExternalLegalEntity") != null) {
			w = w + " AND (ext_le_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(exteLeId)));
		} else if (keys.get("MultiCounterparty") == null) {
			w = w + " AND (ext_le_id =  ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getInternalAgentId())));
		}
		if (keys.get("ExternalRole") != null) {
			w = w + " AND (ext_le_role = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getExternalRole()));
		} else {
			w = w + " AND (ext_le_role = 'Agent')";
		}
		if (keys.get("InternalLegalEntity") != null) {
			int inLeId = transfer.getInternalLegalEntityId();
			w = w + " AND (int_le_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(inLeId)));
		}
		if (keys.get("InternalRole") != null) {
			w = w + " AND (int_le_role = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getInternalRole()));
		}
		if (keys.get("GLAccount") != null) {
			w = w + "AND (gl_account_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getGLAccountNumber())));
		}
		if (keys.get("Bundle Id") != null) {
			w = w + " AND (bundle_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getBundleId())));
		}
		if (keys.get("TradeDate") != null) {
			w = w + " AND (trade_date = ? )";
			bindVariables.add(new CalypsoBindVariable(3001, transfer.getTradeDate()));
		}
		if (keys.get("InternalAgent") != null) {
			int inLeId = transfer.getInternalAgentId();
			w = w + " AND (int_agent_le_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(inLeId)));
		}
		if (keys.get("InternalCashAgent") != null) {
			int inLeId = transfer.getInternalCashAgentId();
			w = w + " AND (int_cash_agent_le_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(inLeId)));
		}
		if (keys.get("InternalSDI") != null) {
			int inLeId = transfer.getInternalSettleDeliveryId();
			w = w + " AND (int_sdi = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(inLeId)));
		}
		if (keys.get("ExternalAgent") != null) {
			int inLeId = transfer.getExternalAgentId();
			w = w + " AND (ext_agent_le_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(inLeId)));
		}
		if (keys.get("ExternalSDI") != null) {
			int inLeId = transfer.getExternalSettleDeliveryId();
			w = w + " AND (ext_sdi = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(inLeId)));
		}
		if (keys.get("DeliveryType") != null) {
			w = w + " AND (delivery_type = ? )";
			bindVariables.add(new CalypsoBindVariable(12, transfer.getDeliveryType()));
		}
		if (keys.get("Book") != null) {
			w = w + " AND (book_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getBookId())));
		}
		if (keys.get("OtherGLAccount") != null) {
			w = w + " AND (cash_account_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getCashAccountNumber())));
		}
		if (keys.get("Trade Cpty") != null) {
			w = w + " AND (orig_cpty_id = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getOriginalCptyId())));
		}
		if (keys.get("Manual SDI") != null) {
			w = w + " AND (manual_sdi = ? )";
			bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(transfer.getManualSDId())));
		}
		if (keys.get("Linked Id") != null) {
			w = w + " AND (linked_id = ? )";
			bindVariables.add(new CalypsoBindVariable(3000, Long.valueOf(transfer.getLinkedLongId())));
		}
		if (keys.get("Return Flag") != null) {
			w = w + " AND (is_return = ? )";
			bindVariables.add(new CalypsoBindVariable(16, Boolean.valueOf(transfer.getIsReturnB())));
		}
		try {
			TransferArray v = DSConnection.getDefault().getRemoteBO().getBOTransfers(w, bindVariables);
			if (v.size() > 0) {
				Vector vv = NettingConfig.getStaticKeys();
				Iterator<String> it = keys.keySet().iterator();
				while (it.hasNext()) {
					String ss = it.next();

					if (vv.contains(ss))
						continue;
					for (int a = 0; a < v.size(); a++) {
						BOTransfer element = v.elementAt(a);
						if (element.getAttribute(ss) != null || transfer.getAttribute(ss) != null)

							if (element.getAttribute(ss) == null || transfer.getAttribute(ss) == null) {
								v.remove(element);
								a--;

							} else if (!element.getAttribute(ss).equals(transfer.getAttribute(ss))) {
								v.remove(element);
								a--;

							}
					}
				}
				return v;
			}
			return null;

		} catch (Exception e) {
			Log.error(this, e);

			return null;
		}
	}
}
