package calypsox.tk.report;

import com.calypso.tk.core.Product;
import com.calypso.tk.report.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author aalonsop
 */
public class SecurityReport extends ProductReport {


    public ReportOutput load(Vector errorMsgs) {
        BondReport bondReport = new BondReport();
        bondReport.setReportTemplate(((SecurityReportTemplate) this.getReportTemplate()).getBondReportTemplate());
        DefaultReportOutput bondOutput = (DefaultReportOutput) bondReport.load(errorMsgs);
        EquityReport eqReport = new EquityReport();
        eqReport.setReportTemplate(((SecurityReportTemplate) this.getReportTemplate()).getEquityReportTemplate());
        DefaultReportOutput equityOutput= (DefaultReportOutput) eqReport.load(errorMsgs);

        DefaultReportOutput mergedOutput=new DefaultReportOutput(this);
        mergedOutput.setRows(mergeContents(bondOutput.getRows(),equityOutput.getRows()));
        return mergedOutput;
    }

    private ReportRow[] mergeContents(ReportRow[] bondRows,ReportRow[] equityRows){
        Set<String> isinSet = new HashSet<>();
        ReportRow[] rows= Stream.concat(Arrays.stream(bondRows), Arrays.stream(equityRows))
                .filter(r->isValidIsin(((Product)r.getProperty("Product")).getSecCode("ISIN")))
                .filter(r->isinSet.add(((Product)r.getProperty("Product")).getSecCode("ISIN").trim()))
                .toArray(size -> (ReportRow[]) Array.newInstance(ReportRow.class, size));
        return rows;
    }

    private boolean isValidIsin(String isin){
        Pattern pattern = Pattern.compile("([A-Z]{2})([A-Z0-9]{9})([0-9]{1})");
        Matcher matcher = pattern.matcher(Optional.ofNullable(isin).orElse(""));
        return matcher.matches();
    }
    private boolean isIsinFiltered(){
        return Boolean.parseBoolean(this.getReportTemplate().get("FILTER_DUPLICATED_ISIN"));
    }

}
