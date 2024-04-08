
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;

public class EmirFieldBuilderSUBMITTEDVALUEDelegate
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        final String actualLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
        final String leiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);
        String rst  =  (!Util.isEmpty(leiValue)) ? leiValue : actualLeiValue;
        if (Util.isEmpty(rst)) {
            rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        }
        return rst;
    }
}
