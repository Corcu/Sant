package calypsox.ui.component.table.celleditor;

import com.jidesoft.combobox.AbstractComboBox;
import com.jidesoft.grid.AbstractComboBoxCellEditor;

public class SantLegalEntityChooserCellEditor extends AbstractComboBoxCellEditor {
    public SantLegalEntityChooserCellEditor(SantLegalEntityChooserOptions options) {
        this._comboBox = this.createAbstractComboBox(options);
    }
    private AbstractComboBox createAbstractComboBox(SantLegalEntityChooserOptions options) {
        return new SantLegalEntityNameComboBox(options);
    }

    @Override
    public AbstractComboBox createAbstractComboBox() {
        return null;
    }
}
