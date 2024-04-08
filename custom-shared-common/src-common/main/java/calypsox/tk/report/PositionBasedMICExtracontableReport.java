package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableInventoryBuilder;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Security;
import com.calypso.tk.report.BOSecurityPositionReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class PositionBasedMICExtracontableReport extends BOSecurityPositionReport {

    static final String ROW_PROP_NAME = "MIC_DATA";

    @Override
    public ReportOutput load(Vector errorMsgs){
        ReportOutput output=super.load(errorMsgs);

        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        standardReportOutput.setRows(Optional.ofNullable(output)
            .map(outputOpt -> ((DefaultReportOutput) outputOpt).getRows()).orElse(new ReportRow[0]));
        initMICBeans(standardReportOutput);

        return standardReportOutput;
}


    private void initMICBeans(DefaultReportOutput output) {
        Arrays.stream(output.getRows()).parallel().forEach(this::enrichReportRow);
    }

    private void enrichReportRow(ReportRow originalReportRow) {
        Inventory position = originalReportRow.getProperty(Inventory.class.getSimpleName());
        MICExtracontableInventoryBuilder builder = null;
        if (position != null) {
            builder = new MICExtracontableInventoryBuilder(position);
            originalReportRow.setProperty(ROW_PROP_NAME, builder.build());
            setSecurity(position,originalReportRow);
        }
    }

    private void setSecurity(Inventory position,ReportRow reportRow){
        if(position instanceof InventorySecurityPosition){
            int productId=((InventorySecurityPosition)position).getSecurityId();
            try {
                Product product= DSConnection.getDefault().getRemoteProduct().getProduct(productId);
                if(product instanceof Security){
                    reportRow.setProperty("Product",product);
                }
            } catch (CalypsoServiceException e) {
                e.printStackTrace();
            }
        }
    }

}
