package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

public class MxNettingXferIsWfSubtypeSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {

	/** SDFilterCriterion attribute name **/
	private static final String CRITERION_NAME = "MxNettingXferIsWfSubtype";
	private static final String MX_TRADES = "MX_Trades";
	protected static final String WORKFLOWSUBTYPE = "WorkflowSubType";

	@Override
	public SDFilterCategory getCategory() {
		return SDFilterCategory.TRANSFER;
	}

	@Override
	public Class<Boolean> getValueType() {
		return Boolean.class;
	}

	@Override
	public String getName() {
		return CRITERION_NAME;
	}

	/*
	 * Checks if a given transfer is netted and if their underlying are from Mexico,
	 * checking if its WorkflowSubType attribute is equals to MX_Trades
	 */
	@Override
	public Boolean getValue(SDFilterInput sdFilterInput) {
		boolean isMxNettingType = false;
		final BOTransfer transfer = sdFilterInput.getTransfer();

		if (transfer != null && transfer.getNettedTransfer()) {
			TransferArray underlyingTransfers;
			try {
				underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(transfer.getLongId());
				if (underlyingTransfers != null) {
					isMxNettingType = true;
					for (int i = 0; i < underlyingTransfers.size(); i++) {
						if (!MX_TRADES.equals(underlyingTransfers.get(i).getAttribute(WORKFLOWSUBTYPE))) {
							isMxNettingType = false;
							break;
						}
					}
				}
			} catch (final CalypsoServiceException exception) {
				final String exceptionMsg = "Not posible take the underlying transfers of transfer ID: "
						+ transfer.getLongId();
				Log.error(this, exceptionMsg, exception);
			}
		}

		return isMxNettingType;
	}

}
