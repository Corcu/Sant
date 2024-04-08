package calypsox.tk.bo;

import java.util.ArrayList;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOProductHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

/**
 * The class BOMarginCallHandler
 * 
 * @author x948800
 *
 */
public class BOMarginCallHandler extends com.calypso.tk.bo.BOMarginCallHandler {
	private static final String ECMS_PLEDGE_INTERNAL_ACCOUNTS = "ECMS_PLEDGE_INTERNAL_ACCOUNTS";
	private static final String DV_SEPARATOR = ";";

	/**
	 * The method performMatching(). Control Amend for the quantity in case of
	 * error, manually performed by the user.
	 * 
	 * @param finalTransferList.   List transfer with new Transfers Created.
	 * @param newTransferList.     List transfers with new Transfers Created.
	 * @param oldTransferList.     List transfers with all Transfers Old.
	 * @param initNewTransferList. List transfers with new Transfers Created.
	 * @param initOldTransferList. List transfers with all Transfers Old.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void performMatching(Vector finalTransferList, Vector newTransferList, Vector oldTransferList,
			Vector initNewTransferList, Vector initOldTransferList, Trade trade) {

		if ((!Util.isEmpty(trade.getKeywordValue("isECMSPledge")))
				&& (trade.getKeywordValue("isECMSPledge").equals("Y"))) {
			updateAccountRemove(finalTransferList);
			if (isECMSAmend(trade)) {
				if (isStatusXfer(oldTransferList)) {
					checkAllTransfer(finalTransferList, oldTransferList,
							getNominal(Math.abs(trade.getQuantity()), oldTransferList));

				} else {
					checkCancelXferLinked(finalTransferList, oldTransferList, trade);
				}
				return;
			}
		}
		super.performMatching(finalTransferList, newTransferList, oldTransferList, initNewTransferList,
				initOldTransferList, trade);

	}

	/**
	 * The checkAllTransfer (). Check new Transfer and check old Transfer
	 * 
	 * @param finalTransferList
	 * @param oldTransferList
	 * @param nominalDiff
	 */
	private void checkAllTransfer(Vector<BOTransfer> finalTransferList, Vector<BOTransfer> oldTransferList,
			double[] nominalDiff) {
		updateXferNominal(finalTransferList, nominalDiff);
		updateOldTransferList(oldTransferList, nominalDiff);

	}

	/**
	 * The method checkCancelXferLinked(). Check the reversal transfer for
	 * cancellations.
	 * 
	 * @param finalTransferList
	 * @param oldTransferList
	 * @param trade
	 */
	private void checkCancelXferLinked(Vector<BOTransfer> finalTransferList, Vector<BOTransfer> oldTransferList,
			Trade trade) {
		if (finalTransferList != null) {
			BOTransfer[] xfers = getAllXfers(trade);
			if (xfers != null && isXfersSettled(xfers)) {
				double[] nominalDiff = getNominal(Math.abs(trade.getQuantity()), xfers);
				checkAllTransfer(finalTransferList, oldTransferList, nominalDiff);
				updateCancelTransferList(xfers, trade, nominalDiff);

			}
		}
	}

	/**
	 * The method get Nominal(). Calculate the Nominal of settled transfers
	 * 
	 * @param quantity
	 * @param oldTransferList
	 * @return Nominal
	 */
	private double[] getNominal(double quantity, BOTransfer[] xfers) {
		double[] nominal = new double[2];
		double amountSettled = 0;
		double amountSettledReceive = 0;
		for (BOTransfer xfer : xfers) {
			if (xfer.getStatus().equals(Status.S_SETTLED)) {

				if (BOProductHandler.PAY.equals(xfer.getPayReceiveType())) {
					amountSettled += xfer.getSettlementAmount();
				} else if (BOProductHandler.RECEIVE.equals(xfer.getPayReceiveType())) {
					amountSettledReceive += xfer.getSettlementAmount();
				}
			}
		}
		return getNominalDiff(quantity, nominal, amountSettled, amountSettledReceive);
	}

	/**
	 * The method updateCancelTransferList(). Apply Action Cancel
	 * 
	 * @param oldTransferList
	 * @param xfers
	 * @param trade
	 */
	private void updateCancelTransferList(BOTransfer[] xfers, Trade trade, double[] nominalDiff) {
		for (BOTransfer xfer : xfers) {
			if (xfer.getLinkedLongId() < 0) {
				if (isCancelAplicable(xfer, trade)) {
					if ((BOProductHandler.PAY.equals(xfer.getPayReceiveType()) && nominalDiff[0] == 0)
							|| (BOProductHandler.RECEIVE.equals(xfer.getPayReceiveType()) && nominalDiff[1] == 0)) {
						xfer.setAction(Action.CANCEL);
						saveXfer(xfer);
					}
				} else {
					Log.info("Not possible to apply Cancel on trade Id :" + trade.getLongId());
				}
			}
		}
	}

	/**
	 * The method saveXfer(). Save transfer
	 * 
	 * @param xfer
	 */
	private void saveXfer(BOTransfer xfer) {
		try {
			long none = DSConnection.getDefault().getRemoteBO().save(xfer, 0, "None");
			Log.info("Saved Transfer Id : " + none);
		} catch (CalypsoServiceException exp) {
			Log.error("Error saving BoTransfer: " + exp);
		}
	}

	/**
	 * The method isCancelAplicable(). Check if the transfer can be cancelled
	 * 
	 * @param xfer
	 * @param trade
	 * @return boolean
	 */
	private boolean isCancelAplicable(BOTransfer xfer, Trade trade) {
		return BOTransferWorkflow.isTransferActionApplicable(xfer, trade, Action.CANCEL, DSConnection.getDefault());
	}

	/**
	 * The method get Nominal(). Calculate the Nominal of settled transfers
	 * 
	 * @param quantity
	 * @param oldTransferList
	 * @return Nominal
	 */
	private double[] getNominal(Double quantity, Vector<BOTransfer> oldTransferList) {
		BOTransfer xfer;
		double[] nominal = new double[2];
		double amountSettled = 0;
		double amountSettledReceive = 0;
		for (int k = 0; k < oldTransferList.size(); k++) {
			xfer = oldTransferList.elementAt(k);
			if (xfer.getStatus().equals(Status.S_SETTLED)) {

				if (BOProductHandler.PAY.equals(xfer.getPayReceiveType())) {
					amountSettled += xfer.getSettlementAmount();
				} else if (BOProductHandler.RECEIVE.equals(xfer.getPayReceiveType())) {
					amountSettledReceive += xfer.getSettlementAmount();
				}
			}
		}
		return getNominalDiff(quantity, nominal, amountSettled, amountSettledReceive);

	}

	/**
	 * The method isXfersSettled(). Check if the transfer is settled
	 * 
	 * @param xfers
	 * @return boolean
	 */
	private boolean isXfersSettled(BOTransfer[] xfers) {
		for (BOTransfer xfer : xfers) {
			if (Status.S_SETTLED.equals(xfer.getStatus())) {
				return true;
			}
		}
		return false;

	}

	/**
	 * The method getAllXfers(). Get all transfers for trade
	 * 
	 * @param trade
	 * @return BOTransfer []
	 */
	private BOTransfer[] getAllXfers(Trade trade) {
		try {
			final TransferArray transfers = DSConnection.getDefault().getRemoteBO().getTransfers(null,
					"trade_id = " + trade.getLongId(), null);
			return transfers.getTransfers();
		} catch (CalypsoServiceException exp) {
			Log.error("Something went wrong while getting the transfers of Trade id " + trade.getLongId() + " "
					+ exp.getCause());
			return null;

		}

	}

	/**
	 * The method updateXferNominal().Check the new transfers to be filtered
	 * 
	 * @param finalTransferList
	 * @param nominalDiff
	 */
	private void updateXferNominal(Vector<BOTransfer> finalTransferList, double[] nominalDiff) {

		if (!finalTransferList.isEmpty()) {
			Vector<BOTransfer> removeFinalTransferList = new Vector<>();
			BOTransfer xfer;
			if (nominalDiff[0] == 0.0 || nominalDiff[1] == 0.0) {
				for (int i = 0; i < finalTransferList.size(); i++) {
					xfer = finalTransferList.elementAt(i);
					if ((BOProductHandler.PAY.equals(xfer.getPayReceiveType()) && nominalDiff[0] == 0.0)
							|| (BOProductHandler.RECEIVE.equals(xfer.getPayReceiveType()) && nominalDiff[1] == 0.0)) {
						removeFinalTransferList.add(xfer);
					}
				}
			}
			deleteTransfers(removeFinalTransferList, finalTransferList);

		}

	}

	/**
	 * The method updateAccountRemove(). Filter the transfers that belong to
	 * internal accounts
	 * 
	 * @param finalTransferList
	 */
	private void updateAccountRemove(Vector<BOTransfer> finalTransferList) {
		if (!finalTransferList.isEmpty()) {
			deleteTransfers(addTransfersList(finalTransferList), finalTransferList);

		}

	}

	/**
	 * The method addTransfersList(). Add transfers to the list for later deletion.
	 * 
	 * @param finalTransferList
	 * @return Vector<BOTransfer>
	 */
	private Vector<BOTransfer> addTransfersList(Vector<BOTransfer> finalTransferList) {
		ArrayList<String> accountsFinal = getEcmsAccounts();
		Account account;
		Vector<BOTransfer> removeFinalTransferList = new Vector<>();
		BOTransfer xfer;
		for (int i = 0; i < finalTransferList.size(); i++) {
			xfer = finalTransferList.elementAt(i);
			account = BOCache.getAccount(DSConnection.getDefault(), xfer.getGLAccountNumber());
			if (accountsFinal.contains(account.getName()))
				removeFinalTransferList.add(xfer);

		}
		return removeFinalTransferList;
	}

	/**
	 * The method updateOldTransferList ().Delete transfers.
	 * 
	 * @param oldTransferList
	 * @param nominalDiff
	 */
	private void updateOldTransferList(Vector<BOTransfer> oldTransferList, double[] nominalDiff) {
		if (!oldTransferList.isEmpty()) {
			deleteTransfers(addRemoveFinalTransferList(oldTransferList, nominalDiff), oldTransferList);

		}

	}

	/**
	 * The method addRemoveFinalTransferList(). Add transfers to the list for later
	 * deletion.
	 * 
	 * @param oldTransferList
	 * @param nominalDiff
	 * @return Vector<BOTransfer>
	 */
	private Vector<BOTransfer> addRemoveFinalTransferList(Vector<BOTransfer> oldTransferList, double[] nominalDiff) {
		BOTransfer xfer;
		Vector<BOTransfer> removeFinalTransferList = new Vector<>();

		for (int i = 0; i < oldTransferList.size(); i++) {
			xfer = oldTransferList.elementAt(i);
			if ((BOProductHandler.PAY.equals(xfer.getPayReceiveType()) && nominalDiff[0] == 0)
					|| (BOProductHandler.RECEIVE.equals(xfer.getPayReceiveType()) && nominalDiff[1] == 0)) {
				if (isStatusXferSettled(xfer)) {
					removeFinalTransferList.add(xfer);
				}

			}
		}
		return removeFinalTransferList;
	}

	/**
	 * The method deleteTransfers(). Delete transfers.
	 * 
	 * @param xfersFinal
	 * @param removeXfersFinal
	 */
	private void deleteTransfers(Vector<BOTransfer> xfersFinal, Vector<BOTransfer> removeXfersFinal) {
		BOTransfer removeXfer;
		for (int k = 0; k < xfersFinal.size(); k++) {
			removeXfer = xfersFinal.elementAt(k);
			removeXfersFinal.remove(removeXfer);

		}

	}

	/**
	 * The method getNominalDiff(). Calculate NominalDiff.
	 * 
	 * @param quantity
	 * @param nominal
	 * @param amountSettled
	 * @param amountSettledRecieve
	 * @return NominalDiff
	 */
	private double[] getNominalDiff(double quantity, double[] nominal, double amountSettled,
			double amountSettledRecieve) {
		if (quantity == amountSettled) {
			nominal[0] = 0;
		} else
			nominal[0] = quantity - amountSettled;
		if (quantity == amountSettledRecieve) {
			nominal[1] = 0;
		} else
			nominal[1] = quantity - amountSettledRecieve;
		return nominal;

	}

	/**
	 * The method isECMSAmend(). check for Amend action on the trade.
	 * 
	 * @param trade
	 * @return boolean
	 */
	private boolean isECMSAmend(Trade trade) {
		return trade.getAction().equals(Action.AMEND);
	}

	/**
	 * The method isStatusXfer(). Check for SPLIT and SETTLE Status on the
	 * transfers.
	 * 
	 * @param oldTransferList
	 * @return
	 */
	private boolean isStatusXfer(Vector<BOTransfer> oldTransferList) {
		boolean isStatusSplit = false;
		boolean isStatusSettled = false;
		if (!oldTransferList.isEmpty()) {

			for (BOTransfer xfer : oldTransferList) {
				if (xfer.getStatus().equals(Status.S_SPLIT)) {
					isStatusSplit = true;
				}
				if (xfer.getStatus().equals(Status.S_SETTLED)) {
					isStatusSettled = true;
				}
				if (isStatusSplit && isStatusSettled)
					return true;
			}
		}
		return isStatusSplit && isStatusSettled;
	}

	/**
	 * The method isStatusXferSettled(). Check status Transfer
	 * 
	 * @param transfer
	 * @return boolean
	 */
	private boolean isStatusXferSettled(BOTransfer transfer) {
		return transfer.getStatus().equals(Status.S_SETTLED);

	}

	/**
	 * The method getEcmsAccounts(). get Ecms accounts.
	 * 
	 * @return ArrayList<String>
	 */
	private ArrayList<String> getEcmsAccounts() {
		ArrayList<String> accountsFinal = new ArrayList<>();
		Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(),
				ECMS_PLEDGE_INTERNAL_ACCOUNTS);

		for (String account : pledgeAccounts) {

			accountsFinal.add(account.split(DV_SEPARATOR)[1]);
		}

		return accountsFinal;
	}

}