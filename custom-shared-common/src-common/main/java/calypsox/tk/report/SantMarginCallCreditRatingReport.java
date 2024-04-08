package calypsox.tk.report;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallCreditRatingReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import org.jfree.util.Log;

public class SantMarginCallCreditRatingReport extends MarginCallCreditRatingReport {

    /**
     *
     */
    private static final long serialVersionUID = 2563316217703881252L;

    @SuppressWarnings("rawtypes")
    @Override
    public ReportOutput load(Vector errorMsgs) {
        String ratingType = "";

        DefaultReportOutput output = new DefaultReportOutput(this);
        int numberRows = 0;
        try {
            List<MarginCallCreditRatingConfiguration> mcRatingCreditConfList = ServiceRegistry.getDefault().getCollateralServer().getAllMarginCallCreditRatingConfig();

            for (int i = 0; i < mcRatingCreditConfList.size(); i++) {
                MarginCallCreditRatingConfiguration currentConf = mcRatingCreditConfList.get(i);

                List<MarginCallCreditRating> mcCreditRatingList = currentConf.getRatings();
                if (mcCreditRatingList != null) {
                    numberRows += mcCreditRatingList.size();
                }
            }

            ReportRow[] rows = new ReportRow[numberRows + 1];

            GlobalRatingConfiguration globalRatingConf = ServiceRegistry.getDefaultRatingConfiguration();

            numberRows = 0;
            for (int i = 0; i < mcRatingCreditConfList.size(); i++) {
                MarginCallCreditRatingConfiguration currentConf = mcRatingCreditConfList.get(i);

                ratingType = currentConf.getRatingType();

                List<MarginCallCreditRating> mcCreditRatingList = currentConf.getRatings();

                if (mcCreditRatingList != null) {
                    for(int iterator = 0; iterator < mcCreditRatingList.size(); iterator++){
                        for(int jterator = iterator+1; jterator < mcCreditRatingList.size(); jterator++){
                            MarginCallCreditRating mcCreditRating1 = mcCreditRatingList.get(iterator);
                            MarginCallCreditRating mcCreditRating2 = mcCreditRatingList.get(jterator);
                            if(mcCreditRating1.getPriority() == mcCreditRating2.getPriority()){
                                if(mcCreditRating1.getAsOfDate().after(mcCreditRating2.getAsOfDate())){
                                    mcCreditRatingList.remove(mcCreditRating2);
                                }else if(mcCreditRating2.getAsOfDate().after(mcCreditRating1.getAsOfDate())){
                                    mcCreditRatingList.remove(mcCreditRating1);
                                }
                            }
                        }
                    }

                    for (int j = 0; j < mcCreditRatingList.size(); j++) {
                        MarginCallCreditRating currentMcCreditRating = mcCreditRatingList.get(j);

                        ReportRow row = new ReportRow(currentMcCreditRating, "MarginCallCreditRatingThreshold"); // Threshold name because of core code
                        row.setProperty("MarginCallCreditRatingConfiguration", currentConf);
                        row.setProperty("GlobalRatingConfiguration", globalRatingConf);
                        row.setProperty("Rating Type", ratingType); // Rating Type property because of core code
                        rows[numberRows] = row;
                        numberRows++;
                    }
                }
            }

            output.setRows(rows);
        } catch (CollateralServiceException e) {
            Log.error(this,e);
        }

        return output;
    }

}
