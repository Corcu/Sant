
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderBUYERLEYVALUE implements EmirFieldBuilder {
    @Override
    /*
    Miramos el valor del campo "Trade Party 1 - Counterparty Side".
    Si es Buyer, entonces ponemos el LEI de la PO de la operación
    Si es Seller, entonces ponemos el LEI de la CTPY> si el valor
    */

    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        String strLogic = EmirFieldBuilderUtil.getInstance().getLogicTRADEPARTY1COUNTERPARTYSIDE(trade);

        boolean isCpty = true;
        if (strLogic.equalsIgnoreCase(EmirSnapshotReduxConstants.BUY)) {
            isCpty = false;
        }

        final String actualLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
        final String leiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);
        rst =  (!Util.isEmpty(leiValue)) ? leiValue : actualLeiValue;


        if (Util.isEmpty(rst)) {
            if (isCpty) {
                rst = trade.getCounterParty().getCode();
            } else {
                rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
            }
        }
        return rst;
    }
}
