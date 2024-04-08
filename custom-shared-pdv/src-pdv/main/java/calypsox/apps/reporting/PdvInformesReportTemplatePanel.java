package calypsox.apps.reporting;


import com.calypso.apps.reporting.TradeReportTemplatePanel;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;
import javax.swing.*;
import java.awt.*;


public class PdvInformesReportTemplatePanel extends TradeReportTemplatePanel {

    private static final long serialVersionUID = 1L;
    protected ReportTemplate reportTemplate;

    protected JCheckBox includeMaturedCheckBox = new JCheckBox("Include Matured");

    public PdvInformesReportTemplatePanel() {
        super();
        try {
            this.jbInitBackLoading();
        } catch (Exception var2) {
            Log.error(this.getClass().getName(), var2);
        }
    }

    protected void jbInitBackLoading() {
        setVisible(true);
        this.includeMaturedCheckBox.setBounds(867, 92, 73, 24);
        this.includeMaturedCheckBox.setHorizontalTextPosition(JCheckBox.LEFT);
        this.includeMaturedCheckBox.setMargin(new Insets(2, 2, 2, 2));
        this.add(this.includeMaturedCheckBox);

    }

    public ReportTemplate getTemplate() {
        super.getTemplate();
        this._template.put("Include Matured", this.includeMaturedCheckBox.isSelected());
        return this._template;
    }

    public void setTemplate(ReportTemplate reporttemplate) {
        super.setTemplate(reporttemplate);
        Boolean includeMatured = (Boolean) this._template.get("Include Matured");
        if (includeMatured != null) {
            this.includeMaturedCheckBox.setSelected(includeMatured);
        }
    }

}