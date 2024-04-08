
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

public class EmirFieldBuilderTRADEPARTY2COUNTRYOTHERCPTY
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        final LegalEntity cpty = trade.getCounterParty();
        // FindBugs: Check if cpty is null before dereferencing it.
        if (cpty != null) {

            LegalEntityAttribute ownershipAttribute = LegalEntityAttributesCache
                    .getInstance().getAttribute(0, cpty.getId(),
                            LegalEntityAttributesCache.ALL_ROLES,
                            EmirSnapshotReduxConstants.LE_ATTRIBUTE_OWNERSHIP_COUNTRY);
            if (ownershipAttribute != null) {
                rst = ownershipAttribute.getAttributeValue();
            }

            if (Util.isEmpty(rst)) {
                final Country country = BOCache.getCountry(
                        DSConnection.getDefault(), cpty.getCountry());
                if (null != country) {
                    rst = country.getISOCode();
                } else {
                    Log.info(this,
                            "Error getting the Country of the counterparty "
                                    + cpty.getName());
                }
            }
        }

        return rst;
    }
}
