package calypsox.tk.report;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.Vector;

/**
 * AccountEnrichmentReport
 * @author x933435
 */
public class AccountEnrichmentReport extends com.calypso.tk.report.AccountEnrichmentReport {

    private static final long serialVersionUID = 5798616790347784204L;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        ReportOutput out = super.load(errorMsgs);
        if(out instanceof DefaultReportOutput) {
            Boolean b = this.getReportTemplate().get(AccountEnrichmentReportTemplate.loadCreTransfer);
            if (b != null && b) {
                ReportRow[] rows = ((DefaultReportOutput) out).getRows();
                if(rows != null){
                    for(ReportRow r: rows){
                        BOCre cre = r.getProperty(ReportRow.ACCOUNT_ENRICHMENT);
                        if(cre != null){
                            BOTransfer xfer = getBOTransfer(cre.getTransferLongId());
                            if(xfer != null){
                                r.setProperty(ReportRow.TRANSFER, xfer);
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    /**
     * Get the BOTransfer by id
     *
     * @param transferId the transfer id
     * @return the BOTransfer
     */
    private BOTransfer getBOTransfer(long transferId) {
        if (transferId > 0L) {
            try {
                return DSConnection.getDefault().getRemoteBackOffice().getBOTransfer(transferId);
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }
        return null;
    }
}
