
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.FXNDF;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EmirFieldBuilderSETTLFIXINGDATE implements EmirFieldBuilder {
  @Override
  public String getValue(Trade trade) {
    // CAL_76_ (HD0000007092450)
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
        Locale.getDefault());
    final Product product = trade.getProduct();
    JDate date = null;
    if (product instanceof FXNDF) {
      final FXNDF productFXNDF = (FXNDF) trade.getProduct();
      date = productFXNDF.getResetDateTime().getJDate(TimeZone.getDefault());

      if (date == null) {
        Log.info(this, "Could not get Reset Date from FXNDF Trade id: "
            + trade.getLongId());
      }
    }
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    if (date != null) {
      rst = sdf.format(date.getJDatetime(TimeZone.getDefault()));
    }

    return rst;
  }
}
