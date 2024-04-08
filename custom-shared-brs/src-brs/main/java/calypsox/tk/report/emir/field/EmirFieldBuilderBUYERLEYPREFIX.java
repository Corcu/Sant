
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderBUYERLEYPREFIX implements EmirFieldBuilder {
    @Override
    /*
   "Nueva logica
    Si somos el Pay del swap entonces informamos este campo con el valor ""LEI""
    Si somos el rec del swap entonces revisamos si la contrapartida tiene LEI. Si lo tiene informamos el valor ""LEI"". Si no lo tiene fijamos el valor ""INTERNAL""."

    */

    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String strLogic = EmirFieldBuilderUtil.getInstance().getLogicTRADEPARTY1COUNTERPARTYSIDE(trade);


        if (strLogic.equalsIgnoreCase(EmirSnapshotReduxConstants.BUY)) {
             rst = EmirSnapshotReduxConstants.LEI;
        }

        final String actualLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
        final String leiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);
        rst =  (!Util.isEmpty(leiValue)) ? leiValue : actualLeiValue;

        if (!Util.isEmpty(rst)) {
            rst = EmirSnapshotReduxConstants.LEI;
        } else {
                rst = EmirSnapshotReduxConstants.INTERNAL;
        }
        return rst;
    }
}
