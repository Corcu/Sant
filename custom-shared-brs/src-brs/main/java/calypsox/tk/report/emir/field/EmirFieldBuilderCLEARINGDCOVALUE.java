package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCLEARINGDCOVALUE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        if (EmirFieldBuilderUtil.getInstance().isCCP(trade)) {
            rst = EmirFieldBuilderUtil.getInstance().getLogicClearingDcoValue(trade);
        }
       return rst;
    }
}
