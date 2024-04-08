package calypsox.apps.reporting;

import com.calypso.apps.reporting.InventoryFilter;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.inventoryposition.AggregationKey;
import com.calypso.tk.service.DSConnection;

import java.util.*;

import static calypsox.apps.reporting.BODisponiblePositionFilter.ACCOUNT_FILTER;
import static calypsox.apps.reporting.BODisponiblePositionFilter.BOOKS_FILTER;
/**
 * @author acd
 */
public class BODisponibleTransferFilter implements InventoryFilter {
    private List<Account> accountsCache = new ArrayList<>();
    private List<Book> booksCache = new ArrayList<>();

    @Override
    public boolean accept(Inventory inv) {
        return true;
    }

    @Override
    public boolean accept(BOTransfer transfer) {
        if(null!=transfer){
            return acceptBook(transfer);
        }
        return true;
    }

    @Override
    public void prepareComplexFilter(Map<AggregationKey, HashMap<JDate, Vector<Inventory>>> aggregationMap, BOPositionReportTemplate.BOPositionReportTemplateContext context) {

    }

    @Override
    public boolean checkComplexFilter(BOPositionReport.ReportRowKey rowKey) {
        return false;
    }

    private boolean acceptBook(BOTransfer transfer){
        List<String> excludeBooksList = DomainValues.values(BOOKS_FILTER);
        if(!Util.isEmpty(excludeBooksList)){
            if(Util.isEmpty(booksCache)){
                booksCache = BOCache.getBooksFromBookNames(DSConnection.getDefault(), new Vector<>(excludeBooksList));
            }
            return booksCache.parallelStream().map(Book::getId).noneMatch(b -> b == transfer.getBookId());
        }
        return true;
    }

    private boolean acceptAccount(BOTransfer transfer){
        List<String> excludeAccountList = DomainValues.values(ACCOUNT_FILTER);
        if(!Util.isEmpty(excludeAccountList)){
            if(Util.isEmpty(accountsCache)){
                try {
                    ArrayList<CalypsoBindVariable> accountsBindVariable = new ArrayList<>();
                    StringBuilder strBld = new StringBuilder();
                    for (String accountName : excludeAccountList){
                        accountsBindVariable.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, accountName));
                        strBld.append("?,");
                    }
                    accountsCache = DSConnection.getDefault().getRemoteAccounting().getAccounts(" ACC_ACCOUNT_NAME IN (" + strBld.substring(0, strBld.length() - 1) + ")", accountsBindVariable);
                } catch (CalypsoServiceException e) {
                    throw new RuntimeException(e);
                }
            }
            return accountsCache.parallelStream().map(Account::getId).noneMatch(t -> t == transfer.getGLAccountNumber());
        }
        return true;
    }
}
