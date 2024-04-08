package calypsox.apps.reporting;

import calypsox.ui.component.table.celleditor.SantLegalEntityChooserCellEditor;
import calypsox.ui.component.table.celleditor.SantLegalEntityChooserOptions;
import com.calypso.apps.reporting.AllocationPanel;
import com.calypso.tk.refdata.AbstractAllocationRule;
import com.jidesoft.swing.JideSwingUtilities;

import javax.swing.table.TableCellEditor;

public class SantAllocationPanel extends AllocationPanel {

    public SantAllocationPanel() {
    }

    @Override
    protected AllocationTable getAllocationTable() {
        return new BondAllocationTable();
    }

    class BondAllocationTable extends AllocationPanel.AllocationTable {
        BondAllocationTable() {
            super();
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            AbstractAllocationRule.AllocationType choice = SantAllocationPanel.this.toolBar.selectedAllocationType();
            if (AbstractAllocationRule.AllocationType.LEGAL_ENTITY == choice && column == SantAllocationPanel.this.LE_COLUMN) {
                return new SantLegalEntityChooserCellEditor(SantLegalEntityChooserOptions.create(SantAllocationPanel.this._trade)
                        .attach(JideSwingUtilities.getFrame(SantAllocationPanel.this)));
            }

            return super.getCellEditor(row, column);
        }
    }
}
