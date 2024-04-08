package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderCLEARINGIND implements EmirFieldBuilder {

  @Override
  public String getValue(Trade trade) {
    // Si el valor de EXECUTIONVENUEMICCODE es XXXX o vacio informamos el valor "ESMA-X".
    // Si el valor del EXECUTIONVENUEMICCODE es XOFF se informa el valor "ESMA-N".
    // Si tiene otro valor el campo EXECUTIONVENUEMICCODE, se informa a vacio este campo.

    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    String execVenue = EmirFieldBuilderUtil.getInstance().getLogicExecutionVenueMicCode(trade);
    if (Util.isEmpty(execVenue) || EmirSnapshotReduxConstants.XXXX.equalsIgnoreCase(execVenue)) {
      rst = EmirSnapshotReduxConstants.ESMA_X;
    } else  if (EmirSnapshotReduxConstants.XOFF.equalsIgnoreCase(execVenue)) {
      rst = EmirSnapshotReduxConstants.ESMA_N;
    }
    return rst;
  }

}
