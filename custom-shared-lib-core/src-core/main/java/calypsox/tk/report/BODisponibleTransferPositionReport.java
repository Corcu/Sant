package calypsox.tk.report;


import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SortColumn;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * @author acd
 */
public class BODisponibleTransferPositionReport extends BODisponibleSecurityPositionReport  {
    HashMap<String,BODisponibleTransferPositionBean> groupByKey = new HashMap<>();

    public static final String ISIN  = "PRODUCT_CODE.ISIN";
    public static final String AGENT  = "Agent";
    @Override
    public ReportOutput load(Vector errorMsgs) {
        setRowSort();
        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

        if (null != output) {
            groupByKey.clear();

            //Group positions by calculated key
            String lastColumnDateValue = Arrays.stream(getReportTemplate().getColumns()).reduce((a, b) -> b).orElse("");
            Arrays.stream(output.getRows()).forEach(reportRow -> {
                double total = getInventoryTotal(reportRow,lastColumnDateValue);
                String rowKey = getGroupingKey(reportRow);

                if(!Util.isEmpty(rowKey)){
                    BODisponibleTransferPositionBean boDisponibleTransferPositionBean = groupByKey.computeIfAbsent(rowKey, BODisponibleTransferPositionBean::new);
                    if (total < 0.0D) {
                        boDisponibleTransferPositionBean.addOnNegativeList(reportRow);
                    } else {
                        boDisponibleTransferPositionBean.addOnPositiveList(reportRow);
                    }
                }
            });

            //Clean empty shorts positions
            groupByKey.entrySet().removeIf(entry -> {
                BODisponibleTransferPositionBean bean = entry.getValue();
                return null!=bean && bean.getNegativeList().isEmpty();
            });

            //Set default selection and colors for positions groups
            ConcurrentLinkedQueue<ReportRow> finalRowList = new ConcurrentLinkedQueue<>();
            groupByKey.values().parallelStream().forEach(bean -> {
                List<ReportRow> negativeList = bean.getNegativeList();
                if(!Util.isEmpty(negativeList)){
                    finalRowList.addAll(negativeList);
                    List<ReportRow> positiveList = bean.getPositiveList();
                    if(!Util.isEmpty(positiveList)){
                        setDefaultSelectionAndColor(negativeList,lastColumnDateValue,"To");
                        setDefaultSelectionAndColor(positiveList,lastColumnDateValue,"From");
                        finalRowList.addAll(positiveList);
                    }
                }
            });

            output.setRows(finalRowList.toArray(new ReportRow[finalRowList.size()]));
        }

        return output;
    }

    /**
     * @param row ReportRow
     * @return Calculated key for group the positions
     */
    private String getGroupingKey(ReportRow row){
        if(null!=row.getProperty("Inventory") && row.getProperty("Inventory") instanceof InventorySecurityPosition){
            InventorySecurityPosition inventory = (InventorySecurityPosition) row.getProperty("Inventory");
            String isin = null!=inventory.getProduct() ? inventory.getProduct().getSecCode("ISIN") : "";
            //String agent = null!=inventory.getAgent() ? inventory.getAgent().getCode() : "";
            return isin;
        }
        return "";
    }

    /**
     * Selects by default the max positive and min negative for the grouping.
     * @param rowList ReportRowList
     * @param lastColumnDateValue column with endDate
     * @param direction direction From or To
     */
    private void setDefaultSelectionAndColor(List<ReportRow> rowList, String lastColumnDateValue, String direction){
        ReportRow row = null;
        if("From".equalsIgnoreCase(direction)){
            row = rowList.stream().max(Comparator.comparing(r -> ((Amount) r.getProperty(lastColumnDateValue)).get())).get();
        }else {
            row = rowList.stream().min(Comparator.comparing(r -> ((Amount) r.getProperty(lastColumnDateValue)).get())).get();
        }
        setRowProperties(row,direction);
    }

    private void setRowProperties(ReportRow row,String direction){
        row.setProperty("SelectedOption",direction);
        row.setProperty("ColorSet", true);
        Color rowColor = "From".equalsIgnoreCase(direction) ? new Color(22, 155, 2, 51) : new Color(245, 102, 102, 51);
        row.setProperty("ColorByRow", rowColor);
    }

    private double getInventoryTotal(ReportRow reportRow,String lastColumnDateValue){
        Amount total = new Amount(0.0);
        if(getReportTemplate().getColumns().length>=1){
            BODisponibleTransferPositionReportStyle style = new BODisponibleTransferPositionReportStyle();
            total = Optional.ofNullable(style.getColumnValue(reportRow, lastColumnDateValue, new Vector())).filter(Amount.class::isInstance).map(Amount.class::cast).orElse(new Amount(0));
            reportRow.setProperty(lastColumnDateValue,total);
        }
        return total.get();
    }



    /**
     * Sort column by Last position amount
     */
    private void setRowSort() {
        String lasDateColumnName = getLasDateColumnName();
        SortColumn sortByISIN = new SortColumn("PRODUCT_CODE.ISIN", true, false);
        SortColumn sortByDateColumn = new SortColumn(lasDateColumnName, true, false);
        getReportTemplate().setSortColumns(Stream.of(sortByISIN,sortByDateColumn).toArray(SortColumn[]::new));
    }

    private String getLasDateColumnName(){
        String[] columnNames = getReportTemplate().getColumnNames();
        if(!Util.isEmpty(columnNames)){
            for(String columnName : columnNames){
                JDate date = Util.MStringToDate(columnName, this.getReportPanel().getLocale(), true);
                if(null!=date){
                    return columnName;
                }
            }
        }
        return "";
    }

    public HashMap<String,BODisponibleTransferPositionBean> getGroupByKey(){
        return groupByKey;
    }

}
