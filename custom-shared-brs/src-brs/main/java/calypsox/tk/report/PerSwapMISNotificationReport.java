package calypsox.tk.report;

import calypsox.tk.bo.mis.PerSwapMisBean;
import calypsox.tk.bo.mis.PerSwapMisBeanBuilder;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author acd
 */
public class PerSwapMISNotificationReport extends TradeReport {

    /**
     * @param errorMsgs
     * @return
     */
    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

        if (null != output){
            List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());
            final List<PerSwapMisBean> newRows = createNewRows(rows);

            for(PerSwapMisBean row : newRows){
                if(null!=row.getSpread()&&!Util.isEmpty(row.getSpread())) {
                    double spread = Double.parseDouble(row.getSpread()) * 100;
                    row.setSpread(Double.toString(spread));
                }
            }
            final ReportRow[] reportRows = newRows.stream().map(ReportRow::new).toArray(ReportRow[]::new);

            output.clear();
            output.setRows(reportRows);
        }
        return output;
    }

    /**
     * @param rows
     * @return
     */
    private List<PerSwapMisBean> createNewRows(List<ReportRow> rows){
        List<PerSwapMisBean> newRows = new ArrayList<>();
        if(Optional.ofNullable(rows).isPresent()){
            newRows = rows.stream().parallel().map(this::buildBean).filter(list -> !Util.isEmpty(list)).flatMap(List::stream).collect(Collectors.toList());
        }
        return newRows;
    }

    /**
     * @param row
     * @return
     */
    private List<PerSwapMisBean> buildBean(ReportRow row){
        List<PerSwapMisBean> beans = new ArrayList<>();
        final Optional opTrade = row.getProperty("Default") instanceof Trade ? Optional.ofNullable(row.getProperty("Default")) : Optional.empty();
        if(opTrade.isPresent()){
            PerSwapMisBeanBuilder builder = new PerSwapMisBeanBuilder((Trade)opTrade.get(),getValDate());
            beans = builder.build(getPricingEnv());
            if(!Util.isEmpty(beans)){
                for(PerSwapMisBean b : beans){
                    b.setTrade((Trade)opTrade.get());
                }
            }
        }
        return beans;
    }
}
