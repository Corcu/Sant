package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderINTRAGROUP implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
       /* Nueva logica
        Si la contrapartida tiene el atributo INTRAGROUP con valor "NO" entonces informamos el valor "false" en este campo.
         En caso de que el atributo INTRAGROUP con valor "YES" entonces informamos el valor "true" en este campo.

        */
        final String intraGroup = LegalEntityAttributesCache.getInstance()
                .getAttributeValue(trade, EmirSnapshotReduxConstants.LE_ATTRIBUTE_INTRAGRUPO, true);


        if (!Util.isEmpty(intraGroup) && !"NO".equalsIgnoreCase(intraGroup)) {
            return Boolean.TRUE.toString();
        }

        return Boolean.FALSE.toString();
    }
}
