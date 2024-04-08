package calypsox.apps.reporting;

import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;

public class AnacreditRepoReportTemplatePanel extends AnacreditTradeReportTemplatePanel {

    CalypsoCheckBox ctasFilter;
    public static final String CTAS_FILTER = "CTASFilter";
    JLabel filterCTASLabel;

    @Override
    protected void jbInitBackLoading() throws Exception {
        ctasFilter = new CalypsoCheckBox("CTAS Filter");

        this.add(this.ctasFilter);
        super.jbInitBackLoading();
    }

    public ReportTemplate getTemplate() {
        super.getTemplate();
        this._template.put(CTAS_FILTER, this.ctasFilter.isSelected());
        return this._template;
    }

    public void setTemplate(ReportTemplate reporttemplate) {
        super.setTemplate(reporttemplate);
        Boolean isSelected = (Boolean)this._template.get(CTAS_FILTER);
        if (isSelected != null) {
            ctasFilter.setSelected(isSelected);
        } else {
            ctasFilter.setSelected(true);
        }

    }
}
