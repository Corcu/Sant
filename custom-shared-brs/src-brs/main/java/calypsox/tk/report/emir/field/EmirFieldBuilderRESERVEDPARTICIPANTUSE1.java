
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderRESERVEDPARTICIPANTUSE1 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        // Enviar el codigo GLCS (campo Short Name a nivel de definición LE) de la contrapartida
        return trade.getCounterParty().getCode();
    }
}
