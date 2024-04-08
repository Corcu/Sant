package calypsox.tk.report;

import com.calypso.tk.product.Equity;
import com.calypso.tk.report.BondReportTemplate;
import com.calypso.tk.report.EquityReportTemplate;
import com.calypso.tk.report.ProductReportTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aalonsop
 */
public class SecurityReportTemplate extends ProductReportTemplate {

    BondReportTemplate bondReportTemplate;
    EquityReportTemplate<Equity> equityReportTemplate;

    public SecurityReportTemplate(){
        bondReportTemplate=new BondReportTemplate();
        equityReportTemplate=new EquityReportTemplate<>();
    }

    public void setDefaults() {
        List<String> columns = new ArrayList<>();
        columns.add("identifierScheme");
        columns.add("identifier");
        columns.add("BC");
        columns.add("clientReference");

        this.resetColumns();
        this.setColumns(columns.toArray(new String[0]));

    }

    public BondReportTemplate getBondReportTemplate() {
        return this.bondReportTemplate;
    }

    public EquityReportTemplate<Equity> getEquityReportTemplate(){
        return this.equityReportTemplate;
    }
}
