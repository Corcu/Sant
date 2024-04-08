
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderMASTERAGREEMENTVERSION
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        final String laDate = EmirFieldBuilderUtil.getInstance()
                .getLogicMasterAgreementDate(trade);
        // The version is the year of the master agreement date
        if (!laDate.isEmpty()) {
            rst = laDate.substring(0, 4);
        }

        return rst;
    }
}
