package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JTable;

import com.calypso.apps.reporting.MarginCallEntryReportTemplatePanel;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.tk.report.ReportTemplate;

import calypsox.tk.report.OptimizerMarginCallEntryReportStyle;
import calypsox.tk.util.OptimizerMarginCallEntryConstants;

public class OptimizerMarginCallEntryReportTemplatePanel extends
		MarginCallEntryReportTemplatePanel implements
		OptimizerMarginCallEntryConstants {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8343573245338021777L;

	public OptimizerMarginCallEntryReportTemplatePanel() {
		GridLayout gridLayoutMain = new GridLayout(1, 2, 2, 2);
		this.setLayout(gridLayoutMain);
		OptimizerMarginCallEntryFilterPanel optimizerFilterPanel = new OptimizerMarginCallEntryFilterPanel();
		optimizerFilterPanel.setReportTemplatePanel(this);
		this.add(optimizerFilterPanel, BorderLayout.WEST);

		initMouseAdapter();
	}

	@Override
	public ReportTemplate getTemplate() {
		ReportTemplate reportTemplate = super.getTemplate();
		return reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
	}

	private MouseAdapter mouseAdapterClick;

	/**
	 * initMouseAdapter
	 */
	void initMouseAdapter() {
		mouseAdapterClick = new MouseAdapter() {
			@SuppressWarnings("deprecation")
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 1) {
					JTable table = getReportWindow().getReportPanel()
							.getTableModel().getTable();
					int colOptimize = findColumnIndex(getReportWindow(),
							OptimizerMarginCallEntryReportStyle.OPTIMIZE_FIELD);
					int selectedCol = table.getSelectedColumn();
					if (colOptimize == selectedCol) {
						int selectedRow = table.getSelectedRow();
						if (selectedRow >= 0) {
							table.setValueAt(!(Boolean) table.getValueAt(
									selectedRow, selectedCol), selectedRow,
									selectedCol);
							getReportWindow().getReportPanel().getTableModel()
									.fireTableDataChanged();
							
						}
					}
				}
			}
		};
	}

	public void callAfterDisplay(final ReportPanel panel) {
		@SuppressWarnings("deprecation")
		TableModelUtil m = panel.getTableModel();
		JTable table = m.getTable();
		if (!Arrays.asList(table.getMouseListeners()).contains(
				mouseAdapterClick)) {
			table.addMouseListener(mouseAdapterClick);
		}
	}

	/**
	 * Function to retrieve Column index
	 * 
	 * @param reportWindow
	 * 
	 * @param name
	 * @return index
	 */
	@SuppressWarnings("deprecation")
	static int findColumnIndex(ReportWindow reportWindow, String name) {
		int col = reportWindow.getReportPanel().getTableModel()
				.findColumn(name);
		if (col == -1) {
			// look for filtered column
			col = reportWindow.getReportPanel().getTableModel()
					.findColumn(name + " (F)");
		}
		return col;
	}
}
