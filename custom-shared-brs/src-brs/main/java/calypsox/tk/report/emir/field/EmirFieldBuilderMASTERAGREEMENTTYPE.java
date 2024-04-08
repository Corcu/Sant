
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.MasterLegalAgreementsCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LegalAgreement;

public class EmirFieldBuilderMASTERAGREEMENTTYPE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        final LegalAgreement la = MasterLegalAgreementsCache.getInstance()
                .getMasterLegalAgreement(trade);

        if (la != null) {
            rst = la.getAuthName();
        }

        return rst;
    }
}
