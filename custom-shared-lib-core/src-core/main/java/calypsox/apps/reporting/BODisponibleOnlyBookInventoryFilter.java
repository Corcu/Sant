package calypsox.apps.reporting;

import com.calypso.apps.reporting.InventoryFilter;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.JDate;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.inventoryposition.AggregationKey;

import java.util.*;

public class BODisponibleOnlyBookInventoryFilter implements InventoryFilter {
    @Override
    public boolean accept(Inventory inv) {
        List<String> excludeBooksList = DomainValues.values(BODisponiblePositionFilter.BOOKS_FILTER);
        return Optional.ofNullable(inv).map(Inventory::getBook)
                .filter(book -> !excludeBooksList.contains(book.getName())).isPresent();
    }

    @Override
    public boolean accept(BOTransfer transfer) {
        return true;
    }

    @Override
    public void prepareComplexFilter(Map<AggregationKey, HashMap<JDate, Vector<Inventory>>> aggregationMap, BOPositionReportTemplate.BOPositionReportTemplateContext context) {

    }

    @Override
    public boolean checkComplexFilter(BOPositionReport.ReportRowKey rowKey) {
        return false;
    }
}
