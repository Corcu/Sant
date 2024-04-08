package calypsox.apps.reporting;

import com.calypso.apps.reporting.InventoryFilter;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.JDate;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.inventoryposition.AggregationKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

public class BODisponibleBondInventoryFilter implements InventoryFilter {
    @Override
    public boolean accept(Inventory inv) {
        return Optional.ofNullable(inv).map(Inventory::getProduct)
                .filter(product -> product instanceof Bond || "Bond".equalsIgnoreCase(product.getProductFamily()))
                .isPresent();
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
