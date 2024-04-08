package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class EmirFieldBuilderPAYFREQPERIODLEG1 implements EmirFieldBuilder {

    private static final int IDX_LITERAL = 1;
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            rst = EmirFieldBuilderUtil.getInstance().getMappedValueCouponFrequency(pLeg.getCouponFrequency().toString(), IDX_LITERAL);
        }
        return rst;
    }


}
