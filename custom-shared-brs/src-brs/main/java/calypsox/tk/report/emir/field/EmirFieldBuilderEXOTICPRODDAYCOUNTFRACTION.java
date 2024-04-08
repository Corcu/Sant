
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderEXOTICPRODDAYCOUNTFRACTION implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        //Fijar su valor con el valor del campo "Daycount" de la pestaña Coupons de la pata del bono en la operación

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
        if (pLeg != null) {
            rst = pLeg.getDayCount().toString();
        }

        return rst;
    }
}
