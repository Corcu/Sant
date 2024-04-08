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

public class BODisponiblePositionInventoryFilter implements InventoryFilter {

    @Override
    public boolean accept(Inventory inv) {
        List<String> excludeAccountList = DomainValues.values("BODisponibleExcludeAccounts");
        List<String> excludeIsinList = DomainValues.values("BODisponibleExcludeIsin");
        List<String> excludeBooksList = DomainValues.values("BODisponibleExcludeBooks");

        return Optional.ofNullable(inv)
                .filter(position -> null!=position.getAccount() && !excludeAccountList.contains(position.getAccount().getName()))
                .filter(position -> null!=position.getProduct() && !excludeIsinList.contains(position.getProduct().getSecCode("ISIN")))
                .filter(position -> null!=position.getBook() && !excludeBooksList.contains(position.getBook().getName())).isPresent();
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
