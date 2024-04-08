package calypsox.tk.report;

import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SantBostonLegalAgreementLevelReport extends CollateralConfigReport {
	private static final long serialVersionUID = 1L;
    
    @Override
    public ReportOutput load(Vector errorMsgs) {
    	//Try this out calling the super.load() method
        DefaultReportOutput output = new DefaultReportOutput(this);
        //ReportTemplate reportTemplate = getReportTemplate();
        if(this.getReportTemplate()!=null){
            List<CollateralConfig> allContracts = super.loadMarginCallConfigs(this.getReportTemplate());
            
            // The CLOSED contracts will be not included in the report
            List<CollateralConfig> contracts = new ArrayList<>();
            for(CollateralConfig contract : allContracts){
            	if(contract!=null && !"CLOSED".equals(contract.getAgreementStatus())){
            		contracts.add(contract);
            	}
            }
            
            ReportRow[] rows = new ReportRow[contracts.size()];
            for(int i = 0; i < rows.length; ++i) {
                rows[i] = new ReportRow(contracts.get(i), "MarginCallConfig");
            }
            output.setRows(rows);
        }
        return output;

    }
}
