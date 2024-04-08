package calypsox.tk.util;

import com.calypso.tk.report.ReportTemplate;

import java.util.Vector;

/**
 * @author aalonsop
 * Overrides shitty ScheduledTaskCSVREPORT to enable/disable custom BOTask creation to record every object's exported status.
 */
public class ScheduledTaskCarterasReport extends ScheduledTaskCSVREPORT{

    public static final String TASK_MARKING_FLAG = "Enable BOTask creation";

    @Override
    public void setTemplateCustomData(ReportTemplate template) {
        super.setTemplateCustomData(template);
        template.put(TASK_MARKING_FLAG,getBooleanAttribute(TASK_MARKING_FLAG));
    }

    @Override
    public Vector getDomainAttributes() {
        final Vector result = super.getDomainAttributes();
        result.add(TASK_MARKING_FLAG);
        return result;
    }
}
