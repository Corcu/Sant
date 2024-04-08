package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderSINGINIPAYAMOUNTPAYER implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        Fee upFrontFee =  EmirFieldBuilderUtil.getInstance().getPremiumFee(trade);
        if (upFrontFee != null)  {
            boolean isCpty = false;
            if (upFrontFee.getAmount() > 0.0) {
                isCpty = true;
            }

            if (isCpty) {
                final String actualLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
                final String leiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);
                rst =  (!Util.isEmpty(leiValue)) ? leiValue : actualLeiValue;
            } else {
                rst = LegalEntityAttributesCache.getInstance().
                    getAttributeValue(trade, EmirSnapshotReduxConstants.LEI, false);
            }

            if (Util.isEmpty(rst)) {
                rst = isCpty ?
                        trade.getCounterParty().getCode()
                            : trade.getBook().getLegalEntity().getCode();
            }

        }
        return rst;
    }
}
