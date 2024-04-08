package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderFLOATINGRATE implements EmirFieldBuilder {

  /*
  "Nueva logica
  Se revisa en la pata de financiación
  Si el swap es flotante, enviamos el valor del campo ""1st Rate"" dividido entre 100. Si no tiene valor el campo ""1st Rate"", enviamos cero.
  Si es fijo, se coge el valor del tipo fijo de la pata de financiación y se divide entre 100. (formato ejemplo, se informará 0.03 para un rate con valor 3 en Calypso)"
  */

  @Override
  public String getValue(Trade trade) {

    String rst = EmirFieldBuilderUtil.getInstance().getlogicFLOATINGRATE(trade);
    return rst;
  }

}
