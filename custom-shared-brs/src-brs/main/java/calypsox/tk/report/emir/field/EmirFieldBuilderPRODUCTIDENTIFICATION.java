
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderPRODUCTIDENTIFICATION implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String micCode = trade.getKeywordValue(
                EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE);
        if (EmirSnapshotReduxConstants.XXXX.equals(micCode) || (EmirSnapshotReduxConstants.EMPTY_SPACE.equals(micCode))
                || micCode == null) {
            rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        } else {
            final String idIsin = trade.getKeywordValue(
                    EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_IDENTIFICATION_ISIN);
            if (!Util.isEmpty(idIsin)) {
                rst = idIsin;
            }
        }

        return rst;
    }
}
