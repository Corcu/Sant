
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCOMPRESSEDTRADE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return Boolean.FALSE.toString().toLowerCase();
    }
}
