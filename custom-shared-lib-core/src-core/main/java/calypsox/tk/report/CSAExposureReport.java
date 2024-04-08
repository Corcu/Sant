package calypsox.tk.report;

import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantCalypsoUtilities;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author fperezur..... v2.0 by aalonsop: Parallel stream processing added
 */
public class CSAExposureReport extends TradeReport implements CheckRowsNumberReport {

    public static final String CSA_EXPOSURE_REPORT = "CSAExposureReport";
    protected static final String ROW_PROPERTY_TRADE_ID = "Trade ID";
    protected static final String ROW_PROPERTY_COMPRESSED_TRADE = "Compressed Trade";

    private static final String CONTRACT_TYPE_STR = "CONTRACT_TYPE";
    private static final String OWNER_COLL_EXP_ATTR = "OWNER";
    private static final int NUM_TRADES_PER_STEP = 999;
    private static final long serialVersionUID = 4508600603653607480L;

    private CSAExposureLogic exposureLogic = new CSAExposureLogic();

    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {
        final DefaultReportOutput output = new StandardReportOutput(this);
        final PricingEnv pricingEnv = getPricingEnv();
        final ReportTemplate reportTemp = getReportTemplate();
        final JDate jdate = reportTemp.getValDate();

        final String verifiedTradesWhereClause =
                "product_desc.product_type = 'CollateralExposure' "
                        + "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
                        + " AND trade.trade_status = 'VERIFIED'"
                        + " AND trunc(trade.trade_date_time) <= "
                        + Util.date2SQLString(jdate);

        final String maturedTradesWhereClause = "product_desc.product_type = 'CollateralExposure' "
                + "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
                + " AND (trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value " +
                "where pl_mark.MARK_ID = pl_mark_value.MARK_ID AND trade.trade_id = pl_mark.trade_id AND " +
                "pl_mark_value.mark_name = 'NPV_BASE' AND pl_mark_value.mark_value != 0 AND " +
                "trunc(pl_mark.valuation_date) = "
                + Util.date2SQLString(jdate) + "))"
                + " AND trunc(trade.trade_date_time) <= "
                + Util.date2SQLString(jdate);
        /*
        final String tradesWhereClauseOld =
                "product_desc.product_type = 'CollateralExposure' "
                        + "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
                        + " AND (trade.trade_status IN 'VERIFIED' OR "
                        + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value "
                        + "where pl_mark.MARK_ID = pl_mark_value.MARK_ID AND trade.trade_id = pl_mark.trade_id AND "
                        + "pl_mark_value.mark_name = 'NPV_BASE' AND pl_mark_value.mark_value != 0 AND "
                        + "trunc(pl_mark.valuation_date) = "
                        + Util.date2SQLString(jdate)
                        + ")))"
                        + " AND trunc(trade.trade_date_time) <= "
                        + Util.date2SQLString(jdate);
        */
        // COL_OUT_016
        // Changed by Carlos Cejudo:
        // Only the ids for the needed trades are retrieved from the
        // database. Once we have every id trades are
        // taken in steps, retrieving a number of them each time. With those
        // trades their corresponding report rows
        // are generated. In the next step the next group of trades is taken
        // and processed. This improves the
        // performance of this process.

        Date iniSelect = new Date();
        Log.info(
                this,
                ">>>>>>>>>> LOG Optimizacion Procesos: Inicio de la consulta ("
                        + iniSelect
                        + ") <<<<<<<<<<");

        long[] tradeIdsArray = new long[0];
        try {
            tradeIdsArray = joinAllTradeIds(verifiedTradesWhereClause, maturedTradesWhereClause);
        } catch (ExecutionException exc) {
            Log.error(this.getClass().getSimpleName(), "Exception while retrieving tradeIds", exc.getCause());
        }
        /*try {
            tradeIdsArray = DSConnection.getDefault().getRemoteTrade().getTradeIds(null, tradesWhereClause, 0, 0, null);
        } catch (CalypsoServiceException exc) {
            Log.error(this.getClass().getSimpleName(), "Exception while retrieving tradeIds", exc.getCause());
        }*/

        //long[] tradeIdsArray = {14963342};

        Date finSelect = new Date();
        Log.info(
                this,
                ">>>>>>>>>> LOG Optimizacion Procesos: Se ha tardado  "
                        + (finSelect.getTime() - iniSelect.getTime())
                        + " milisecs en obtener "
                        + tradeIdsArray.length
                        + " resultados ("
                        + finSelect
                        + ")) <<<<<<<<<<");
        List<ReportRow> reportRows = new ArrayList<>();
        //Does the magic
        //chunkTradeIdArray(tradeIdsArray, NUM_TRADES_PER_STEP).parallel().map(ids -> this.getReportRowsFromTradeIds(ids, jdate, reportTemp, errorMsgsP, pricingEnv)).forEachOrdered(reportRows::addAll);
        List<long[]> chunkedTradeIds = chunkTradeIdArray(tradeIdsArray, NUM_TRADES_PER_STEP);
        for (long[] chunk : chunkedTradeIds) {
            List<ReportRow> chunkRows = getReportRowsFromTradeIds(chunk, jdate, reportTemp, errorMsgsP, pricingEnv);
            reportRows.addAll(chunkRows);
        }
        ReportRow[] reportRowArray = new ReportRow[reportRows.size()];
        output.setRows(reportRows.toArray(reportRowArray));

        //Generate a task exception if the number of rows is out of an umbral defined
        HashMap<String , String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
        checkAndGenerateTaskReport(output, value);

        return output;
    }

    private long[] joinAllTradeIds(String whereVerified, String whereMatured) throws ExecutionException {
        CompletableFuture<long[]> verifiedTradesFuture = CompletableFuture.supplyAsync(getTradesIdsByWhereClause(whereVerified));
        CompletableFuture<long[]> maturedTradesFuture = CompletableFuture.supplyAsync(getTradesIdsByWhereClause(whereMatured));
        CompletableFuture<long[]> allTradesIdsFuture = verifiedTradesFuture.thenCombine(maturedTradesFuture, this::mergeArray);

        long[] allTradeIds = new long[0];
        try {
            allTradeIds = allTradesIdsFuture.get();
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(exc);
        }
        return allTradeIds;
    }

    /**
     * @param verifiedTradeIds
     * @param maturedTradeIds
     * @return
     */
    private long[] mergeArray(long[] verifiedTradeIds, long[] maturedTradeIds) {
        return LongStream.concat(Arrays.stream(verifiedTradeIds), Arrays.stream(maturedTradeIds)).toArray();
    }


    /**
     * @param where
     * @return
     */
    private Supplier<long[]> getTradesIdsByWhereClause(String where) {
        return () -> {
            long[] tradeIdsArray = new long[0];
            try {
                tradeIdsArray = DSConnection.getDefault().getRemoteTrade().getTradeIds(null, where, 0, 0, null, null);
            } catch (CalypsoServiceException exc) {
                Log.error(this.getClass().getSimpleName(), "Exception while retrieving tradeIds", exc.getCause());
            }
            return tradeIdsArray;
        };
    }

    /**
     * @param tradeIdArray
     * @param chunkSize
     * @return
     */
    public List<long[]> chunkTradeIdArray(long[] tradeIdArray, int chunkSize) {
        int numOfChunks = (int) Math.ceil((double) tradeIdArray.length / chunkSize);
        List<long[]> chunkedArrays = new ArrayList<>();
        for (int i = 0; i < numOfChunks; ++i) {
            int start = i * chunkSize;
            int length = Math.min(tradeIdArray.length - start, chunkSize);
            long[] temp = new long[length];
            System.arraycopy(tradeIdArray, start, temp, 0, length);
            chunkedArrays.add(temp);
        }
        //return chunkedArrays.stream();
        return chunkedArrays;
    }

    /**
     * @param streamChunk
     * @param jdate
     * @param reportTemp
     * @param errorMsgsP
     * @param pricingEnv
     * @return
     */
    private List<ReportRow> getReportRowsFromTradeIds(long[] streamChunk, JDate jdate, ReportTemplate reportTemp, Vector errorMsgsP, PricingEnv pricingEnv) {
        List<ReportRow> reportRows = Collections.emptyList();
        try {
            TradeArray tradeArray = SantCalypsoUtilities.getInstance().getTradesWithTradeFilter(streamChunk);
            List<Trade> derivTrades = filterTrades(tradeArray, jdate, reportTemp);
            reportRows = getReportRows(DSConnection.getDefault(), derivTrades, jdate, errorMsgsP, pricingEnv);
        } catch (CalypsoServiceException exc) {
            Log.error(CSAExposureReport.class.getSimpleName(), "Error getting trades from an id array", exc.getCause());
        }
        return reportRows;
    }

    /**
     * New method to filter trades with threads
     *
     * @param tradeList
     * @param i
     * @param jdate
     * @param reportTemplate
     * @return
     */
    private List<Trade> filterTrades(
            final TradeArray tradeList,
            final JDate jdate,
            final ReportTemplate reportTemplate) {
        List<Trade> filteredTrades = new ArrayList<>();
        for (Trade trade : tradeList.asList()) {
            if (acceptContractType(trade) && filterByContractPO(reportTemplate, trade)
                    && filterByExposureAfterMaturityKeyword(trade, jdate)) {
                filteredTrades.add(trade);
            }
        }
        return filteredTrades;
    }

    private boolean filterByContractPO(ReportTemplate template, Trade trade) {
        boolean res = true;
        if (trade.getProduct() instanceof CollateralExposure) {
            String ownerName = (String) ((CollateralExposure) trade.getProduct()).getAttribute(OWNER_COLL_EXP_ATTR);
            if (!Util.isEmpty(ownerName)) {
                res = filterByCollateralExposureOwner(ownerName, template);
            } else {
                res = CollateralUtilities.filterPoByTemplate(template, trade);
            }
        }
        //Negated cause true means that the trade is not accepted...
        return !res;
    }

    private boolean filterByCollateralExposureOwner(String ownerName, ReportTemplate template) {
        return CollateralUtilities.filterPoByTemplate(template, ownerName);
    }

    private boolean acceptContractType(Trade trade) {
        return ((trade.getKeywordValue(CONTRACT_TYPE_STR) != null)
                && (!trade.getKeywordValue(CONTRACT_TYPE_STR).equals("ISMA"))
                && (!trade.getKeywordValue(CONTRACT_TYPE_STR).equals("MMOO")));
    }

    /**
     * @param trade the trade
     * @param jdate the value date
     * @return false if maturityDate is before valueDate and ExposureAfterMaturity keyword is false or
     * null.
     */
    private boolean filterByExposureAfterMaturityKeyword(Trade trade, JDate jdate) {
        boolean res = true;
        JDate maturityDate = trade.getMaturityDate();
        if (maturityDate != null && maturityDate.before(jdate) && isExposureAfterMaturityTrade(trade)) {
            res = false;
        }
        return res;
    }

    /**
     * @param trade
     * @return
     */
    private boolean isExposureAfterMaturityTrade(Trade trade) {
        return trade != null && (trade.getKeywordValue("ExposureAfterMaturity") == null
                || "false".equalsIgnoreCase(trade.getKeywordValue("ExposureAfterMaturity")));
    }


    /**
     * Puts a trade as a property in a report row. In this implementation the report is first
     * compressed to save memory an then saved in the row.
     *
     * @param row   The row where the trade is going to be saved
     * @param trade The trade to be saved
     * @throws IOException
     * @author Carlos Cejudo
     */
    /*
     * MIG 16.1, deprecated method to avoid GZIP processing. Trades are going to be put directly.
     */
    @Deprecated
    private void putTrade(ReportRow row, Trade trade) throws IOException {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutput = new GZIPOutputStream(byteArray);
        ObjectOutputStream output = new ObjectOutputStream(gzipOutput);
        output.writeObject(trade);
        output.close();
        row.setProperty(ROW_PROPERTY_COMPRESSED_TRADE, byteArray.toByteArray());
        row.setProperty(ROW_PROPERTY_TRADE_ID, trade.getLongId());
    }

    /**
     * @param row
     * @param trade
     */
    private void putTradeRowProperty(ReportRow row, Trade trade) {
        row.setProperty(ROW_PROPERTY_COMPRESSED_TRADE, trade);
        row.setProperty(ROW_PROPERTY_TRADE_ID, trade.getLongId());
    }

    // COL_OUT_016

    /**
     * Gets the report rows for the trades in the specified trade array.
     *
     * @param dsConn      A DSConnection to retrieve the needed PLMarks
     * @param derivTrades The array of trades
     * @param errorMsgsP  Vector or error messages
     * @return A list with the generated report rows
     * @author Carlos Cejudo
     */
    private List<ReportRow> getReportRows(
            final DSConnection dsConn,
            final List<Trade> derivTrades,
            final JDate jdate,
            final Vector errorMsgsP,
            final PricingEnv pricingEnv) {

        List<ReportRow> finalReportRows = new ArrayList<>();
        for (Trade trade : derivTrades) {
            CSAExposureItem csaExpItem = exposureLogic.getCSAExposureItem(trade, jdate, errorMsgsP, dsConn, pricingEnv);
            ReportRow repRow = new ReportRow(csaExpItem);
            putTradeRowProperty(repRow, trade);
            finalReportRows.add(repRow);
        }
        return finalReportRows;
    }
}
