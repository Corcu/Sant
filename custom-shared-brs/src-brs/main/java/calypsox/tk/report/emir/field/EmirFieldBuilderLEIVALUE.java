
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderLEIVALUE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return LegalEntityAttributesCache.getInstance().getAttributeValue(trade,
                KeywordConstantsUtil.LE_ATTRIBUTE_LEI, false);
    }
}
