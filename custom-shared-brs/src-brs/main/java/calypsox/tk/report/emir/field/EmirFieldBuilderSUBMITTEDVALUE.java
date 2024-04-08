
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LegalEntityAttribute;

public class EmirFieldBuilderSUBMITTEDVALUE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String value = EmirSnapshotReduxConstants.EMPTY_SPACE;

        int leId = trade.getBook().getLegalEntity().getId();

        LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
                .getInstance().getAttribute(0, leId,
                        LegalEntityAttributesCache.ALL_ROLES,
                        KeywordConstantsUtil.LE_ATTRIBUTE_LEI);
        if (leiAttribute != null) {
            value = leiAttribute.getAttributeValue();
        }

        return value;
    }
}
