package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallPositionEntryReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportTemplate;

public class SantBostonCollateralLevelReport extends MarginCallPositionEntryReport {

    @Override
    public ReportOutput load(Vector errorMsgs) {
        return super.load(errorMsgs);
    }
    
	protected String buildQuery(ReportTemplate template) {
		String ret = super.buildQuery(template);
		if(StringUtils.isNotBlank(ret)){
			ret += " AND margin_call_entries.MCC_ID = mrgcall_config.MRG_CALL_DEF AND mrgcall_config.AGREEMENT_STATUS <> 'CLOSED' ";
		}else{
			ret = " margin_call_entries.MCC_ID = mrgcall_config.MRG_CALL_DEF AND mrgcall_config.AGREEMENT_STATUS <> 'CLOSED' ";
		}
		
		return ret;
	}

}
