
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderTRADEPARTYVAL1 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String value = EmirSnapshotReduxConstants.EMPTY_SPACE;

        value = LegalEntityAttributesCache.getInstance().getAttributeValue(
                trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, false);

        if (Util.isEmpty(value)) {
            value = EmirSnapshotReduxConstants.EMPTY_SPACE;
        }

        return value;
    }
}
