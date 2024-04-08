package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * @author Juan Angel Torija D?az
 */

@SuppressWarnings("serial")
public class ExportFactorScheduleConciliationReport extends BondReport {

    public static final String EXPORT_FACTORSCHEDULECONCILIATION_REPORT = "ExportFactorScheduleConciliationReport";

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
            Vector<Bond> vBonds = dsConn.getRemoteProduct().getAllProducts(null, "product_type in ('BondAssetBacked')", null);
            if (vBonds != null) {
                // process them, one per one
                for (int i = 0; i < vBonds.size(); i++) {
                    //JRL & JTD 11/04/2016 Migration 14.4
                    if (vBonds.get(i).getSecCodes() != null) {
                        Product p = BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(), "ISIN", vBonds.get(i).getSecCodes().get("ISIN").toString());
                        if ((p != null) && (p instanceof BondAssetBacked)) {
                            BondAssetBacked abs = new BondAssetBacked();
                            abs = (BondAssetBacked) p;
                            final Vector<ExportFactorScheduleConciliationItem> exp_FactorScheduleConciliationItem = ExportFactorScheduleConciliationLogic
                                    .getReportRows(abs, reportTemp.getValDate().toString(), dsConn, errorMsgsP);
                            for (int j = 0; j < exp_FactorScheduleConciliationItem.size(); j++) {
                                final ReportRow repRow = new ReportRow(vBonds.get(i), "Product");
                                repRow.setProperty("ExportFactorScheduleConciliationItem",
                                        exp_FactorScheduleConciliationItem.get(j));
                                reportRows.add(repRow);
                            }
                        }
                    }
                }
            }
            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ExportFactorScheduleConciliationReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
        }

        return null;
    }
}
