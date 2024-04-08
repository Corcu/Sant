
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTY2THIPARTYVIEWIDTYPE implements EmirFieldBuilder {
  @Override
  public String getValue(Trade trade) {

      /*
      "Nueva logica
       La contrapartida tiene un atributo especifico (EMIR_ThirdParty) que la marca como que opera a traves de un intermediario (atributo pte de definir que indicarira el GLCS del intermediario).
       Con este dato iremos a buscar el valor del LEI o SWIFTBIC del intermediario.
       Si tiene LEI se indica el literal ""LEI"" Si no tiene LEI, revisamos si tiene BIC que esta informado a nivel de contacto de tipo SWIFT (Swift, Swif_CLS o Swift_NOCLS). Si lo tiene, se informa con el literal ""SWIFTBIC"".
       Si no tiene contacto de tipo Swift entonces se genera a vacío y no se incluye en el reporte"

       */
      String rst = EmirFieldBuilderUtil.getInstance().getTRADEPARTY2THIPARTYVIEWIDTYPELogic(trade);




      return  rst;
  }


}
