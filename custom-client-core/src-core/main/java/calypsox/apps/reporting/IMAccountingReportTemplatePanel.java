package calypsox.apps.reporting;

import java.awt.Dimension;
import java.util.List;
import javax.swing.JLabel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;

public class IMAccountingReportTemplatePanel extends ReportTemplatePanel{

	private static final long serialVersionUID = 1L;
	
	private ReportTemplate template;
	protected SantLegalEntityPanel poAgrPanel;
	public static final String CASH = "CASH";
	public static final String SEC = "SEC";
	private CalypsoComboBox cashTemplate;
	private CalypsoComboBox secTemplate;

	public IMAccountingReportTemplatePanel() {
		setSize(new Dimension(1140, 50));
		
		this.cashTemplate = new CalypsoComboBox();
		this.secTemplate = new CalypsoComboBox();
		
		setTemplateNames(cashTemplate,"BOCashPosition");
		setTemplateNames(secTemplate,"BOSecurityPosition");
		
	    JLabel cash = new JLabel("Cash Position Template");
	    cash.setBounds(231, 61, 80, 24);
	    add(cash);
		add(this.cashTemplate);
		JLabel sec = new JLabel("Security Position Template");
		sec.setBounds(231, 61, 80, 24);
	    add(sec);
		add(this.secTemplate);
	
	}


	@Override
	public final ReportTemplate getTemplate() {

		 this.template.put(CASH, this.cashTemplate.getSelectedItem());
		 this.template.put(SEC, this.secTemplate.getSelectedItem());

		return this.template;
	}

	@Override
	public void setTemplate(final ReportTemplate arg0) {
		this.template = arg0;
		this.cashTemplate.setSelectedItem(template.get(CASH));
		this.secTemplate.setSelectedItem(template.get(SEC));
	}
	
	@SuppressWarnings("unchecked")
	public void setTemplateNames(CalypsoComboBox combo,String reportName){
		List<ReportTemplateName> templates;
		templates = BOCache.getReportTemplateNames(DSConnection.getDefault(), reportName, DSConnection.getDefault().getUser());
			if(!Util.isEmpty(templates)){
				for(ReportTemplateName name : templates){
					combo.addItem(name.getTemplateName());
				}
			}	
	}
	
}
