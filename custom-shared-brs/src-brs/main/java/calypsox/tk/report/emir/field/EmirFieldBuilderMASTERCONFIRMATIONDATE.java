
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class EmirFieldBuilderMASTERCONFIRMATIONDATE
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(),  EmirSnapshotReduxConstants.DV_EMIR_MASTER_DOCUMENT_DATE);
        if (!Util.isEmpty(v)) {
            rst = v.get(0);
        }

        return rst;
    }
}
