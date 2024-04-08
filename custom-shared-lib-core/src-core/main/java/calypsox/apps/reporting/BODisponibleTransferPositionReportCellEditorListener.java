package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportRow;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import java.util.Optional;

public class BODisponibleTransferPositionReportCellEditorListener implements CellEditorListener {

    ReportPanel panel;

    public BODisponibleTransferPositionReportCellEditorListener(ReportPanel panel) {
        this.panel = panel;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        DefaultCellEditor fromToCellEditor = Optional.ofNullable(e.getSource()).filter(DefaultCellEditor.class::isInstance).map(DefaultCellEditor.class::cast).orElse(null);
        if(null!=panel && null!=fromToCellEditor){
            ReportRow[] selectedReportRows = panel.getSelectedReportRows();
            if(!Util.isEmpty(selectedReportRows)){
                String selectedOption = Optional.ofNullable(fromToCellEditor.getCellEditorValue()).map(String::valueOf).orElse("");
                ReportRow selectedReportRow = selectedReportRows[0];
                selectedReportRow.setProperty("SelectedOption", selectedOption);
            }
        }
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
    }
}
