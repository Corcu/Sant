
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.LifeCycleEventValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.util.KeywordConstants;

public class EmirFieldBuilderPRIORUTIPREFIX implements EmirFieldBuilder {
	@Override
	public String getValue(Trade trade) {

		String rst  = trade.getKeywordValue(KeywordConstants.PRIORUTIPREFIX);
		if (Util.isEmpty(rst)) {
			rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
		}
		return rst;

	}
}
