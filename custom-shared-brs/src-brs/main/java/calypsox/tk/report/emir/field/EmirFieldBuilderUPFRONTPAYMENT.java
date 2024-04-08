
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Trade;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

public class EmirFieldBuilderUPFRONTPAYMENT implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}
