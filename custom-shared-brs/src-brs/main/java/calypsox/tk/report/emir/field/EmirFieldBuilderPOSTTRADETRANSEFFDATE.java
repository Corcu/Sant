
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EmirFieldBuilderPOSTTRADETRANSEFFDATE
implements EmirFieldBuilder {
  @Override
  public String getValue(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    final String actionType = EmirFieldBuilderUtil.getInstance()
        .getLogicActionType(trade);
    if (EmirSnapshotReduxConstants.ACTION_C.equalsIgnoreCase(actionType)
        || EmirSnapshotReduxConstants.ACTION_E.equalsIgnoreCase(actionType)) {

      final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
          Locale.getDefault());
      JDate date = null;

      // Drop 3 - Agreement date should be always the date of the event
      final JDatetime valDatetime = trade.getUpdatedTime();
      if (valDatetime != null) {
        date = valDatetime.getJDate(TimeZone.getDefault());
      }

      if (date != null) {
        rst = sdf.format(date.getDate(TimeZone.getDefault()));
      }

    }
    return rst;
  }
}
