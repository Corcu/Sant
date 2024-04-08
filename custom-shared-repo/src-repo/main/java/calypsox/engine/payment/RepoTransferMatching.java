package calypsox.engine.payment;

import com.calypso.engine.payment.MatchingInfo;
import com.calypso.engine.payment.SecFinanceTransferMatching;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class RepoTransferMatching extends SecFinanceTransferMatching {

	@Override
	public int match(BOTransfer newTransfer, BOTransfer oldTransfer, MatchingInfo matchingInfo) {

		String activeXferMarchStatus = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "RepoXferMatchStatus");
		if (!Util.isEmpty(activeXferMarchStatus) && Boolean.parseBoolean(activeXferMarchStatus)) {
			if (!Util.isSame(newTransfer.getAttribute("DAPMatchKey1"), oldTransfer.getAttribute("DAPMatchKey1"))) {
				if (Log.isCategoryLogged("MATCHING")) {
					Log.debug("MATCHING", "DAPMatchKey1 Attribute");
				}

				return 0;
			} else if (!oldTransfer.getIsReturnB() && oldTransfer.getCallableDate() != null && newTransfer.getCallableDate() == null && isFailed(oldTransfer.getStatus()) && Util.idateToString(newTransfer.getValueDate()).equals(newTransfer.getAttribute("StartDate"))) {
				if (Log.isCategoryLogged("MATCHING")) {
					Log.debug("MATCHING", "Term on start date case: Callable Date new:" + newTransfer.getCallableDate() + " old:" + oldTransfer.getCallableDate());
				}

				return 0;
			} else {
				return -1;
			}
		} else {
			return super.match(newTransfer, oldTransfer, matchingInfo);
		}
	}

	private boolean isFailed(Status status) {
		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "customTransferFailedStatus");
		return null != status && !v.isEmpty() && v.contains(status.toString());
	}

	@Override
	public boolean isDefaultMatchingRequired(int columnId) {
		if (columnId == INTERNAL_SDIVERSION || columnId == EXTERNAL_SDIVERSION) {
			return false;
		}

		if (columnId == NETTING_TYPE) {
			return false;
		}

		if (columnId == INTERNAL_SDSTATUS || columnId == EXTERNAL_SDSTATUS) {
			return false;
		}

		return super.isDefaultMatchingRequired(columnId);
	}

}

