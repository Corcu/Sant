package calypsox.ui.component.table.celleditor;

import calypsox.apps.reporting.util.SantLegalEntityChooser;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.ProductAllocator;
import com.calypso.tk.service.DSConnection;
import com.calypso.ui.component.table.celleditor.LegalEntityChooserOptions;
import com.jidesoft.combobox.AbstractComboBox;
import com.jidesoft.combobox.PopupPanel;
import com.jidesoft.swing.JideSwingUtilities;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public class SantLegalEntityNameComboBox extends AbstractComboBox {

    private SantLegalEntityChooserOptions options;

    public SantLegalEntityNameComboBox(SantLegalEntityChooserOptions options) {
        super(1);
        this.options = options;
        this.initComponent();
        this.setEditable(false);
    }

    @Override
    public EditorComponent createEditorComponent() {
        return new DefaultTextFieldEditorComponent(String.class);
    }

    @Override
    public PopupPanel createPopupComponent() {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SantLegalEntityChooser lec;
        Frame frame;
        if(this.options != null && this.options.frame != null){
            frame = this.options.frame;
            lec = new SantLegalEntityChooser(this.options.frame);
        }else{
            frame = JideSwingUtilities.getFrame(this);
            lec = new SantLegalEntityChooser(frame);
        }

        Vector<LegalEntity> les = new Vector<>();
        Vector<LegalEntity> childLEs = new Vector<>();
        String cptyName = "";
        try {
            if(this.options != null && this.options.trade != null){
                cptyName = this.options.trade.getCounterParty().getAuthName();
                les.add(DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(cptyName));
                childLEs = DSConnection.getDefault().getRemoteReferenceData().getChildLegalEntities(cptyName,null);
                les.addAll(childLEs);
            }
        } catch (CalypsoServiceException ex) {
            Log.error(this,ex);
        }

        lec.setModalityType(Dialog.ModalityType.MODELESS);
        lec.setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);
        lec.setModal(false);

        //Sketchy way of filling the report but havent found another way,
        //calling initSearchMultipleSelectionNotUsed(les) only shows the LE's after the second opening of the window
        //and only if it is called in the listener aswell
        lec.setVisible(true);
        lec.initSearchMultipleSelectionNotUsed(les,null);
        lec.setVisible(false);
        lec.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                lec.initSearchMultipleSelectionNotUsed(les,null);
                lec.revalidate();
                lec.configure(LegalEntityChooserOptions.create().multiSelection(false).includeDisabled(false));
                lec.newSelection();
            }
        });
        lec.show();

        String selectedLE = lec.getCurrentSelection();
        LegalEntity le = null;
        try {
            le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(selectedLE);
        } catch (CalypsoServiceException ex) {
            ex.printStackTrace();
        }

        //should always resolve to bond allocator
        ProductAllocator allocator = Defaults.getProductAllocatorFactory().getAllocator(this.options.trade.getProduct());
        if (le != null && !allocator.isLegalEntityValid(le, this.options.trade, new Vector<String>())) {
            AppUtil.displayWarning(frame,"Please, select a child legal entity of the trades CounterParty, or the CounterParty itself: [" + cptyName + "]");
        }else{
            this._editor.setItem(selectedLE);
        }
    }
}
