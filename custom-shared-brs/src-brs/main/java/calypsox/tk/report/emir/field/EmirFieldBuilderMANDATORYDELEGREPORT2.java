
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;

public class EmirFieldBuilderMANDATORYDELEGREPORT2 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String result = EmirSnapshotReduxConstants.EMPTY_SPACE;

        final String attr = EmirFieldBuilderUtil.getInstance().getLegalEntityAttribute(trade,
                KeywordConstantsUtil.LE_ATTRIBUTE_EMIR_CPTY_CLASS, true);

        if (!Util.isEmpty(attr)
                && "NFC".equals(attr) ) {
            result = "YES";
        }

        return result;
    }
}
