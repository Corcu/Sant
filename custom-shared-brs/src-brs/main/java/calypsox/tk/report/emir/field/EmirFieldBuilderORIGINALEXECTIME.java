package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EmirFieldBuilderORIGINALEXECTIME implements EmirFieldBuilder {

  @Override
  public String getValue(Trade trade) {
    String rst = null;

    final SimpleDateFormat sdfUTC = new SimpleDateFormat(EmirSnapshotReduxConstants.UTC_DATE_FORMAT, Locale.getDefault());
    // Indicamos que la zona horaria es UTC
    sdfUTC.setTimeZone(TimeZone.getTimeZone(EmirSnapshotReduxConstants.TIMEZONE_UTC));

    rst = sdfUTC.format(trade.getUpdatedTime());

    return rst;
  }

}
