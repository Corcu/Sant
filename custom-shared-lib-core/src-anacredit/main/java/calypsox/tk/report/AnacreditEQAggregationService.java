package calypsox.tk.report;

import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.EquityTypeIdentifier;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class AnacreditEQAggregationService  {

    public List<ReportRow> filterEQNOTforIsinWithPositions(List<ReportRow> inventoryRows) {
        List<ReportRow> result = new ArrayList<>();
        ConcurrentHashMap<String, Double> totalPerIsin = new ConcurrentHashMap<>();
        inventoryRows.stream().parallel().forEach(reportRow -> {
            InventorySecurityPosition pos = reportRow.getProperty(ReportRow.INVENTORY);
            if (pos == null) {
                return;
            }
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(pos);
            totalPerIsin.computeIfPresent(identifier.getISIN(), (k,v) ->  v + identifier.getNominal());
            totalPerIsin.computeIfAbsent(identifier.getISIN(), v -> identifier.getNominal());
        });

        inventoryRows.stream().forEach(reportRow -> {

            InventorySecurityPosition pos = reportRow.getProperty(ReportRow.INVENTORY);
            if (pos == null) {
                return;
            }
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(pos);
            if (identifier.isEQNOT()
                    && null != totalPerIsin.get(identifier.getISIN())
                    && 0.00d != totalPerIsin.get(identifier.getISIN())) {
                return;
            }
            else {
                result.add(reportRow);
            }
        });

        return result;

    }

    public List<ReportRow> sumEQDESPositionSameISIN(List<ReportRow> inventoryRows) {
        List<ReportRow> result = new ArrayList<>();
        HashMap<String, ReportRow> totalEQDDES = new HashMap<String, ReportRow>();

        inventoryRows.stream().forEach(reportRow -> {
            InventorySecurityPosition pos = reportRow.getProperty(ReportRow.INVENTORY);
            if (pos == null) {
                return;
            }
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(pos);
            if (!identifier.isEQDES()) {
                result.add(reportRow);
            }
            else if (!totalEQDDES.containsKey(identifier.getISIN())) {
                    totalEQDDES.put(identifier.getISIN(), reportRow);
            }
            else  {

                ReportRow summaryRow = totalEQDDES.get(identifier.getISIN());
                InventorySecurityPosition summaryInventoryPos = reportRow.getProperty(ReportRow.INVENTORY);
                summaryInventoryPos.addTotalPosition(pos);
                summaryInventoryPos.addDailyPositionToTotal(pos);
                totalEQDDES.put(identifier.getISIN(), summaryRow);

            }
        });
        result.addAll(totalEQDDES.values());
        return result;

    }

    public List<ReportRow> doAggregation(List<ReportRow> source, Vector<String> errorMsgs) {
        HashMap<String, ReportRow> _aggregatedPos = new HashMap<>();
        source.stream().forEach(reportRow -> {
            InventorySecurityPosition pos = reportRow.getProperty(ReportRow.INVENTORY);
            if (pos == null) {
                return;
            }
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(pos);
            String key = buildKey(identifier);
            reportRow.setProperty(AnacreditEQPositionReport.PROPERTY_AGGREGO, key);
            if (!_aggregatedPos.containsKey(key)) {
                _aggregatedPos.put(key, reportRow);
                return;
            }
            ReportRow sourceRow = _aggregatedPos.get(key);

            InventorySecurityPosition storedPos = sourceRow.getProperty(ReportRow.INVENTORY);
            storedPos.addTotalPosition(pos);
            sourceRow.setProperty(ReportRow.INVENTORY, storedPos);
            _aggregatedPos.put(key,sourceRow);

        });
        return new ArrayList<ReportRow>(_aggregatedPos.values());

    }

    private String buildKey(EquityTypeIdentifier identifier) {
        String entidad = AnacreditMapper.getEntidadDepositaria(identifier);
        String  tipo_cartera_irfs9 = AnacreditMapper.getTipoCartera(identifier.getBook());
        StringBuilder sb = new StringBuilder();
        sb.append(identifier.getBook().getLegalEntity().getCode());
        sb.append(identifier.getISIN());
        sb.append(identifier.getCcy());
        sb.append(tipo_cartera_irfs9);
        sb.append(entidad);
        return sb.toString();
    }

}
