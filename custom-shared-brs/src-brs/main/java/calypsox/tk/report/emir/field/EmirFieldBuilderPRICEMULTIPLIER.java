
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderPRICEMULTIPLIER implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        // Return "1";
        return Integer.toString(1);
    }
}
