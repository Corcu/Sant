
package calypsox.tk.report.emir.field;


import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCLEARINGTIMESTAMP implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String isCleared = EmirFieldBuilderUtil.getInstance().getLogicCLEARINGSTATUS(trade);

        String result = EmirSnapshotReduxConstants.EMPTY_SPACE;

        if (Boolean.TRUE.toString().equalsIgnoreCase(isCleared)) {

        }
        /*
        if (GenericReg_SantDTCCGTRUtil.getInstance().isForexClear(trade)) {

            String s = trade.getKeywordValue(
                    KeywordConstantsBlotterFx.BLOTTER_FX_CCP_CLEARING_TIMESTAMP);

            if (null != s) {
                result = s;
            }
        }
        */
        //TODO - INDENTIFICAR ccps
        return result;
    }
}
