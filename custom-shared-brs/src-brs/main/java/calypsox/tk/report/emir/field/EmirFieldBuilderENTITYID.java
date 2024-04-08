
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.proxy.datatypes.impl.LegalEntityCalypsoDataType;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class EmirFieldBuilderENTITYID implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        /*
        Codigo EMIR Issuer RED code que esta asociado a la entidad que emite el bono.
                Se va a dar de alta un nuevo atributo a nivel de contrapartida llamado EMIR_ISSUER_RED_CODE.
        A partir de la
        */
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        if (trade.getProduct() instanceof PerformanceSwap) {

            PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
            if (pLeg != null) {
                if (pLeg.getReferenceProduct() instanceof Bond) {
                    Bond bond = (Bond) pLeg.getReferenceProduct();
                    int leId = bond.getIssuerId();

                    LegalEntityAttribute leiValue = LegalEntityAttributesCache.getInstance()
                            .getAttribute(leId, leId, "ALL",  EmirSnapshotReduxConstants.LEI);
                    if (leiValue != null
                            && !Util.isEmpty(leiValue.getAttributeValue())) {
                        rst = leiValue.getAttributeValue();
                    } else  {
                        leiValue = LegalEntityAttributesCache.getInstance()
                                .getAttribute(leId, leId, "ALL",
                                        EmirSnapshotReduxConstants.LE_ATTR_EMIR_ISSUER_RED_CODE);
                        if (leiValue != null
                                && !Util.isEmpty(leiValue.getAttributeValue())) {
                            rst = leiValue.getAttributeValue();
                        }
                    }
                }
            }
        }

        if (Util.isEmpty(rst)) {
            rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        }

        return rst;
    }
}
