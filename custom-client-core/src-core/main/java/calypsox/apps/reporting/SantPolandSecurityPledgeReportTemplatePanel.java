package calypsox.apps.reporting;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.TradeReportTemplatePanel;
import com.calypso.apps.util.TableModelUtil;

/**
 * Customized GUI elements to select criteria for the SantPolandSecurityPledge
 * report.
 * 
 * @author Carlos Cejudo
 *
 */
public class SantPolandSecurityPledgeReportTemplatePanel
        extends TradeReportTemplatePanel {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 1L;

    private static final String SELECT_ALL_LABEL_TEXT = "Select All";
    private static final String UNSELECT_ALL_LABEL_TEXT = "Unselect All";
    private static final String REVERSE_TRADES_TEXT = "Reverse Trades";

    private static final String REPORT_TITLE = "Sant Poland Security Pledge";

    protected MouseListener mouseListener;

    @Override
    protected void jbInit() throws Exception {
        super.jbInit();

        // Get original criteria panel
        JPanel originalPanel = null;
        int originalComponentIndex = -1;
        Component[] components = getComponents();
        for (int iComponent = 0; originalPanel == null
                && iComponent < components.length; iComponent++) {
            if (components[iComponent] instanceof JPanel) {
                originalPanel = (JPanel) components[iComponent];
                originalComponentIndex = iComponent;
            }
        }
        // Remove the original panel, we will later include it in out own panel.
        remove(originalComponentIndex);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(originalPanel);

        // Add new buttons to the right of the original panel.
        mainPanel.add(getReverseButtonsPanel());

        // Add the main panel (with original panel and new buttons) in the same
        // position as the original panel was.
        add(mainPanel, originalComponentIndex);

        // Set window title
        getReportWindow().setTitle(REPORT_TITLE);
    }

    /**
     * Builds a panel containing the new elements of the Template Panel,
     * including:<list>
     * <li>"Select All" Checkbox</li>
     * <li>"Unselect All" Checkbox</li>
     * <li>"Reverse Trades" Button</li></list>
     * 
     * @return A JPanel with the new elements.
     */
    private JPanel getReverseButtonsPanel() {
        JPanel reverseButtonsPanel = new JPanel();
        reverseButtonsPanel.setLayout(
                new BoxLayout(reverseButtonsPanel, BoxLayout.PAGE_AXIS));

        JCheckBox selectAllCheckBox = new JCheckBox();
        selectAllCheckBox.setText(SELECT_ALL_LABEL_TEXT);
        selectAllCheckBox.setActionCommand(
                SantPolandSecurityPledgeActionListener.ACTION_SELECT_ALL);

        JCheckBox unselectAllCheckBox = new JCheckBox();
        unselectAllCheckBox.setText(UNSELECT_ALL_LABEL_TEXT);
        unselectAllCheckBox.setActionCommand(
                SantPolandSecurityPledgeActionListener.ACTION_UNSELECT_ALL);

        JButton reverseTradesButton = new JButton();
        reverseTradesButton.setText(REVERSE_TRADES_TEXT);
        reverseTradesButton.setActionCommand(
                SantPolandSecurityPledgeActionListener.ACTION_REVERSE_TRADES);

        // Construct ActionListener
        ActionListener actionListener = new SantPolandSecurityPledgeActionListener(
                this, selectAllCheckBox, unselectAllCheckBox);
        // Construct MouseListener
        mouseListener = new SantPolandSecurityPledgeMouseListener(this,
                selectAllCheckBox, unselectAllCheckBox);

        // All three elements use the same ActionListener
        selectAllCheckBox.addActionListener(actionListener);
        unselectAllCheckBox.addActionListener(actionListener);
        reverseTradesButton.addActionListener(actionListener);

        reverseButtonsPanel.add(selectAllCheckBox);
        reverseButtonsPanel.add(unselectAllCheckBox);
        reverseButtonsPanel.add(reverseTradesButton);

        return reverseButtonsPanel;
    }

    /**
     * This method is overriden to we can add our custom MouseListener in case
     * it has not been added yet.
     *
     * @param panel
     *            Panel to associate the publisher.
     */
    @Override
    public void callAfterDisplay(final ReportPanel panel) {
        TableModelUtil m = panel.getTableModelWithFocus();
        JTable table = m.getTable();
        if (!Arrays.asList(table.getMouseListeners()).contains(mouseListener)) {
            table.addMouseListener(mouseListener);
        }
    }

}
