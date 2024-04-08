
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.PerformanceSwap;

public class EmirFieldBuilderPRODUCTVALUE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        if (trade.getProduct() instanceof PerformanceSwap) {
            return EmirSnapshotReduxConstants.CREDIT_TOTAL_RETURN_SWAP;
        }
        return rst;
    }
}
