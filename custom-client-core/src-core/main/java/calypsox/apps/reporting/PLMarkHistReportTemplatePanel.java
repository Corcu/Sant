package calypsox.apps.reporting;

import java.awt.Component;

import com.calypso.apps.reporting.PLMarkReportTemplatePanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.tk.report.ReportTemplate;
import com.jidesoft.status.LabelStatusBarItem;
import com.jidesoft.status.StatusBar;

/**
 * Template Panel for report PLMarkHistReportTemplatePanel
 *
 * Same as PLMarkReportTemplatePanel
 *
 */
@SuppressWarnings("serial")
public class PLMarkHistReportTemplatePanel extends PLMarkReportTemplatePanel {
	public PLMarkHistReportTemplatePanel() {
	}

    @Override
    public void setTemplate(final ReportTemplate reportTemplate) {
        super.setTemplate(reportTemplate);
        final ReportWindow reportWindow = getReportWindow();
        final StatusBar reportWindowStatusBar = reportWindow.getStatusBar();

        final Component[] arrayOfComponent = reportWindowStatusBar
                .getComponents();
        for (final Component component : arrayOfComponent) {
            if (component instanceof LabelStatusBarItem) {
                ((LabelStatusBarItem) component).setText("Archive: ON");
                component.setVisible(true);
                break;
            }
        }

    }
}
