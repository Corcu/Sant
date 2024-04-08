package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.Vector;

public class BODisponibleMicCustodioReport extends BODisponibleSecurityPositionReport {

    @Override
    public ReportOutput load(Vector errorMsgs) {
        StandardReportOutput output = new StandardReportOutput(this);
        DefaultReportOutput coreOutput = (DefaultReportOutput)super.load(errorMsgs);
        if (coreOutput != null && !Util.isEmpty(coreOutput.getRows())) {
            Arrays.stream(coreOutput.getRows()).parallel()
                    .forEach(this::enrichReportRow);
            output.setRows(coreOutput.getRows());
        }
        return output;
    }

    private void enrichReportRow(ReportRow row) {
        //TODO
    }

    private LegalEntity loadIssuer(Product product){
        if (product instanceof Bond) {
            return BOCache.getLegalEntity(DSConnection.getDefault(), ((Bond) product).getIssuerId());
        }else if (product instanceof Equity) {
            Equity equity = (Equity) product;
            LegalEntity le = equity.getIssuer();
            return le;
        }
        return null;
    }

}
