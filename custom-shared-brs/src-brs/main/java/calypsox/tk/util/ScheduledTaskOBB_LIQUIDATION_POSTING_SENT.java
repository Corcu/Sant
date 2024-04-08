package calypsox.tk.util;

import calypsox.tk.event.PSEventPostingLiquidation;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScheduledTaskOBB_LIQUIDATION_POSTING_SENT extends ScheduledTaskREPORT {
    
	
    public ScheduledTaskOBB_LIQUIDATION_POSTING_SENT() {
    	super();
    }

    
    @Override
    public String getTaskInformation() {
        return "Generate and send OBB Liquidation Lines (Postings) to MIC";
    }
    
    
    @Override
    protected String saveReportOutput(ReportOutput output, String format, String type, String[] fileNames, StringBuffer sb) {
        List<PSEventPostingLiquidation> allEvents = new ArrayList<>();
        if(null!=output){
            DefaultReportOutput reportOutput = (DefaultReportOutput) output;
            if(null!=reportOutput.getRows()){
                Arrays.stream(reportOutput.getRows()).forEach(line ->{
                	BOPosting posting = line.getProperty("Default");
                	PSEventPostingLiquidation event = new PSEventPostingLiquidation(posting.getId());
                	Log.info(this, "Published event for posting " + posting.getId());
                	allEvents.add(event);
                });
            }            
        }
        publishAllevents(allEvents);
        Log.info(this, "All cash posting published.");
        return "";
    }
    
    
    private void publishAllevents(List<PSEventPostingLiquidation> postingLiqList){
        try {
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(postingLiqList);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error publishing cash posting: " + e.getCause().getMessage());
        }
    }

    
}