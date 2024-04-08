package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

/**
 * TransferRule that cancels SPLIT child transfers afeter aply UNDO action in
 * SPLIT status
 * 
 * @author x957355
 *
 */

public class UndoErrorSplitCancelXfersTransferRule implements WfTransferRule {

	private static final String UNDO_XFER_ATTRIBUTE = "IsECMSUndoSplit";

	/**
	 * Check the rule to make the workflow transition
	 */
	@Override
	public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade,
			Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

		if (transfer.getStatus().equals(Status.S_SPLIT)) {
			try {
				TransferArray transfers = dsCon.getRemoteBackOffice().getBOTransfers(trade.getLongId(), true);

				if (!transfers.isEmpty()) {
					for (BOTransfer xfer : transfers) {
						if (xfer.getParentLongId() == transfer.getLongId()
								&& !xfer.getStatus().equals(Status.S_PENDING)) {
							return false; // Only can cancel PENDING transfer
						}
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error getting transfers.", e);
				return false;
			}

		} else {
			return false;
		}

		return true;
	}

	/**
	 * Returns the description of the rule
	 */
	@Override
	public String getDescription() {
		return "Cancel splited transfers after aply UNDO action in a transfer whith SPLIT status";
	}

	/*
	 * Update thee transfers afected by the workflow transition
	 */
	@Override
	public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade,
			Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

		TransferArray transfers;
		try {
			transfers = dsCon.getRemoteBackOffice().getBOTransfers(trade.getLongId(), true);
			if (!transfers.isEmpty()) {
				for (BOTransfer xfer : transfers) {
					if (xfer.getParentLongId() == transfer.getLongId() && xfer.getStatus().equals(Status.S_PENDING)) {
						Log.debug("UndoErrorSplitXfersCancelRule","Transfer id (PENDING):" +xfer.getLongId());
						cancelTransfer(trade, xfer);
					}
				}
			}

		} catch (CalypsoServiceException e) {
			Log.error(this, "Error candelling transfers.", e);
			return false;
		}
		return true;
	}

	/**
	 * Save BoTransfer
	 * 
	 * @param boTransfer
	 */
	private void saveBOTransfer(BOTransfer boTransfer) {
		try {
			DSConnection.getDefault().getRemoteBackOffice().save(boTransfer, 0, null);
		} catch (CalypsoServiceException e) {

			Log.error(this, "Error saving BOTransfer for trade: " + boTransfer.getTradeLongId());
		}
	}

	/**
	 * Cancel transfers and put xfer attribute
	 * 
	 * @param trade
	 * @param xfer
	 */
	private void cancelTransfer(Trade trade, BOTransfer xfer) {
		try {
			BOTransfer clone = (BOTransfer) xfer.clone();
			clone.setAction(Action.CANCEL);
			if (trade.getKeywordValue("isECMSPledge") != null && trade.getKeywordValue("isECMSPledge").equals("Y")) {
				clone.setAttribute(UNDO_XFER_ATTRIBUTE, "Y");
				Log.debug("UndoErrorSplitXfersCancelRule attributo seteado IsECMSUndoSplit","Transfer id :" +xfer.getLongId());

			}

			saveBOTransfer(clone);
		} catch (CloneNotSupportedException e) {
			Log.error(this, "Error cloning BOTransfer: " + e);
		}
	}

}
