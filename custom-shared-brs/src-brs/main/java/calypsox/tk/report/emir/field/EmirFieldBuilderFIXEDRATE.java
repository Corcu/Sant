package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxReportLogic;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.SwapLeg;

import java.math.BigDecimal;

public class EmirFieldBuilderFIXEDRATE implements EmirFieldBuilder {

  /*
  "Nueva logica
  Se revisa en la pata de financiación
  Si el swap es flotante, enviamos el valor del campo ""1st Rate"" dividido entre 100. Si no tiene valor el campo ""1st Rate"", enviamos cero.
  Si es fijo, se coge el valor del tipo fijo de la pata de financiación y se divide entre 100. (formato ejemplo, se informará 0.03 para un rate con valor 3 en Calypso)"
  */

  @Override
  public String getValue(Trade trade) {

    String rst = EmirFieldBuilderUtil.getInstance().getlogicFIXRATE(trade);
    return rst;
  }

}
