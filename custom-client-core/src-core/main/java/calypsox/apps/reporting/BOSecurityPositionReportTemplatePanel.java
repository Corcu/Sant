package calypsox.apps.reporting;

import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.report.ReportTemplate;

public class BOSecurityPositionReportTemplatePanel extends com.calypso.apps.reporting.BOSecurityPositionReportTemplatePanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private ReportTemplate template;
    private CalypsoCheckBox flag;
    public static final String CORE_FLAG = "Core Flag";


    public BOSecurityPositionReportTemplatePanel() {
        super();
        this.flag = new CalypsoCheckBox(CORE_FLAG);
        this.flag.setBounds(916, 194, 100, 24);
        add(flag);
    }

    @Override
    public ReportTemplate getTemplate() {
        final boolean isFlagSelected = this.flag.isSelected();
        this.setTemplate(super.getTemplate());
        this.template.put(CORE_FLAG, isFlagSelected);
        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate arg0) {

        this.template = arg0;
        final Boolean flag = (Boolean) this.template.get(CORE_FLAG);

        if (flag != null) {
            this.flag.setSelected(flag);
        }
        super.setTemplate(arg0);

    }

}
