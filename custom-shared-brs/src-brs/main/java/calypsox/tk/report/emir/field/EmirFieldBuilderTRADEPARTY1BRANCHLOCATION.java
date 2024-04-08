
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.service.DSConnection;

public class EmirFieldBuilderTRADEPARTY1BRANCHLOCATION implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String value = EmirSnapshotReduxConstants.EMPTY_SPACE;
        final DSConnection dsConn = DSConnection.getDefault();
        LegalEntity le =    trade.getBook().getLegalEntity();
        String countryName = "";
        if (!le.getCountry().equals("NONE")) {
            countryName = le.getCountry();
            try {
                Country country = BOCache.getCountry(dsConn, countryName);
                if (null != country) {
                    value = country.getISOCode();
                }
            } catch (Exception e) {
                Log.error(this, "Error Extractin ISO Country from " + countryName + ": ", e);
            }

        }
        return value;
    }
}
