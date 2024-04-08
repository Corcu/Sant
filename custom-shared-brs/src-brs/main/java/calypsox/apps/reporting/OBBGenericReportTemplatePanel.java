package calypsox.apps.reporting;

import com.calypso.apps.reporting.PostingReportTemplatePanel;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.report.ReportTemplate;

public class OBBGenericReportTemplatePanel extends PostingReportTemplatePanel {

    private ReportTemplate template;
    private CalypsoCheckBox agregoFlag;
    public static final String SET_AGREGO = "DoNotSetAgrego";


    public OBBGenericReportTemplatePanel() {
        super();
        this.agregoFlag = new CalypsoCheckBox(SET_AGREGO);
        this.agregoFlag.setBounds(1130, 3, 150, 24);
        //this.agregoFlag.setBounds(500, 273, 140, 24);
        add(agregoFlag);
    }

    @Override
    public final ReportTemplate getTemplate() {
        final boolean isFlagSelected = this.agregoFlag.isSelected();
        this.setTemplate(super.getTemplate());
        this.template.put(SET_AGREGO, isFlagSelected);
        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate arg0) {
        this.template = arg0;
        final Boolean flag = (Boolean) this.template.get(SET_AGREGO);

        if (flag != null) {
            this.agregoFlag.setSelected(flag);
        }
        super.setTemplate(arg0);

    }

}
