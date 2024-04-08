package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("serial")
public class CSAExposureReportOld extends TradeReport {
    public static final String CSA_EXPOSURE_REPORT = "CSAExposureReport";
    public static final String ROW_PROPERTY_TRADE_ID = "Trade ID";
    public static final String ROW_PROPERTY_COMPRESSED_TRADE = "Compressed Trade";
    private static final int NUM_TRADES_PER_STEP = 1000;

    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {
        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        final DSConnection dsConn = getDSConnection();
        final PricingEnv pricingEnv = getPricingEnv();
        final ReportTemplate reportTemp = getReportTemplate();
        JDate jdate = reportTemp.getValDate();
        ArrayList<Trade> derivTrades = new ArrayList<Trade>();

        try {
            /*
             * String trades_where_clause =
             * "product_desc.product_type = 'CollateralExposure' " +
             * "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
             * +
             * "AND (product_desc.maturity_date is NULL or trunc(product_desc.maturity_date) >= trunc("
             * + Util.date2SQLString(jdate) + ") -1) " +
             * "AND trade.trade_status in ('VERIFIED', 'MATURED') " +
             * "AND trade.trade_date_time <= " +
             * Util.datetime2SQLString(dateTime);
             */

            /*
             * String trades_where_clause =
             * "product_desc.product_type = 'CollateralExposure' " +
             * "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
             * +
             * "AND (product_desc.maturity_date is NULL or trunc(product_desc.maturity_date) >= "
             * + Util.date2SQLString(jdate) + ") " +
             * " AND (trade.trade_status = 'VERIFIED' OR " +
             * "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value "
             * +
             * "where pl_mark.MARK_ID = pl_mark_value.MARK_ID AND trade.trade_id = pl_mark.trade_id AND "
             * +
             * "pl_mark_value.mark_name = 'NPV_BASE' AND pl_mark_value.mark_value != 0 AND "
             * + "trunc(pl_mark.valuation_date) = " + Util.date2SQLString(jdate)
             * + ")))" + " AND trunc(trade.trade_date_time) <= " +
             * Util.date2SQLString(jdate);
             */

            String trades_where_clause =
                    "product_desc.product_type = 'CollateralExposure' "
                            + "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
                            + " AND (trade.trade_status = 'VERIFIED' OR "
                            + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value "
                            + "where pl_mark.MARK_ID = pl_mark_value.MARK_ID AND trade.trade_id = pl_mark.trade_id AND "
                            + "pl_mark_value.mark_name = 'NPV_BASE' AND pl_mark_value.mark_value != 0 AND "
                            + "trunc(pl_mark.valuation_date) = "
                            + Util.date2SQLString(jdate)
                            + ")))"
                            + " AND trunc(trade.trade_date_time) <= "
                            + Util.date2SQLString(jdate)
                            + " AND ROWNUM < 3000";

            // COL_OUT_016
            // Changed by Carlos Cejudo:
            // Only the ids for the needed trades are retrieved from the
            // database. Once we have every id trades are
            // taken in steps, retrieving a number of them each time. With those
            // trades their corresponding report rows
            // are generated. In the next step the next group of trades is taken
            // and processed. This improves the
            // performance of this process.

            long TInicio, TFin, tiempo;
            /** */
            TInicio = System.currentTimeMillis();
            /** */
            long[] tradeIds = dsConn.getRemoteTrade().getTradeIds(null, trades_where_clause, 0, 0, null, null);
            // int[] overdueTradeIds=
            // dsConn.getRemoteTrade().getTradeIds(null,overdue_trades_where_clause, 0, 0, null);
            TFin = System.currentTimeMillis();
            /** */
            tiempo = TFin - TInicio;
            /** */
            System.err.println(
                    "*******Tiempo total para TrIDs: " + tiempo + ". Total TrIds: " + tradeIds.length);
            /** */
            Log.info(Log.OLD_TRACE, "All trades IDs have been loaded: " + tradeIds.length);
            /** */
            int numOfTrades = tradeIds.length;

            int startIdx = 0;
            int endIdx = getEndIndex(startIdx, numOfTrades, NUM_TRADES_PER_STEP);
            TInicio = System.currentTimeMillis();
            /** */
            while ((startIdx < numOfTrades) && (endIdx <= numOfTrades)) {
                long[] tradesInStep = Arrays.copyOfRange(tradeIds, startIdx, endIdx);
                TradeArray tradeArray = dsConn.getRemoteTrade().getTrades(tradesInStep);
                System.err.println("N?mero de trades procesados: " + startIdx);
                /** */

                // quitar repos estructurados
                derivTrades.clear();
                for (int i = 0; i < tradeArray.size(); i++) {

                    final Trade trade = tradeArray.get(i);

                    if ((trade.getKeywordValue("CONTRACT_TYPE") != null)
                            && (!trade.getKeywordValue("CONTRACT_TYPE").equals("ISMA"))
                            && (!trade.getKeywordValue("CONTRACT_TYPE").equals("MMOO"))) {

                        // GSM 15/07/15. SBNA Multi-PO filter
                        if (CollateralUtilities.filterPoByTemplate(getReportTemplate(), trade)) {
                            continue;
                        }
                        // GSM: 16/10/2014. MMOO contracts must not appear
                        if (filterByExposureAfterMaturityKeyword(trade, jdate)) {
                            derivTrades.add(tradeArray.get(i));
                        }
                    }
                }
                // quitar repos estructurados

                reportRows.addAll(getReportRows(dsConn, derivTrades, jdate, errorMsgsP, pricingEnv));

                startIdx = endIdx;
                endIdx = getEndIndex(startIdx, numOfTrades, NUM_TRADES_PER_STEP);
            }
            TFin = System.currentTimeMillis();
            /** */
            tiempo = TFin - TInicio;
            /** */
            System.err.println(
                    "*******Tiempo total del tratamiento de trades: "
                            + tiempo
                            + ". Numero productos en ReportRows: "
                            + reportRows.size());
            /** */
            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ReposTradeReport - " + e.getMessage() + "\n" + e); // sonar
            ControlMErrorLogger.addError(
                    ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated"); // CONTROL-M
            // ERROR
        } catch (final IOException e) {
            Log.error(this, "ReposTradeReport - " + e.getMessage() + "\n" + e); // sonar
            ControlMErrorLogger.addError(
                    ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated"); // CONTROL-M
            // ERROR
        }

        return null;
    }

    // COL_OUT_016

    /**
     * Gets the report rows for the trades in the specified trade array.
     *
     * @param dsConn     A DSConnection to retrieve the needed PLMarks
     * @param trades     The array of trades
     * @param errorMsgsP Vector or error messages
     * @return A list with the generated report rows
     * @throws RemoteException
     * @throws IOException
     * @author Carlos Cejudo
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArrayList<ReportRow> getReportRows(
            DSConnection dsConn,
            ArrayList<Trade> trades,
            JDate jdate,
            final Vector errorMsgsP,
            PricingEnv pricingEnv)
            throws RemoteException, IOException {
        // PLMarksMap plMarksMap = new PLMarksMap(dsConn, trades);
        ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

        for (int numTrades = 0; numTrades < trades.size(); numTrades++) {
            Trade trade = trades.get(numTrades);

            // * ONLY PROCESS TRADES WITH VERIFIED STATUS OR MATURED STATUS (con
            // maturitydate=fecha-1) *//
            // Jos? D. Sevillano - 03/09/2012 - Added the condition to skip
            // trades in VERIFIED but with past end date.
            // if (((null != collExposure) && (null ==
            // collExposure.getEndDate()))
            // || ((trade.getStatus().equals("VERIFIED")) && (null !=
            // collExposure) && (JDate.diff(jdate,
            // collExposure.getEndDate()) >= -1))
            // || ((trade.getStatus().equals("MATURED")) && (JDate.diff(jdate,
            // trade.getMaturityDate()) == -1))) {

            final Vector<CSAExposureItem> csaExpItem = null;
            // CSAExposureLogic.getReportRows(trade, jdate, errorMsgsP, dsConn, pricingEnv);

            for (int i = 0; i < csaExpItem.size(); i++) {
                final ReportRow repRow = new ReportRow(csaExpItem.get(i));
                putTrade(repRow, trade);
                reportRows.add(repRow);
            }
            // }
        }
        return reportRows;
    }

    // COL_OUT_016

    /**
     * Get the end index of the next subarray to be taken.
     *
     * @param startIdx The start index of the subarray
     * @param length   The total length of the array from which the subarrays are taken
     * @param maxItems Maximum number of items in any subarray
     * @return The end index of the subarray
     * @author Carlos Cejudo
     */
    private int getEndIndex(int startIdx, int length, int maxItems) {
        int endIdx = (startIdx + maxItems);
        if (endIdx > length) {
            endIdx = length;
        }

        return endIdx;
    }

    // COL_OUT_016

    /**
     * Puts a trade as a property in a report row. In this implementation the report is first
     * compressed to save memory an then saved in the row.
     *
     * @param row   The row where the trade is going to be saved
     * @param trade The trade to be saved
     * @throws IOException
     * @author Carlos Cejudo
     */
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
     * @param trade the trade
     * @param jdate the value date
     * @return false if maturityDate is before valueDate and ExposureAfterMaturity keyword is false or
     * null.
     */
    private boolean filterByExposureAfterMaturityKeyword(Trade trade, JDate jdate) {

        JDate maturityDate = trade.getMaturityDate();

        if (maturityDate != null && maturityDate.before(jdate)) {
            if (trade.getKeywordValue("ExposureAfterMaturity") == null
                    || trade.getKeywordValue("ExposureAfterMaturity").equalsIgnoreCase("false")) {
                return false;
            }
        }

        return true;
    }
}
