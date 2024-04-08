package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderMATRIXTYPE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        PerformanceSwapLeg pLEg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
        if (pLEg != null) {
            if (pLEg.getLegConfig().equalsIgnoreCase("SingleAsset")) {
                rst =  EmirSnapshotReduxConstants.STANDARD_TERMS_SUPPLEMENT_TYPE;
            }
            else if (pLEg.getLegConfig().equalsIgnoreCase("Managed")) {
                rst = EmirSnapshotReduxConstants.CREDIT_DERIVATIVES_PHYSYCAL_SETTLEMENT_MATRIX;
            }
        }
        return rst;
    }
}
