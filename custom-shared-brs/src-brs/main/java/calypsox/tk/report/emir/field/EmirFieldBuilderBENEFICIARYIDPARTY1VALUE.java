
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LegalEntityAttribute;

public class EmirFieldBuilderBENEFICIARYIDPARTY1VALUE
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        // Se enviará en todos los casos el valor del atributo "LEI" asociado a la PO de la operación
        String value  = EmirSnapshotReduxConstants.EMPTY_SPACE;

        int leId = trade.getBook().getLegalEntity().getId();

        LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
                .getInstance().getAttribute(0, leId,
                        LegalEntityAttributesCache.ALL_ROLES,
                        KeywordConstantsUtil.LE_ATTRIBUTE_LEI);
        if (leiAttribute != null) {
            value = leiAttribute.getAttributeValue();
        }

        return value;
    }
}
