
package calypsox.tk.report.emir.field;

import calypsox.tk.core.SantanderUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxUtil;
import calypsox.tk.util.emir.LegalEntitiesCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Basket;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class EmirFieldBuilderINDEXFACTOR implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
        if (pLeg != null) {
            if (!pLeg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_SINGLE_ASSET)) {
                if (pLeg.getReferenceProduct() instanceof  Basket) {
                    Basket basket = (Basket) pLeg.getReferenceProduct();
                    if (basket != null) {
                        basket.getBasketComponents().size();
                    }
                }
            }
        }

        return rst;
    }
}
