package calypsox.apps.reporting;

import com.calypso.apps.reporting.AuditReportTemplatePanel;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BODisponibleAuditReportTemplatePanel extends AuditReportTemplatePanel implements ActionListener{

    ReportTemplate template;
    CalypsoCheckBox calculateRealPositionCheck;
    public static final String EXCLUDE_NETTING_TRANSFERS = "Exclude Netting";

    public BODisponibleAuditReportTemplatePanel() {
        this.calculateRealPositionCheck = new CalypsoCheckBox(EXCLUDE_NETTING_TRANSFERS);
        this.add(this.calculateRealPositionCheck);
        this.calculateRealPositionCheck.setBounds(456, 105, 190, 24);
        this.calculateRealPositionCheck.setToolTipText(EXCLUDE_NETTING_TRANSFERS);
        this.calculateRealPositionCheck.addActionListener(this);
    }

    @Override
    public void setTemplate(ReportTemplate arg0) {
        super.setTemplate(arg0);
        this.template = arg0;
        if(null!=this.template){
            String s = (String) this.template.get(EXCLUDE_NETTING_TRANSFERS);
            if (Util.isTrue(s, false)) {
                this.calculateRealPositionCheck.setSelected(true);
            }
        }
    }

    @Override
    public boolean isValidLoad(ReportPanel panel) {
        return true;
    }

    @Override
    public ReportTemplate getTemplate() {
        this.template = super.getTemplate();
        if(null!=this.template){
            this.template.put(EXCLUDE_NETTING_TRANSFERS, this.calculateRealPositionCheck.isSelected() ? "true" : "false");
        }
        return this.template;
    }

    /**
     * Invoked when an action occurs.
     *
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object object = event.getSource();
        if (!(object instanceof CalypsoCheckBox)) {
            return;
        }

        CalypsoCheckBox checkBox = (CalypsoCheckBox) object;
        if (checkBox != calculateRealPositionCheck) {
            return;
        }

        if (!checkBox.isSelected()) {
            checkBox.setSelected(false);
        }
    }

}
