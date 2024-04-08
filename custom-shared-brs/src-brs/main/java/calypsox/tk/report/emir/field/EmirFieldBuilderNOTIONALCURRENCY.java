
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.*;

public class EmirFieldBuilderNOTIONALCURRENCY implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);

        if (pLeg != null) {
            rst = pLeg.getCurrency();
        }
        return rst;
    }

}
