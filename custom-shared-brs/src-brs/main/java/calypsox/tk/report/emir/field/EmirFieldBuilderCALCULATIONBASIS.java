
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Basket;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class EmirFieldBuilderCALCULATIONBASIS implements EmirFieldBuilder {
    @Override
    /*
    "Nueva logica
    Si somos el Pay del swap entonces informamos este campo con el valor ""LEI""
    Si somos el rec del swap entonces revisamos si la contrapartida tiene LEI. Si lo tiene informamos el valor ""LEI"". Si no lo tiene fijamos el valor ""INTERNAL""."
    */

    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        if (trade.getProduct() instanceof PerformanceSwap)  {
            SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
            if (pLeg != null) {
                rst = LocalCache.getDomainValueComment(DSConnection.getDefault(), EmirSnapshotReduxConstants.DV_EMIR_CALCULATION_BASIS, pLeg.getDayCount().toString());
                if (Util.isEmpty(rst)) {
                    rst = pLeg.getDayCount().toString();
                }
            }
        }

        if (Util.isEmpty(rst)) {
            rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        }

        return rst;
    }

}
