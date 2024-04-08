package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportObjectHandler;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.presentation.risk.RiskPresenterWorker;
import com.calypso.tk.product.secfinance.triparty.TripartyAllocationRecord;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Triparty Collateral Allocation window customization.<br>
 * <br>
 * This class extends from the core calypso class TripartyCollateralAllocationReportWindowHandler, overwriting its core methods. <br>
 * In version 1.0, the customization is adding an option when right clicking a specific row, in order to open the specific trade associated to said selected row. <br>
 *
 * @see com.calypso.apps.reporting.TripartyCollateralAllocationReportWindowHandler
 * TripartyCollateralAllocationReportWindowHandler
 * @see com.calypso.apps.reporting.ReportWindowHandlerAdapter
 * ReportWindowHandlerAdapter
 * @see java.awt.event.ActionListener
 * ActionListener
 *
 * @author x854118
 * @version 1.0
 */
public class TripartyCollateralAllocationReportWindowHandler extends com.calypso.apps.reporting.TripartyCollateralAllocationReportWindowHandler implements ActionListener {


    /**
     * This customization of the popup menu includes an additional action to show the pledge id associated to the selected row.
     *
     * @param jpopupmenu the popup menu that appears when right clicking row in the report
     * @param riskpresenterworker the risk associated parameters for the report
     */
    @Override
    public void customizePopupMenu(final JPopupMenu jpopupmenu, final RiskPresenterWorker riskpresenterworker) {
        boolean foundShowMenu = false;

        final Component[] components = jpopupmenu.getComponents();
        for (int i = 0; i < components.length; i++) {
            final Component component = components[i];
            if (component instanceof JMenu) {
                final JMenu menu = (JMenu) component;
                final String menuName = menu.getText();
                if ("Show".equalsIgnoreCase(menuName) && menu.isVisible()) {
                    foundShowMenu = true;
                }
            }
        }

        if(!foundShowMenu){
            JMenu showMenu = new JMenu("Show");

            JMenuItem tradeMenuItem = new JMenuItem("Trade");
            tradeMenuItem.setActionCommand("Trade");
            tradeMenuItem.addActionListener(this);

            showMenu.add(tradeMenuItem);
            jpopupmenu.add(showMenu);
        }
    }

    /**
     * Implements the functionality to open the associated trade to the selected row when selecting open trade from the context menu.
     *
     * @param e the event that contains the selected action from the context menu
     */
    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(final ActionEvent e) {
        if (this._reportWindow == null) {
            return;
        }

        if (this._reportWindow.getReportPanel().getRowCount() <= 0) {
            return;
        }

        Vector<TripartyAllocationRecord> tripartyRows = this._reportWindow.getReportPanel().getSelectedObjects();

        if (tripartyRows.size() > 1) {
            AppUtil.displayWarning("Select at most one row", this._reportWindow);
        }
        if (tripartyRows.size() == 0) {
            AppUtil.displayWarning("Select at least one row", this._reportWindow);
        }

        final long selectedRowPledgeId = tripartyRows.get(0).getPledgeId();
        if (selectedRowPledgeId > 0) {
            ReportObjectHandler.showTrade(selectedRowPledgeId);
        }
    }
}
