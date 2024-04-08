package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.SwapLeg;

public class EmirFieldBuilderPAYFREQMULTLEG1 implements EmirFieldBuilder {

  private static final int IDX_TENOR = 0;

  @Override
  public String getValue(Trade trade) {
    /*
    "Si esta informado el campo FIXRATE esta informado:
    Vamos a la pata de financiación a revisar la frecuencia de pago.
    En este campo indicar el tipo de periodo (( en los que se ha definido el calculo de intereres.

            Si no esta informado el campo FIXRATE se informan a vacio."
    */

    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
    if (pLeg != null) {
      rst = EmirFieldBuilderUtil.getInstance().getMappedValueCouponFrequency(pLeg.getCouponFrequency().toString(), IDX_TENOR);
    }
    return rst;
  }
}
