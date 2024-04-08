
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderPRODUCTIDENTIFICATIONTYPE
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        /*
        Si la keyword EMIR_MIC_CODE esta informada con el valor "XOFF" o n valor diferente a "XXXX", se reporta el campo con el valor "I"
        Si la keyword EMIR_MIC_CODE esta informada con el valor "XXXX", se reporta el campo a vacío y no se incluye en el report.
        Misma logica FX

        */
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String micCode = trade.getKeywordValue(
                EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE);
        if ((!EmirSnapshotReduxConstants.XXXX.equals(micCode) || EmirSnapshotReduxConstants.XOFF.equals(micCode))
                && (micCode != null && !EmirSnapshotReduxConstants.EMPTY_SPACE.equals(micCode.trim()))) {
            // CR v15.1.0
            rst = EmirSnapshotReduxConstants.LITERAL_I;
        }
       return rst;

    }
}
