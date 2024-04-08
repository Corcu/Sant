/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.SantMultiPOSelectorReportTemplatePanel;

public class ELBEIsinCollatReportTemplatePanel extends SantMultiPOSelectorReportTemplatePanel {

	private static final long serialVersionUID = 8853961180218580882L;
	
	private ReportTemplate template;
	private CalypsoCheckBox flag;
	public static final String OLD_WAY = "New Method";
	
	
	public ELBEIsinCollatReportTemplatePanel() {
		super();
		this.flag = new CalypsoCheckBox(OLD_WAY);
		this.flag.setBounds(916, 194, 100, 24);
		add(flag);
	}
	
	@Override
	public final ReportTemplate getTemplate() {
		final boolean isFlagSelected = this.flag.isSelected();
		this.setTemplate(super.getTemplate());
		this.template.put(OLD_WAY, isFlagSelected);
		return this.template;
	}
	
	@Override
	public void setTemplate(final ReportTemplate arg0) {
		this.template = arg0;
		final Boolean flag = (Boolean) this.template.get(OLD_WAY);

		if (flag != null) {
			this.flag.setSelected(flag);
		}
		super.setTemplate(arg0);
	
	}

}
