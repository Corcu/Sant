package calypsox.apps.reporting;

import calypsox.tk.report.SecurityReportTemplate;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.report.ReportTemplate;
import com.jidesoft.swing.JideBoxLayout;

import javax.swing.*;

/**
 * @author aalonsop
 */
public class SecurityReportTemplatePanel extends ReportTemplatePanel{

    private CalypsoCheckBox includeCheckBox;
    private CalypsoCheckBox filterDuplicatedIsin;
    protected ReportTemplate template;


    public SecurityReportTemplatePanel() {
        initCheckBoxDuplicates();
        initCheckBoxMatured();

        this.add(this.includeCheckBox);
        this.add(this.filterDuplicatedIsin);

        this.setLayout(new JideBoxLayout(this, 0));
        this.template=new SecurityReportTemplate();
    }

    protected void initCheckBoxMatured() {
        this.filterDuplicatedIsin = new CalypsoCheckBox();// 393
        this.filterDuplicatedIsin.setHorizontalAlignment(SwingConstants.LEFT);
        this.filterDuplicatedIsin.setVerticalAlignment(SwingConstants.CENTER);
        this.filterDuplicatedIsin.setText("Filter duplicated ISINs");
        this.filterDuplicatedIsin.setToolTipText("If selected, rows will be filtered to avoid isin duplicities even in case of being different products");
    }

    protected void initCheckBoxDuplicates() {
        this.includeCheckBox = new CalypsoCheckBox();// 393
        this.includeCheckBox.setHorizontalAlignment(SwingConstants.LEFT);
        this.includeCheckBox.setVerticalAlignment(SwingConstants.CENTER);
        this.includeCheckBox.setText("Include Matured Securities");
        this.includeCheckBox.setToolTipText("If selected, matured securities will appear in the report");
    }

    @Override
    public void setTemplate(ReportTemplate template) {
        if(template!=null){
            this.includeCheckBox.setSelected(Boolean.parseBoolean(template.get("INCLUDE_MATURED_SECURITY")));
            this.filterDuplicatedIsin.setSelected(Boolean.parseBoolean(template.get("FILTER_DUPLICATED_ISIN")));
        }
    }

    @Override
    public ReportTemplate getTemplate() {
        this.template.put("INCLUDE_MATURED_SECURITY", Boolean.toString(this.includeCheckBox.isSelected()));
        this.template.put("FILTER_DUPLICATED_ISIN", Boolean.toString(this.filterDuplicatedIsin.isSelected()));
        return this.template;
    }
}
