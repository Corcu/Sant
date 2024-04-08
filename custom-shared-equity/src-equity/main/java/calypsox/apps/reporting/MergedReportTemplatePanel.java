package calypsox.apps.reporting;

import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class MergedReportTemplatePanel extends ReportTemplatePanel {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public static final String TEMPLATE_TYPE_1 = "TEMPLATE_TYPE_1";
	public static final String TEMPLATE_CHOICE_1 = "TEMPLATE_CHOICE_1";
	public static final String TEMPLATE_TYPE_2 = "TEMPLATE_TYPE_2";
	public static final String TEMPLATE_CHOICE_2 = "TEMPLATE_CHOICE_2";
	public static final String COLUMN_MERGE_MODE = "COLUMN_MERGE_MODE";
	public static final String COLUMN_MERGE_MODE_ID = "Id";
	public static final String COLUMN_MERGE_MODE_NUMBER = "Number";
	public static final String COLUMN_MERGE_MODE_NAME = "Name";
	
	protected ReportTemplate _template;
	
	CalypsoComboBox templateTypeChoice1 = new CalypsoComboBox();
	CalypsoComboBox templateNameChoice1 = new CalypsoComboBox();
	CalypsoComboBox templateTypeChoice2 = new CalypsoComboBox();
	CalypsoComboBox templateNameChoice2 = new CalypsoComboBox();
	CalypsoComboBox mergeColumnMode = new CalypsoComboBox();

	
	@Override
	public ReportTemplate getTemplate() {
		_template.put(TEMPLATE_TYPE_1, templateTypeChoice1.getSelectedItem());
		_template.put(TEMPLATE_CHOICE_1, templateNameChoice1.getSelectedItem());
		_template.put(TEMPLATE_TYPE_2, templateTypeChoice2.getSelectedItem());
		_template.put(TEMPLATE_CHOICE_2, templateNameChoice2.getSelectedItem());
		_template.put(COLUMN_MERGE_MODE, mergeColumnMode.getSelectedItem());

		return _template;
	}
	
	
	@Override
	public void setTemplate(ReportTemplate template) {
		_template = template;
		if(template!=null) {
		
			setJComboBoxValue(templateTypeChoice1, template.get(TEMPLATE_TYPE_1));
			setJComboBoxValue(templateNameChoice1, template.get(TEMPLATE_CHOICE_1));
			setJComboBoxValue(templateTypeChoice2, template.get(TEMPLATE_TYPE_2));
			setJComboBoxValue(templateNameChoice2, template.get(TEMPLATE_CHOICE_2));
			String columnMergeMode = template.get(COLUMN_MERGE_MODE);
			if(columnMergeMode==null)
				columnMergeMode=COLUMN_MERGE_MODE_ID;
			mergeColumnMode.setSelectedItem(columnMergeMode);
			
		}
		
	}

	
	public MergedReportTemplatePanel() {
		initPanel();
	}
	
	@SuppressWarnings("unchecked")
	public void initPanel() {

		this.setLayout((LayoutManager) null);

		int startX = 25;
		int startY = 15;
		int comboWidth = 150;
		int labelWidth1 = 40;
		int labelWidth2 = 70;
		int labelWidth3 = 160;
		int spacing = 15;
		
		int currentX = startX;
		int currentY = startY;
		
		JLabel labelType1 = new JLabel("Type :");
		labelType1.setBounds(currentX, currentY, labelWidth1, 22);
		add(labelType1);
		
		currentX = currentX+labelWidth1+spacing;
		
		add(this.templateTypeChoice1);
		this.templateTypeChoice1.setBounds(
				currentX,
				currentY,
				comboWidth, 22);
		
		currentX = currentX + comboWidth + spacing;
		
		JLabel labelTemplate1 = new JLabel("Template :");
		labelTemplate1.setBounds(currentX, currentY, currentX+labelWidth2, 22);
		add(labelTemplate1);

		
		currentX = currentX + labelWidth2 + spacing;

		add(this.templateNameChoice1);
		this.templateNameChoice1.setBounds(
				currentX,
				currentY,
				comboWidth, 22);

		currentX = startX ;
		currentY = currentY + 30;

		
		JLabel labelType2 = new JLabel("Type :");
		labelType2.setBounds(currentX, currentY, labelWidth1, 22);
		add(labelType2);
		
		currentX = currentX+labelWidth1+spacing;
		
		add(this.templateTypeChoice2);
		this.templateTypeChoice2.setBounds(
				currentX,
				currentY,
				comboWidth, 22);
		
		currentX = currentX + comboWidth + spacing;
		
		JLabel labelTemplate2 = new JLabel("Template :");
		labelTemplate2.setBounds(currentX, currentY, currentX+labelWidth2, 22);
		add(labelTemplate2);

		
		currentX = currentX + labelWidth2 + spacing;

		add(this.templateNameChoice2);
		this.templateNameChoice2.setBounds(
				currentX,
				currentY,
				comboWidth, 22);
		
		for(String type : getReportTypes()) {
			templateTypeChoice1.addItem(type);
			templateTypeChoice2.addItem(type);
		}
		

		templateTypeChoice1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				CalypsoComboBox sourceBox = (CalypsoComboBox) e.getSource();
				templateNameChoice1.removeAllItems();
				Vector<ReportTemplateName> templates = getTemplates(sourceBox.getSelectedItem().toString());
				for(ReportTemplateName templateName : templates) {
					templateNameChoice1.addItem(templateName.getTemplateName());
				}
			}
			
		});
		
		
		templateTypeChoice2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				CalypsoComboBox sourceBox = (CalypsoComboBox) e.getSource();
				templateNameChoice2.removeAllItems();
				Vector<ReportTemplateName> templates = getTemplates(sourceBox.getSelectedItem().toString());
				for(ReportTemplateName templateName : templates) {
					templateNameChoice2.addItem(templateName.getTemplateName());
				}
				
			}
			
		});
		
		
		currentX = startX ;
		currentY = currentY + 30;
		
		JLabel labelMergeByColumnNumber = new JLabel("Column merge mode :");
		labelMergeByColumnNumber.setBounds(currentX, currentY, labelWidth3, 22);
		add(labelMergeByColumnNumber);
		
		mergeColumnMode.addItem(COLUMN_MERGE_MODE_ID);
		mergeColumnMode.addItem(COLUMN_MERGE_MODE_NUMBER);
		mergeColumnMode.addItem(COLUMN_MERGE_MODE_NAME);
		
		mergeColumnMode.setBounds(currentX + labelWidth3 + spacing, currentY, 100, 22);
		add(mergeColumnMode);
		
	}
	
	@SuppressWarnings("rawtypes")
	protected void setJComboBoxValue(JComboBox comboBox, String value) {
		if(value==null) {
			if(comboBox.getItemCount()!=0)
				comboBox.setSelectedIndex(0);
		}
		else
				comboBox.setSelectedItem(value);
	}
	
	public Vector<String> getReportTypes() {
		return LocalCache.getDomainValues(DSConnection.getDefault(),"REPORT.Types");
	}
	
	public Vector<ReportTemplateName> getTemplates(String reportType) {
		Vector<ReportTemplateName> templates = BOCache.getReportTemplateNames(DSConnection.getDefault(), reportType, DSConnection.getDefault().getUser());
		return templates;
	}

}
