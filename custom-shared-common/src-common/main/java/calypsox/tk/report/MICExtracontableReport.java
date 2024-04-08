package calypsox.tk.report;

import calypsox.tk.report.extracontable.*;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.*;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.sqlbindnig.UploaderSQLBindVariable;
import com.calypso.tk.upload.util.UploaderSQLBindAPI;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.TradeArray;
import org.jfree.util.Log;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aalonsop
 */
public class MICExtracontableReport extends TradeReport {

    static final String ROW_PROP_NAME = "MIC_DATA";
    private static final long serialVersionUID = -1655127533046540816L;
    private List<ReportRow> clonedRows = new ArrayList<>();

    @Override
    public ReportOutput load(Vector errorMsgs) {

        ReportOutput output = super.load(errorMsgs);


        //Load Canceled trades on D and generate their rows
        if (null != output) {
            TradeArray canceledTrades = loadCanceledTrades();
            createRowsFromCanceled(canceledTrades, output);
        }

        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        standardReportOutput.setRows(Optional.ofNullable(output)
                .map(outputOpt -> ((DefaultReportOutput) outputOpt).getRows()).orElse(new ReportRow[0]));
        initMICBeans(standardReportOutput);

        standardReportOutput.setRows(Stream.of(standardReportOutput.getRows(), clonedRows.toArray())
                .flatMap(Arrays::stream)
                .toArray(ReportRow[]::new));

        return standardReportOutput;
    }


    private void initMICBeans(DefaultReportOutput output) {
        //clear cloneRows
        clonedRows = new ArrayList<>();
        Arrays.stream(output.getRows()).parallel().forEach(this::enrichReportRow);
    }

    private void enrichReportRow(ReportRow originalReportRow) {
        Trade trade = originalReportRow.getProperty(Trade.class.getSimpleName());
        MICExtracontableTradeBuilder builder = null;
        if (trade != null) {
            builder = getMicExtracontableBuilder(trade);
            originalReportRow.setProperty(ROW_PROP_NAME, builder.build());

            //Clone the row when the trade got C&R on trade date
            cloneRowWithOldPartenon(trade, originalReportRow);
        }
    }

    private void cloneRowWithOldPartenon(Trade trade, ReportRow originalReportRow) {
        if (isAcceptedProductToClone(trade)) {
            Vector v = getTradeAudit(trade);
            if (null != v) {
                List<AuditValue> auditValues = checkPartenonAuditChange(v);
                auditValues.forEach(s -> createCloneRowWithOldPartenon(originalReportRow, trade, s));
            }
        }
    }

    private MICExtracontableTradeBuilder getMicExtracontableBuilder(Trade trade) {
        MICExtracontableTradeBuilder builder = null;

        if (trade.getProduct() instanceof SecLending) {
            builder = new MICExtracontablePDVBuilder(trade);
        } else if (trade.getProduct() instanceof PerformanceSwap) {
            builder = new MICExtracontableBRSBuilder(trade);
        } else if (trade.getProduct() instanceof Equity) {
            builder = new MICExtracontableEquityBuilder(trade);
        } else if (trade.getProduct() instanceof Repo) {
            builder = new MICExtracontableRepoBuilder(trade);
        }else if (trade.getProduct() instanceof Pledge) {
            builder = new MICExtracontablePledgeBuilder(trade);
        }else if (trade.getProduct() instanceof Bond) {
            builder = new MICExtracontableBondBuilder(trade);
        }else if (trade.getProduct() instanceof CA && trade.getProduct().getUnderlyingProduct() instanceof Bond){
            builder = new MICExtracontableCARFBuilder(trade);
        }
        return builder;
    }

    private Boolean isAcceptedProductToClone(Trade trade) {
        String partenon = trade.getKeywordValue("PartenonAccountingID");
        String oldPartenon = trade.getKeywordValue("OldPartenonAccountingID");
        if (Util.isEmpty(partenon) || Util.isEmpty(oldPartenon)) return false;
        return !partenon.equals(oldPartenon);
    }

    private Vector getTradeAudit(Trade trade) {
        Vector v = null;
        JDatetime startDay = new JDatetime(getIntervalDayDate(getValuationDatetime()).getStart().toDate());
        JDatetime endDay = new JDatetime(getIntervalDayDate(getValuationDatetime()).getEnd().toDate());
        try {
            List<UploaderSQLBindVariable> bindVariablesList = new ArrayList();
            DataUploaderUtil.valueToPreparedString(CalypsoIDAPIUtil.getId(trade), bindVariablesList);
            DataUploaderUtil.valueToPreparedString(startDay, bindVariablesList);
            DataUploaderUtil.valueToPreparedString(endDay, bindVariablesList);

            String w = "entity_id =? and modif_date BETWEEN ? AND  ?";
            v = UploaderSQLBindAPI.getAudit(w, " version_num DESC", bindVariablesList);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName() + "Can't retrieve the trade audit: " + trade.getLongId(), e);
        }
        return v;
    }

    private List<AuditValue> checkPartenonAuditChange(Vector v) {
        List<AuditValue> auditValues = Collections.list(v.elements());
        auditValues = auditValues.stream().filter(s -> s.getField().getName().equals("DELKEY#PartenonAccountingID")).collect(Collectors.toList());
        return auditValues;
    }

    private void createCloneRowWithOldPartenon(ReportRow originalReportRow, Trade trade, AuditValue auditValue) {
        ReportRow row = originalReportRow.clone();
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getRolledUpTrade(trade.getLongId(), auditValue.getVersion() - 1);
            MICExtracontableTradeBuilder builder = getMicExtracontableBuilder(trade);
            row.setProperty(ROW_PROP_NAME, builder.build());
            clonedRows.add(row);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), e);
        }
    }

    private Interval getIntervalDayDate(JDatetime valDate) {
        int day = valDate.getJDate(TimeZone.getDefault()).getDayOfMonth();
        int month = valDate.getJDate(TimeZone.getDefault()).getMonth();
        int year = valDate.getJDate(TimeZone.getDefault()).getYear();
        DateTime start = new DateTime(year, month, day, 0, 0, 0, 0);
        DateTime end = new DateTime(year, month, day, 23, 59, 59, 59);
        return new Interval(start, end);
    }

    private TradeArray loadCanceledTrades() {
        TradeArray trades = null;
        TradeFilter tfilter = getTradeFilterByProduct();
        if (null != tfilter) {
            try {
                trades = DSConnection.getDefault().getRemoteTrade().getTrades(tfilter, getValuationDatetime());
            } catch (CalypsoServiceException e) {
                Log.error("Cant retrieve the trades from trade filter: " + tfilter.getName());
            }
        }
        return Optional.ofNullable(trades).orElse(new TradeArray());
    }

    private TradeFilter getTradeFilterByProduct() {
        String tfName = "";
        TradeFilter tfilter = null;
        String productType = getReportTemplate().get("ProductType");

        if (!Util.isEmpty(productType)) {
            tfName = productType + "CanceledIK";
        }
        if (!Util.isEmpty(tfName)) {
            tfilter = BOCache.getTradeFilter(DSConnection.getDefault(), tfName);
        }
        if (tfilter == null) {
            Log.info("Cant find any trade filter with name: " + tfName);
        }
        return tfilter;
    }

    private void createRowsFromCanceled(TradeArray trades, ReportOutput output) {
        List<ReportRow> rows = new ArrayList<>();
        trades.forEach(s->this.fillRow((Trade) s, rows));
        ((DefaultReportOutput) output).setRows(Stream.of(((DefaultReportOutput) output).getRows(), rows.toArray())
                .flatMap(Arrays::stream)
                .toArray(ReportRow[]::new));
    }

    private void fillRow(Trade trade, List<ReportRow> rows){
        ReportRow row = new ReportRow(trade);
        row.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
        row.setProperty(ReportRow.PRICING_ENV, getPricingEnv());
        rows.add(row);
    }

}
