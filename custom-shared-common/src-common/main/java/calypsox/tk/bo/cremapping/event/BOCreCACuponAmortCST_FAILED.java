package calypsox.tk.bo.cremapping.event;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class BOCreCACuponAmortCST_FAILED extends BOCreCACuponAmortCST {

	public BOCreCACuponAmortCST_FAILED(BOCre cre, Trade trade) {
		super(cre, trade);
	}

	public void fillValues() {
		super.fillValues();
		// CA RF
		this.nettingType = getCANettingType();
	}

	private String getCANettingType() {
		BOTransfer transfer = null;
		try {
			transfer = DSConnection.getDefault().getRemoteBackOffice().getBOTransfer(this.boCre.getTransferLongId());
		} catch (CalypsoServiceException e) {
			Log.error(this, "Unabale to get Transfer: " + this.boCre.getTransferLongId());
		}
		return (transfer != null && transfer.getNettedTransfer()) ? "N"
				: getInstance().getNettingType(this.creBoTransfer);
	}
}
