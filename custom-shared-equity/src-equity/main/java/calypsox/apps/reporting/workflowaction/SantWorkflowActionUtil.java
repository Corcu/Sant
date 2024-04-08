package calypsox.apps.reporting.workflowaction;

import java.awt.Frame;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.apps.reporting.TSSettlementPanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.BOTransferUtil;
import com.calypso.tk.bo.util.ProcessTaskUtil;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.ExternalArray;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.RoundingMethod;
import com.calypso.tk.core.Status;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.BONYUtil;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.TransferArray;

public class SantWorkflowActionUtil {
	private String SETTLE_WINDOW_PRODUCTS_DV = "SettleWindowProducts";
	
	public boolean handleWorkflowAction(Action action, BOTransfer transfer, List<String> messages, Frame frame) {
		Vector<String> errors = new Vector<String>();
		boolean res = (boolean)settle(action, transfer, errors, frame);
		if (errors.size() > 0) {
			for (String error : errors) {
				messages.add(error);
			}
		}
		return res;
		
//		ReportBOTransferHandler rth = new ReportBOTransferHandler();
//		rth.setFrame((JFrame)owner);
//
//		Vector<String> errors = new Vector<String>();
//		try {
//			Method settleMethod = ReportBOTransferHandler.class.getDeclaredMethod("settle", BOTransfer.class, Vector.class);
//			settleMethod.setAccessible(true);
//			boolean res = (boolean)settleMethod.invoke(rth, transfer, errors);
//			if (!res) {
//				messages.add("Error applying " + action + " action.\n");
//				
//				if (errors.size() > 0) {
//					for (String error : errors) {
//						messages.add(error);
//					}
//				}
//			}
//			return res;
//		} catch (NoSuchMethodException e) {
//			Log.error(this, "Could not find settle method for invocation : " + e.toString());
//		} catch (SecurityException e) {
//			Log.error(this, "Security Exception for invocation : " + e.toString());
//		} catch (IllegalAccessException e) {
//			Log.error(this, "IllegalAccessException for invocation : " + e.toString());
//		} catch (IllegalArgumentException e) {
//			Log.error(this, "IllegalArgumentException for invocation : " + e.toString());
//		} catch (InvocationTargetException e) {
//			Log.error(this, "InvocationTargetException for invocation : " + e.toString());
//		}
	}

	public boolean isWorkflowActionImplemented(Action action, BOTransfer[] transfers) {
		if (transfers == null || transfers.length == 0) {
			return false;
		}
		
		Vector<String> dvProductTypes = LocalCache.getDomainValues(DSConnection.getDefault(), SETTLE_WINDOW_PRODUCTS_DV);
        if (Util.isEmpty(dvProductTypes)) {
            Log.error(this, "No products specified under domain " + SETTLE_WINDOW_PRODUCTS_DV);
            return false;
        }
		
        BOTransfer transfer = transfers[0];
		String productType = transfer.getProductType();
		if (dvProductTypes.contains(productType)) {
			return true;
		}
		
		return false;
	}
	
	protected boolean settle(Action action, BOTransfer transfer, Vector<String> msgs, Frame frame) {
		Vector<BOTransfer> transfers = new Vector();
		transfers.addElement(transfer);
		TSSettlementPanel sp = new TSSettlementPanel(frame, true);
		sp.setTransfers(transfers);
		sp.setModal(true);
		sp.setVisible(true);
		if (!sp.getAnswer())
			return false; 
		Vector<BOTransfer> updatedTransfers = sp.getTransfers();
		if (updatedTransfers == null) {
			AppUtil.displayError("Could not get the Transfers", frame);
			return false;
		} 
		TransferArray nettedModTransfers = sp.getNettedTransfers();
		Hashtable<Object, Object> netHashtable = new Hashtable<>();
		if (nettedModTransfers != null)
			for (int j = 0; j < nettedModTransfers.size(); j++) {
				BOTransfer tt = (BOTransfer)nettedModTransfers.get(j);
				TransferArray tArray = (TransferArray)netHashtable.get(Long.valueOf(tt.getNettedTransferLongId()));
				if (tArray == null) {
					tArray = new TransferArray();
					netHashtable.put(Long.valueOf(tt.getNettedTransferLongId()), tArray);
				} 
				tArray.add(tt);
			}  
		int i = 0;
		if (i < transfers.size()) {
			Vector<BOTransfer> payments = new Vector();
			Vector<BOTransfer> savetransfers = new Vector();
			Vector<BOTransfer> links = new Vector();
			BOTransfer stransfer = updatedTransfers.elementAt(i);
			double ratio = 1.0D;
			if (stransfer.getSettlementAmount() != 0.0D)
				ratio = Math.abs(stransfer.getRealSettlementAmount() / stransfer
						.getSettlementAmount()); 
			double cashRatio = 1.0D;
			if (stransfer.getOtherAmount() != 0.0D && 
					stransfer.getDeliveryType().equals("DAP"))
				cashRatio = Math.abs(stransfer.getRealCashAmount() / stransfer
						.getOtherAmount()); 
			stransfer.setAction(action);
			if (BONYUtil.isMoneyDiffSimpleTransfersProcess(stransfer)) {
				Vector errors = new Vector();
				BONYUtil.createAndSaveMoneyDiffSimpleTransfers(stransfer, 
						DSConnection.getDefault(), null, errors, null);
				if (!Util.isEmpty(errors)) {
					msgs.add("Can Not create MoneyDiff trades for transfer " + stransfer
							.getLongId() + " :\n" + 
							Util.collectionToString(errors, "\n"));
				} else {
					stransfer.setRealSettlementAmount(stransfer.getSettlementAmount());
					stransfer.setRealCashAmount(stransfer.getOtherAmount());
					payments.addElement(stransfer);
				} 
			} else if (stransfer.getNettedTransfer()) {
				TransferArray nettedTransfers = getNettedTransfers(stransfer.getLongId());
				TransferArray modNettedTransfers = (TransferArray)netHashtable.get(Long.valueOf(stransfer.getLongId()));
				if (modNettedTransfers == null) {
					for (int k = 0; k < nettedTransfers.size(); k++) {
						BOTransfer nettedTransfer = (BOTransfer)nettedTransfers.get(k);
						int digits = BOTransferUtil.getDigits(nettedTransfer);
						String cur = nettedTransfer.getSettlementCurrency();
						int otherdigits = CurrencyUtil.getRoundingUnit(cur);
						double amount = nettedTransfer.getSettlementAmount();
						amount = RoundingMethod.roundNearest(amount * ratio, digits);
						nettedTransfer.setRealSettlementAmount(amount);
						amount = nettedTransfer.getOtherAmount();
						amount = RoundingMethod.roundNearest(amount * cashRatio, otherdigits);
						nettedTransfer.setRealCashAmount(amount);
						nettedTransfer.setSettleDate(stransfer.getSettleDate());
						nettedTransfer.setAction(Action.SETTLE);
						links.addElement(stransfer);
						links.addElement(nettedTransfer);
						savetransfers.addElement(nettedTransfer);
					} 
				} else {
					for (int k = 0; k < modNettedTransfers.size(); k++) {
						BOTransfer nettedTransfer = (BOTransfer)modNettedTransfers.get(k);
						nettedTransfer.setSettleDate(stransfer.getSettleDate());
						nettedTransfer.setAction(Action.SETTLE);
						links.addElement(stransfer);
						links.addElement(nettedTransfer);
						savetransfers.addElement(nettedTransfer);
					} 
				} 
				payments.addElement(stransfer);
			} else {
				savetransfers.addElement(stransfer);
			} 
			boolean error = false;
			try {
				int l;
				for (l = 0; l < savetransfers.size(); l++) {
					BOTransfer tr = savetransfers.elementAt(l);
					tr.setEnteredUser(DSConnection.getDefault().getUser());
				} 
				for (l = 0; l < payments.size(); l++) {
					BOTransfer tr = payments.elementAt(l);
					tr.setEnteredUser(DSConnection.getDefault().getUser());
				} 
				ProcessTaskUtil.getAndSetGenericComments(transfer);
				DSConnection.getDefault().getRemoteBO()
				.saveTransfers(0L, null, new TransferArray(savetransfers), new TransferArray(payments), new TransferArray(links), new ExternalArray());
			} catch (RemoteException e) {
				RemoteException ex = e;
				while (ex.detail instanceof RemoteException)
					ex = (RemoteException)ex.detail; 
				String msg = null;
				if (ex.detail == null) {
					msg = ex.getMessage();
				} else {
					msg = ex.detail.getMessage();
				} 
				msgs.add("Error Applying Settle Action on Transfer " + stransfer
						.getLongId() + ": " + msg);
				error = true;
			} catch (Exception e) {
				String msg = e.getMessage();
				msgs.add("Error Applying Settle Action on Transfer " + stransfer
						.getLongId() + ": " + msg);
				error = true;
			} 
			return !error;
		} 
		return true;
	}

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
}
