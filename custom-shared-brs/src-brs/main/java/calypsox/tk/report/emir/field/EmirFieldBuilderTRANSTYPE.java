
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.TransactionTypeValue;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Arrays;
import java.util.List;

public class EmirFieldBuilderTRANSTYPE implements EmirFieldBuilder {

    private static enum FieldTransTypeValue {
        Trade, Exit
    };

    @Override
    public String getValue(Trade trade) {
        final TransactionTypeValue[] transTypeValues = TransactionTypeValue
                .values();
        final List<TransactionTypeValue> listTransactionTypeValue = Arrays
                .asList(transTypeValues);

        String logicTransType = trade.getKeywordValue(
                EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE);

        if (!Util.isEmpty(logicTransType)) {
            final TransactionTypeValue transTypeValue = getTransTypeValue(
                    logicTransType);

            if (transTypeValue != null
                    && !Util.isEmpty(listTransactionTypeValue)) {
                final int index = listTransactionTypeValue
                        .indexOf(transTypeValue);
                if (index > -1 && index < FieldTransTypeValue.values().length) {
                    logicTransType = FieldTransTypeValue.values()[index].name();
                }
            }
        }

        return logicTransType;
    }

    private TransactionTypeValue getTransTypeValue(
            final String logicTransType) {
        TransactionTypeValue transTypeValue = null;
        try {
            transTypeValue = TransactionTypeValue.valueOf(logicTransType);
        } catch (Exception e) {
            Log.error(this,
                    String.format("Error getting the TransactionTypeValue %s",
                            logicTransType),
                    e);
        }
        return transTypeValue;
    }
}
