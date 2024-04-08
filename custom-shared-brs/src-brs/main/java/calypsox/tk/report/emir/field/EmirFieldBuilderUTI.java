package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderUTI implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_UTI_REFERENCE);
        if (Util.isEmpty(rst))      {
            rst = EmirFieldBuilderUtil.getInstance().getUtiTemporal(trade);
        }
        return  rst;
    }
}
