package calypsox.tk.report;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Rate;
import com.calypso.tk.report.CashFlowReportStyle;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;

public class RepoFOBOFlowsReportStyle extends CashFlowReportStyle {

    public static final String CONTRACTID = "CONTRACTID";
    public static final String ROOTCONTRACT = "ROOTCONTRACT";
    public static final String EXTERNAL = "EXTERNALNUMBER";
    public static final String INDICE_DE_REFERNCIA = "INDICE_DE_REFERNCIA";

    public static final String NOMINAL = "NOMINAL";
    public static final String IMPORTE = "IMPORTE";
    public static final String TIPO = "TIPO";


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        RepoFOBOFlowsBean bean = Optional.ofNullable(row.getProperty(RepoFOBOFlowsBean.class.getName())).map(o -> (RepoFOBOFlowsBean) o).orElse(new RepoFOBOFlowsBean());

        if (CONTRACTID.equalsIgnoreCase(columnId)) {
            return bean.getContractId();
        }else if (ROOTCONTRACT.equalsIgnoreCase(columnId)) {
            return bean.getRootContractId();
        }else if (EXTERNAL.equalsIgnoreCase(columnId)) {
            return bean.getExternalNumber();
        }else if (NOMINAL.equalsIgnoreCase(columnId)) {
            return getValue(row, "Cash Flow Notional", errors);
        }else if (IMPORTE.equalsIgnoreCase(columnId)) {
            return getValue(row, "Interest Amt", errors);
        }else if (TIPO.equalsIgnoreCase(columnId)) {
            return getValue(row, "Rate", errors);
        }else if (INDICE_DE_REFERNCIA.equalsIgnoreCase(columnId)) {
            return bean.getReferenceIndex();
        }else {
            return super.getColumnValue(row, columnId, errors);
        }
    }

    private String getValue(ReportRow row,String columnName, Vector errors){
        String value = "0.00";
        Object columnValue = super.getColumnValue(row, columnName, errors);
        if(columnValue instanceof Amount){
            value = SecFinanceTradeUtil.getInstance().formatValue(((Amount)columnValue).get(),"0.00");
        }
        if(columnValue instanceof Rate){
            value = SecFinanceTradeUtil.getInstance().formatValue(((Rate)columnValue).get()*100,"");
        }
        return value;
    }

}
