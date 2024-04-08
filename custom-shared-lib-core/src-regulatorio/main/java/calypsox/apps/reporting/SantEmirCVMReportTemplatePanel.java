package calypsox.apps.reporting;

import java.awt.Dimension;
import javax.swing.JCheckBox;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.report.ReportTemplate;

public class SantEmirCVMReportTemplatePanel extends ReportTemplatePanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ReportTemplate template;

	private final JCheckBox flag;

	public static final String LEI = "LEI";

	protected ReportTemplatePanel poAgrPanel;

	public SantEmirCVMReportTemplatePanel() {
		setSize(new Dimension(1140, 50));

		this.flag = new JCheckBox("LEI");
		this.flag.setBounds(350, 2, 150, 40);

		add(this.flag);

	}


	@Override
	public final ReportTemplate getTemplate() {
		final boolean isFlagSelected = this.flag.isSelected();
		
		this.template.put(LEI, isFlagSelected);

		return this.template;
	}



	@Override
	public void setTemplate(final ReportTemplate arg0) {

		this.template = arg0;

		final Boolean flag = (Boolean) this.template.get(LEI);

		if (flag != null) {
			this.flag.setSelected(flag);
		}

	}


}
