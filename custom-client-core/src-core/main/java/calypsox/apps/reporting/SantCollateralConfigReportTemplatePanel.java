/**
 * 
 * 
 * @author aela
 * 
 */
package calypsox.apps.reporting;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class SantCollateralConfigReportTemplatePanel extends SantMCConfigReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}
}
