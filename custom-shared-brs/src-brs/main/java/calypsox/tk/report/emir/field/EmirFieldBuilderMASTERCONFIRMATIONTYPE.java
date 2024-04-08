
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderMASTERCONFIRMATIONTYPE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        /*
        Se obtiene el valor según la logica establecida a partir del domain value EMIR_Documentation_Type
        Si el bono de la operación esta definido como SingleAsset entonces informamos este campo con el valor "StandardTermsSupplementType".
        Si el bono esta definico como Managed entonces lo informamos como CreditDerivativesPhysicalSettlementMatrix
         */

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        PerformanceSwapLeg pLEg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
        if (pLEg != null) {
            if (pLEg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_SINGLE_ASSET)) {
                rst =  EmirSnapshotReduxConstants.STANDARD_TERMS_SUPPLEMENT_TYPE;
            }
            else if (pLEg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_MANAGED)) {
                rst = EmirSnapshotReduxConstants.CREDIT_DERIVATIVES_PHYSYCAL_SETTLEMENT_MATRIX;
            }
        }
        return rst;
    }
}
