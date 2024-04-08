
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCOLLATERALIZEDPARTY2 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        //El valor de este campo se obtiene del grado de colateralización que tiene definido el contrato de colaterales asociado a la contrapartida. Este campo aparece en la pestaña de Additional Info del contrato.
        //Este campos se debe revisar en cada envio.
        //Revisar el listado de valores permitidos en DTCC y los valores del campo en Calypso por si hubiera que hacer conversión.
        //Si no tiene valor definido o no tiene contrato de colateral asociado fijar el valor como "UNCOLLATERALIZED"

        return EmirFieldBuilderUtil.getInstance().getLogicCollateralized(trade);
    }
}
