package calypsox.apps.reporting;

import calypsox.tk.anacredit.api.AnacreditConstants;
import com.calypso.apps.reporting.TradeReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class AnacreditTradeReportTemplatePanel extends TradeReportTemplatePanel  {

    private static final long serialVersionUID = -1181869763865226945L;
    public final String LOG = "AnacreditTradeReport";
    JLabel copyTypeLabel = new JLabel();
    CalypsoComboBox copyTypeChoice = new CalypsoComboBox();

    protected void jbInitBackLoading() throws Exception {
        this.copyTypeLabel.setBounds(new Rectangle(867, 92, 73, 24));
        this.copyTypeLabel.setText("Select Anacredit COPY file to Generate");
        this.copyTypeLabel.setHorizontalAlignment(4);
        this.add(this.copyTypeLabel);
        this.copyTypeChoice.setBounds(new Rectangle(952, 92, 188, 24));
        this.add(this.copyTypeChoice);
    }

    public ReportTemplate getTemplate() {
        super.getTemplate();
        this._template.put(AnacreditConstants.ROW_DATA_TYPE, this.copyTypeChoice.getSelectedItem());
        return this._template;
    }

    public void setTemplate(ReportTemplate reporttemplate) {
        super.setTemplate(reporttemplate);
        String s2 = (String)this._template.get(AnacreditConstants.ROW_DATA_TYPE);
        if (s2 != null) {
            this.copyTypeChoice.setSelectedItem(s2);
        } else {
            this.copyTypeChoice.setSelectedIndex(0);
        }

    }

    public AnacreditTradeReportTemplatePanel() {
        try {
            this.jbInitBackLoading();
        } catch (Exception var2) {
            Log.error(LOG, var2);
        }

        this.initDomainsBackLoading();
    }

    protected void initDomainsBackLoading() {
        AppUtil.set(this.copyTypeChoice, Arrays.asList(AnacreditConstants.COPY_3,
                AnacreditConstants.COPY_4, AnacreditConstants.COPY_4A, AnacreditConstants.COPY_11, AnacreditConstants.COPY_13));
    }
}
