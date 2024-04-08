
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderINIPAYMENTCURRENCY implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        Fee upFront = EmirFieldBuilderUtil.getInstance().getPremiumFee(trade);
        if (upFront != null) {
            return upFront.getCurrency();
        }
        return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}

