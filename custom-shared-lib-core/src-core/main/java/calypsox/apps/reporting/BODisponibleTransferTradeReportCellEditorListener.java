package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.util.CalypsoCellEditor;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import java.util.Arrays;
import java.util.Optional;

public class BODisponibleTransferTradeReportCellEditorListener  implements CellEditorListener {
    ReportPanel panel;

    public BODisponibleTransferTradeReportCellEditorListener(ReportPanel panel) {
        this.panel = panel;
    }

    /**
     * This tells the listeners the editor has ended editing
     *
     * @param e
     */
    @Override
    public void editingStopped(ChangeEvent e) {
        CalypsoCellEditor fromToCellEditor = Optional.ofNullable(e.getSource()).filter(CalypsoCellEditor.class::isInstance).map(CalypsoCellEditor.class::cast).orElse(null);
        if(fromToCellEditor!=null){
            String columnName = getColumnName(fromToCellEditor.getColumn());
            Trade selectedTrade = getSelectedTrade();
            String columnValue = Optional.of(fromToCellEditor).map(CalypsoCellEditor::getCellEditorValue).map(String::valueOf).orElse("");
            if("Quantity".equalsIgnoreCase(columnName)){
                if(columnValue.contains(",")){
                    columnValue = columnValue.replace(",",".");
                }
                Double value = Optional.of(columnValue).map(Double::parseDouble).orElse(0.0);
                updateTradeQuantity(selectedTrade,value);
            }else if("TRADE_KEYWORD.NIFBeneficiario".equalsIgnoreCase(columnName)){
                updateTradeKeyword(selectedTrade,columnName,columnValue);
            }else if("TRADE_KEYWORD.DescripcionBeneficiario".equalsIgnoreCase(columnName)){
                updateTradeKeyword(selectedTrade,columnName,columnValue);
            }
        }

    }

    protected void updateTradeKeyword(Trade trade, String kwName, String kwValue){
        Optional.ofNullable(trade).ifPresent(t -> {
            String finalkwName = kwName;
            if(kwName.contains("TRADE_KEYWORD.")){
                finalkwName = kwName.replaceAll("TRADE_KEYWORD.","");
            }
            t.addKeyword(finalkwName,kwValue);
        });
    }
    protected void updateTradeQuantity(Trade trade, double quantity){
        Optional.ofNullable(trade).ifPresent(t -> {
            t.setQuantity(quantity);
        });
    }

    protected Trade getSelectedTrade(){
        if(null!=panel){
            ReportRow reportRow = Arrays.stream(panel.getSelectedReportRows()).findFirst().orElse(new ReportRow(null));
            return Optional.ofNullable(reportRow.getProperty("Trade")).filter(Trade.class::isInstance).map(Trade.class::cast).orElse(null);
        }
        return null;
    }

    private String getColumnName(int columnId){
           return Optional.ofNullable(panel).map(p -> p.getTemplate().getColumn(columnId)).orElse("");
    }

    /**
     * This tells the listeners the editor has canceled editing
     *
     * @param e
     */
    @Override
    public void editingCanceled(ChangeEvent e) {

    }
}
