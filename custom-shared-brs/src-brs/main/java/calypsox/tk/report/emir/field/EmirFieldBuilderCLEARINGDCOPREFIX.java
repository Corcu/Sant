package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderCLEARINGDCOPREFIX implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {


        if (EmirFieldBuilderUtil.getInstance().isCCP(trade)) {
           if (Util.isEmpty(EmirFieldBuilderUtil.getInstance().getLogicClearingDcoValue(trade))) {
               return EmirSnapshotReduxConstants.LEI;
           }
        }

       return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}
