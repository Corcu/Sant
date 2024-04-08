
package calypsox.tk.report.emir.field;

import calypsox.tk.core.SantanderUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class EmirFieldBuilderCOUNTERPARTYREGION implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        final Vector<String> allCountries = EmirFieldBuilderUtil.getInstance()
                .castVector(String.class, LocalCache.getDomainValues(
                        DSConnection.getDefault(), EmirSnapshotReduxConstants.EEA_COUNTRIES));

        final String country = trade.getCounterParty().getCountry();
        // CAL_710
        if (!Util.isEmpty(country) && !allCountries.isEmpty()) {
            if (SantanderUtil.getInstance().containsCaseInsensitive(country,
                    allCountries)) {
                return EmirSnapshotReduxConstants.EEA;
            } else {
                return EmirSnapshotReduxConstants.NON_EEA;
            }
        }
        return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}
