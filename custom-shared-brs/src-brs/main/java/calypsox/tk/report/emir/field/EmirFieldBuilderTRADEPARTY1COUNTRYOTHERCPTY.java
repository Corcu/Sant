
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.service.DSConnection;

public class EmirFieldBuilderTRADEPARTY1COUNTRYOTHERCPTY
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        String  strCountry =  trade.getBook().getLegalEntity().getCountry();

        final Country country = BOCache.getCountry(
                DSConnection.getDefault(), strCountry);
        if (null != country) {
            rst = country.getISOCode();
        } else {
            Log.info(this,
                    "Error getting the Country of the PO "
                            +trade.getBook().getLegalEntity().getCode());
        }

        return EmirSnapshotReduxConstants.ES;
    }
}
