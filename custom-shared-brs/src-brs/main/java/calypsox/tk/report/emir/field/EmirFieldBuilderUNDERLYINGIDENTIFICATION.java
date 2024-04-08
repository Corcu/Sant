
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderUNDERLYINGIDENTIFICATION
        implements EmirFieldBuilder {

    private static final String ISIN = "ISIN";

    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String underType =  EmirFieldBuilderUtil.getInstance().getUnderlyingIdentificationType(trade);

        if (ISIN.equalsIgnoreCase(underType)) {
            if (trade.getProduct()  instanceof PerformanceSwap) {
                PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
                PerformanceSwapLeg pLeg = (PerformanceSwapLeg)perfSwap.getPrimaryLeg();
                if (pLeg == null || !(pLeg.getReferenceProduct() instanceof Bond)) {
                    pLeg = (PerformanceSwapLeg)perfSwap.getSecondaryLeg();
                }
                if (pLeg != null && pLeg.getReferenceProduct() instanceof Bond) {
                    rst =   pLeg.getReferenceProduct().getSecCode(ISIN);
                }
            }
        }
        return rst;
    }
}
