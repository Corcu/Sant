package calypsox.tk.report;

import calypsox.tk.bo.boi.BOIDiarioBean;
import calypsox.tk.bo.boi.BOIDiarioBuilderBond;
import calypsox.tk.bo.boi.BOIDiarioBuilderRepo;
import calypsox.tk.bo.boi.BOIDiarioMsgBuilder;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dmenendd
 * A file containing info trade is generated to be sent to BOI system.
 */
public class BOIDiarioReport extends TradeReport {

    /**
     * @param errorMsgs
     * @return
     */
    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

        if (null != output){
            List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());
            final List<BOIDiarioBean> newRows = createNewRows(rows);
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
    private List<BOIDiarioBean> createNewRows(List<ReportRow> rows){
        List<BOIDiarioBean> newRows = new ArrayList<>();
        if(Optional.ofNullable(rows).isPresent()){
            newRows = rows.stream().parallel().map(this::buildBeans).filter(list -> !Util.isEmpty(list)).flatMap(List::stream).collect(Collectors.toList());
        }
        return newRows;
    }

    /**
     * @param row
     * @return
     */
    private List<BOIDiarioBean> buildBeans(ReportRow row){
        return Optional.ofNullable(BOIDiarioBeanFactory.getBuilderClass(row,getValDate()))
                .map(builder->builder.build(getPricingEnv()))
                .orElse(new ArrayList<>());
    }

    private static class BOIDiarioBeanFactory{

        static BOIDiarioMsgBuilder getBuilderClass(ReportRow row, JDate valDate){
            BOIDiarioMsgBuilder builder=null;
            Trade trade=row.getProperty("Default") instanceof Trade ? row.getProperty("Default") : null;
            if(trade!=null){
                if(trade.getProduct() instanceof Repo){
                    builder=new BOIDiarioBuilderRepo(trade,valDate);
                }else if(trade.getProduct() instanceof PerformanceSwap){
                    builder=new BOIDiarioBuilderRepo(trade,valDate);
                }else if(trade.getProduct() instanceof Bond){
                    builder=new BOIDiarioBuilderBond(trade,valDate);
                }
            }
            return builder;
        }

    }
}