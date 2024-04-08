package calypsox.tk.report;

import calypsox.tk.anacredit.formatter.AnacreditFormatterRepo;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

public class AnacreditRepoReportStyle extends AnacreditTradeReportStyle {


    public static final String ANACREDIT_REPO_TYPE = "Anacredit Repo Type";

    @Override
    public Object getColumnValue(ReportRow reportRow, String columnId, Vector errors) throws InvalidParameterException {


        if (ANACREDIT_REPO_TYPE.equals(columnId)) {
            Trade trade = (Trade)reportRow.getProperty("Trade");
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            if (trade != null)   {
                if (trade.getProduct() instanceof Repo) {
                    if (AnacreditFormatterRepo.isRepoRPPLZ(trade, valDatetime)) {
                        return "RPPLZ";
                    }  else if ( trade.computeNominal(valDatetime.getJDate(TimeZone.getDefault())) < 0) {
                        return "CTA";
                    } else {
                        return "ATA";
                    }
                }
            }
        }

        return  super.getColumnValue(reportRow, columnId, errors);
    }

    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(ANACREDIT_REPO_TYPE);
        return  treeList;

    }

}
