package calypsox.tk.report.generic.loader;

import calypsox.tk.report.AccountingNotificationEntry;
import calypsox.tk.report.AccountingNotificationEntryBuilder;
import com.calypso.tk.core.JDate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.TradeArray;

import java.util.List;
/**
 * @author acd
 */
public class AccountingNotificationLoader extends SantInterestNotificationLoader{

    public List<AccountingNotificationEntry> loadAccounting(final ReportTemplate template, final JDate processStartDate,
                                                            final JDate processEndDate, final JDate valDate) throws Exception {

        final TradeArray trades = loadTrades(template, processStartDate, processEndDate);

        final AccountingNotificationEntryBuilder builder = new AccountingNotificationEntryBuilder();
        builder.build(trades, processStartDate, processEndDate, valDate, template);
        return builder.getAccountingEntries();
    }


}
