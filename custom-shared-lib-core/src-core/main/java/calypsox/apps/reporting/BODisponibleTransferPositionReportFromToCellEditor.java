package calypsox.apps.reporting;


import com.calypso.apps.reporting.ReportPanel;

import javax.swing.*;

public class BODisponibleTransferPositionReportFromToCellEditor extends DefaultCellEditor {

    ReportPanel panel;

    /**
     * Constructs a <code>DefaultCellEditor</code> that uses a text field.
     *
     * @param textField a <code>JTextField</code> object
     */
    public BODisponibleTransferPositionReportFromToCellEditor(JTextField textField) {
        super(textField);
    }

    /**
     * Constructs a <code>DefaultCellEditor</code> object that uses a check box.
     *
     * @param checkBox a <code>JCheckBox</code> object
     */
    public BODisponibleTransferPositionReportFromToCellEditor(JCheckBox checkBox) {
        super(checkBox);
    }

    /**
     * Constructs a <code>DefaultCellEditor</code> object that uses a
     * combo box.
     *
     * @param comboBox a <code>JComboBox</code> object
     */
    public BODisponibleTransferPositionReportFromToCellEditor(JComboBox comboBox, ReportPanel panel) {
        super(comboBox);
        this.panel = panel;
    }

    /**
     * Forwards the message from the <code>CellEditor</code> to
     * the <code>delegate</code>.
     *
     * @see EditorDelegate#getCellEditorValue
     */
    @Override
    public Object getCellEditorValue() {
        return super.getCellEditorValue();
    }


}
