package calypsox.ui.component.table.celleditor;

import com.calypso.apps.util.LegalEntityFilter;
import com.calypso.tk.core.Trade;
import com.calypso.ui.factory.CustomEditorFactoryOptions;
import com.jidesoft.grid.CellEditorManager;
import com.jidesoft.grid.EditorContext;

import javax.swing.*;
import java.awt.*;

public class SantLegalEntityChooserOptions implements CustomEditorFactoryOptions {

    Frame frame;
    Trade trade;
    LegalEntityFilter filter;

    private SantLegalEntityChooserOptions(){

    }

    public static SantLegalEntityChooserOptions create(Trade trade) {
        SantLegalEntityChooserOptions options = new SantLegalEntityChooserOptions();
        options.trade(trade);
        return options;
    }

    public SantLegalEntityChooserOptions trade(Trade trade) {
        this.trade = trade;
        return this;
    }

    public SantLegalEntityChooserOptions attach(Frame frame) {
        this.frame = frame;
        return this;
    }

    @Override
    public CellEditor createContextualEditor(EditorContext context) {
        return CellEditorManager.getEditor(Object.class, context);
    }

    @Override
    public void customizeEditor(CellEditor editor) {

    }
}
