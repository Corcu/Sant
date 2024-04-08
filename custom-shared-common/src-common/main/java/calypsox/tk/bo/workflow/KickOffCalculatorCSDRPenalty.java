package calypsox.tk.bo.workflow;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.KickOffCalculator;
import com.calypso.tk.bo.workflow.KickOffCutOffConfig;
import com.calypso.tk.core.JDatetime;

/**
 * @author aalonsop
 */
public class KickOffCalculatorCSDRPenalty implements KickOffCalculator {
    @Override
    public JDatetime getKickOffTime(KickOffCutOffConfig cfg, Task task, JDatetime dt) {
        return dt;
    }
    @Override
    public JDatetime getCutOffTime(KickOffCutOffConfig cfg, Task task, JDatetime dt) {
        return null;
    }


}
