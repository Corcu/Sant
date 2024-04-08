
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderEXECUTIONVENUEPREFIX implements EmirFieldBuilder {

    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EXECUTION_VENUE_PREFIX;

        if (!Util.isEmpty(EmirFieldBuilderUtil.getInstance().getLogicEXECUTIONVENUE(trade)))  {
            rst = EmirSnapshotReduxConstants.FREEFORMATTEXT;
        }
        return rst;
    }
}
