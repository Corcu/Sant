
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EmirFieldBuilderUNADJUSTEDDATE2 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        String rst = null;

        JDate date = trade.getMaturityDate();

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());

        if (date != null) {
            rst = sdf.format(date.getDate());
        }

        return rst;
    }
}
