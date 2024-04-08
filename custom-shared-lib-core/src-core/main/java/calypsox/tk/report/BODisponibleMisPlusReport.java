package calypsox.tk.report;

import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;

import java.util.Vector;

public class BODisponibleMisPlusReport extends BODisponibleSecurityPositionReport{
    @Override
    public ReportOutput load(Vector errorMsgs) {
        StandardReportOutput output = new StandardReportOutput(this);
        DefaultReportOutput load = (DefaultReportOutput) super.load(errorMsgs);
        if(null!=load && !Util.isEmpty(load.getRows())){
            output.setRows(load.getRows());
        }
        return output;
    }
}
