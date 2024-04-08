package calypsox.tk.report;

import com.calypso.tk.core.Product;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class SecurityReportStyle extends ProductReportStyle {


    public static final String DEFAULT_IDENTIFIER_SCHEME="identifierScheme";
    public static final String ISIN_IDENTIFIER="identifier";
    public static final String EMPTY_BC="BC";
    public static final String EMPTY_CLIENTREF="clientReference";



    private static final long serialVersionUID = 6216200284059440314L;

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {
        Object columnValue;
        Product product = row.getProperty("Product");
        if (DEFAULT_IDENTIFIER_SCHEME.equalsIgnoreCase(columnId)) {
            columnValue = "ISIN";
        } else if (ISIN_IDENTIFIER.equalsIgnoreCase(columnId)) {
            columnValue = product.getSecCode("ISIN");
        } else if (EMPTY_BC.equalsIgnoreCase(columnId) || EMPTY_CLIENTREF.equalsIgnoreCase(columnId)) {
            columnValue = "";
        }else{
            columnValue=super.getColumnValue(row,columnId,errors);
        }
        return columnValue;
    }
}
