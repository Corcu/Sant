package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import com.calypso.tk.core.Log;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * @author Juan Angel Torija Díaz
 */

@SuppressWarnings("serial")
public class ExportBondConciliationReport extends BondReport {

    public static final String EXPORT_BONDCONCILIATION_REPORT = "ExportBondConciliationReport";

    @SuppressWarnings("unchecked")
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        final DSConnection dsConn = getDSConnection();
        final ReportTemplate reportTemp = getReportTemplate();

        // Main process
        try {
            // get all bonds from system
            Vector<Bond> vBonds = dsConn.getRemoteProduct().getAllProducts(null, "product_type in ('Bond', 'BondAssetBacked')", null);
            if (vBonds != null) {
                // process them, one per one
                for (int i = 0; i < vBonds.size(); i++) {
                    final Vector<ExportBondConciliationItem> exp_BondConciliationItem = ExportBondConciliationLogic
                            .getReportRows(vBonds.get(i), reportTemp.getValDate().toString(), dsConn, errorMsgsP);
                    for (int j = 0; j < exp_BondConciliationItem.size(); j++) {
                        final ReportRow repRow = new ReportRow(vBonds.get(i), "Product");
                        repRow.setProperty("ExportBondConciliationItem", exp_BondConciliationItem.get(j));
                        reportRows.add(repRow);
                    }
                }
            }
            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ExportConciliationReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
        }

        return null;
    }
}
