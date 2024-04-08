
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EmirFieldBuilderVALUEDATE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        rst = sdf.format(trade.getSettleDate().getDate(TimeZone.getDefault()));

        return rst;
    }
}
