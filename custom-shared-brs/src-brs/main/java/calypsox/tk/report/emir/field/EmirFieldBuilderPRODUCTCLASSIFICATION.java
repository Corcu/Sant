
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxUtil;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;


public class EmirFieldBuilderPRODUCTCLASSIFICATION implements EmirFieldBuilder {
  /*
     Hay que informar un valor con 6 caracteres.Algunos de esos caracteres seran siempre fijos y otros variaran según el tipo de bono.
             La logica para construir el valor es la siguiente:
     Caracter 1: siempre valor "S"
     Caracter 2: siempre valor "C"
     Caracter 3: Este caracter se obtiene de ir a mirar si la pata del bono es managed o SingleAsset. Si es SingleAsset indicamos una "U".
                 Si es managed informamos "I".
     Caracter 4: siempre valor "T"

     Caracter 5: Si el caracter 3 esta informado con el valor "I" el caracter 5 se informará con el valor "L".
                   En caso de que el caracter 3 sea "U"  entonces habrá que ir a la definición del bono para obtener el
                   tipo de bono (campo bond type). Con ese valor, revisar si esta en el domainvalue
                   "EMIR_Sovereing_Bond", en ese caso es soberano y se fija el caracter con valor "S",
                   En caso contrario es corporativo  se fija el caracter con valor "C"
     ver si tiene un atributo que indentifique si es Corporate o Sovereing
     Caracter 6: siempre valor "C"

     Un ejemplo de como quedaria seria: SCITSC
     */
  @Override
  public String getValue(Trade trade) {
    String rst = EmirSnapshotReduxConstants.SCXTXX;
    String pos3 = "X";
    String pos5 = "X";

    if (trade.getProduct()  instanceof PerformanceSwap) {
      PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
      if (pLeg != null) {
          Product bond = (Product) pLeg.getReferenceProduct();
          Vector<String> sovereignTypes =  LocalCache.getDomainValues(DSConnection.getDefault(),
                  EmirSnapshotReduxConstants.DV_EMIR_SOVEREIGN_BOND);
          // pos3
          if (pLeg.getLegConfig().equalsIgnoreCase("SingleAsset")) {
            pos3 = EmirSnapshotReduxConstants.LITERAL_U;
          } else {
            pos3 = EmirSnapshotReduxConstants.LITERAL_I;
          }

          // pos5
          if (pos3.equalsIgnoreCase(EmirSnapshotReduxConstants.LITERAL_I)) {
            pos5 = EmirSnapshotReduxConstants.LITERAL_L;
          } else  {
            if (!Util.isEmpty(sovereignTypes)
                        && sovereignTypes.contains(bond.getType())) {
              pos5 = EmirSnapshotReduxConstants.LITERAL_S;
            } else {
              pos5 = EmirSnapshotReduxConstants.LITERAL_C;
            }
          }
          rst = "SC"+pos3+"T"+pos5+"C";
        }
      }
     return rst;
  }

  private PerformanceSwapLeg getPerformanceSwapBondLeg(Trade trade) {

    PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
    PerformanceSwapLeg pLeg = (PerformanceSwapLeg)perfSwap.getPrimaryLeg();
    if (pLeg == null || !(pLeg.getReferenceProduct() instanceof Bond)) {
      pLeg = (PerformanceSwapLeg)perfSwap.getSecondaryLeg();
    }

    if (pLeg != null && pLeg.getReferenceProduct() instanceof Bond) {
      return pLeg;
    }

    return null;
  }
}