package calypsox.tk.bo.workflow;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.KickOffCalculator;
import com.calypso.tk.bo.workflow.KickOffCutOffConfig;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.service.DSConnection;

import java.util.Calendar;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class KickOffCalculatorFromXferValueDate implements KickOffCalculator {

    @Override
    public JDatetime getKickOffTime(KickOffCutOffConfig cfg, Task task, JDatetime dt) {
        JDatetime kickoffTime=dt;
        if(task!=null&& PSEventTransfer.class.getSimpleName().equals(task.getEventClass())){
                BOTransfer xfer= getBOTransfer(task);
                JDate valueDate=Optional.ofNullable(xfer).map(BOTransfer::getValueDate).orElse(null);
                if(valueDate!=null) {
                    JDatetime kickoff=valueDate.addBusinessDays(cfg.getKickOff(),cfg.getHolidays()).getJDatetime();
                    kickoffTime = setKickoffTimeFromConfig(kickoff,cfg);
                    Log.debug(this,"Calculated KickOff Time for Xfer: "+xfer.getLongId()+" is "+ kickoffTime);
                }
        }
        return kickoffTime;
    }

    private JDatetime setKickoffTimeFromConfig(JDatetime kickOffDate,KickOffCutOffConfig cfg){
        JDatetime kickoffTime=kickOffDate;
        if(cfg.getKickOffTime()!=0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(kickOffDate);
            calendar.set(Calendar.HOUR_OF_DAY, this.splitTime(cfg.getKickOffTime(), true));
            calendar.set(Calendar.MINUTE, this.splitTime(cfg.getKickOffTime(), false));
            kickoffTime=new JDatetime(calendar);
        }
        return kickoffTime;
    }

    private BOTransfer getBOTransfer(Task task){
        BOTransfer xfer=null;
        Object obj=task.getUnderlyingObjects().get(BOTransfer.class);
        if(obj instanceof BOTransfer){
            xfer= (BOTransfer) obj;
        }else{
            long xferId=task.getObjectLongId();
            if(xferId>0L) {
                try {
                    xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(xferId);
                } catch (CalypsoServiceException exc) {
                    Log.error(this,exc.getCause());
                }
            }
        }
        return xfer;
    }

    @Override
    public JDatetime getCutOffTime(KickOffCutOffConfig cfg, Task task, JDatetime dt) {
        return null;
    }

    public int splitTime(int hhmm, boolean hours) {
        return hours ? hhmm / 100 : hhmm % 100;// 58 59 62
    }
}
