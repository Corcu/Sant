
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderBENEFICIARYIDPARTY2VALUE
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        String IDPrefix = EmirFieldBuilderUtil.getInstance().getLogicBENEFICIARYIDPARTY2PREFIX(trade);
        if (EmirSnapshotReduxConstants.LEI.equalsIgnoreCase(IDPrefix)) {

            final String actualLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
            final String leiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);
            rst =  (!Util.isEmpty(leiValue)) ? leiValue : actualLeiValue;
            if (Util.isEmpty(rst)) {
                LegalEntity cpty = trade.getCounterParty();
                if (cpty != null) {
                    rst = cpty.getCode();
                }
            }
        }

        return rst;
    }
}
