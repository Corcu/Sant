
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EmirFieldBuilderSINGPAYDATE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst =  EmirSnapshotReduxConstants.EMPTY_SPACE;
        Fee upFront = EmirFieldBuilderUtil.getInstance().getPremiumFee(trade);
        if (upFront != null) {

            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault());
            if (upFront.getFeeDate() != null) {
                rst = sdf.format(upFront.getFeeDate().getDate());
            }
            return upFront.getCurrency();
        }
        return rst;
    }
}

