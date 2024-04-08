package calypsox.tk.confirmation.handler;

import com.calypso.tk.core.Trade;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalypsoConfirmationMifidHandler {

    /*
     * Trade keywords
     */
    private final String MIFID_MARKUP="MIFID_MARKUP";
    private final String MIFID_SALESCREDIT="MIFID_SALESCREDIT";
    private final String MIFID_CVA="MIFID_CVA";
    private final String MIFID_LVA="MIFID_LVA";
    private final String MIFID_ADDCHARGE="MIFID_ADDCHARGE";
    private final String MIFID_COSTS_CURR="MifidCostsExpensesCurr";

    private final String currentMifidCostsExpensesAmount;
    private final String currentMifidCostsExpensesCurr;

    public CalypsoConfirmationMifidHandler(Trade trade){
        this.currentMifidCostsExpensesAmount=buildMifidCostsExpensesAmount(trade);
        this.currentMifidCostsExpensesCurr=buildMifidCostsExpensesCurr(trade);
    }

    private String buildMifidCostsExpensesAmount(Trade trade) {
        double markup= Optional.ofNullable(trade)
                .map(t -> t.getKeywordAsDouble(MIFID_MARKUP)).orElse(0.0D);
        double salesCredit= Optional.ofNullable(trade)
                .map(t -> t.getKeywordAsDouble(MIFID_SALESCREDIT)).orElse(0.0D);
        double cva= Optional.ofNullable(trade)
                .map(t -> t.getKeywordAsDouble(MIFID_CVA)).orElse(0.0D);
        double lva= Optional.ofNullable(trade)
                .map(t -> t.getKeywordAsDouble(MIFID_LVA)).orElse(0.0D);
        double addCharge= Optional.ofNullable(trade)
                .map(t -> t.getKeywordAsDouble(MIFID_ADDCHARGE)).orElse(0.0D);
        return formatNumber(markup+salesCredit+cva+lva+addCharge);
    }

    private String buildMifidCostsExpensesCurr(Trade trade) {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue(MIFID_COSTS_CURR)).orElse("");
    }

    private String formatNumber(Double number) {
        return String.format(Locale.ENGLISH, "%.6f", number);
    }

    public String getCurrentMifidCostsExpensesAmount() {
        return currentMifidCostsExpensesAmount;
    }

    public String getCurrentMifidCostsExpensesCurr() {
        return currentMifidCostsExpensesCurr;
    }
}
