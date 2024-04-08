
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwapLeg;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EmirFieldBuilderUNADJUSTEDDATE1 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
        if (pLeg != null) {
            JDate date = pLeg.getStartDate();
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault());
            if (date != null) {
                rst = sdf.format(date.getDate());
            }

        }

        return rst;
    }
}
