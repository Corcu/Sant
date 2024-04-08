package calypsox.apps.reporting;

import calypsox.tk.report.AccountEnrichmentReportTemplate;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;

/**
 * AccountEnrichmentReportTemplatePanel
 *
 * @author x933435
 */
public class AccountEnrichmentReportTemplatePanel extends com.calypso.apps.reporting.AccountEnrichmentReportTemplatePanel {

    JCheckBox loadLinkedTransfer = new JCheckBox();

    public AccountEnrichmentReportTemplatePanel() {
        super();
        jbInit();
    }

    private void jbInit() {
        this.loadLinkedTransfer.setText("Load CRE xfer");
        this.loadLinkedTransfer.setToolTipText("If you select this option, the TransferReport columns will " +
                "be generated with the transfer associated with the CRE.");
        this.loadLinkedTransfer.setBounds(4, 196, 120, 24);
        this.loadLinkedTransfer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(loadLinkedTransfer);
    }


    @Override
    public void setTemplate(ReportTemplate template) {
        if (template != null) {
            Boolean b = template.get(AccountEnrichmentReportTemplate.loadCreTransfer);
            if (b != null) {
                this.loadLinkedTransfer.setSelected(b);
            }
        }
        super.setTemplate(template);
    }

    @Override
    public ReportTemplate getTemplate() {
        ReportTemplate template = super.getTemplate();
        template.put(AccountEnrichmentReportTemplate.loadCreTransfer, this.loadLinkedTransfer.isSelected());
        return template;
    }
}
