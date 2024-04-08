package calypsox.tk.report;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;
import org.apache.commons.lang.ArrayUtils;
import org.jfree.util.Log;

import java.util.*;

public class PdvTransferReport extends TransferReport {
        private static final long serialVersionUID = 1L;
        @Override
        @SuppressWarnings("rawtypes")
        public ReportOutput load(Vector vector) {

            DefaultReportOutput dro = (DefaultReportOutput) super.load(vector);
            createRowsFromTerminated(dro);


            return dro;
        }

    private void createRowsFromTerminated(DefaultReportOutput dro) {
        try {
            if (dro == null)   {
                return;
            }

            Map<Long, Trade> trades = loadTerminatedTrades();
            TransferArray transferList = loadTerminatedTransfers(trades.keySet());
            if (transferList.isEmpty()) {
                return;
            }
            Iterator<BOTransfer> ite = transferList.iterator();
            Vector<ReportRow> rows = new Vector<>();
            while (ite.hasNext()) {
               final BOTransfer xfer = ite.next();
               ReportRow reportRow = new ReportRow(xfer);
               reportRow.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
               reportRow.setProperty(ReportRow.PRICING_ENV, getPricingEnv());
               reportRow.setProperty(ReportRow.TRANSFER, xfer);
               reportRow.setProperty(ReportRow.TRADE, trades.get(xfer.getTradeLongId()));
               reportRow.setUniqueKey(xfer.getKey());
               rows.add(reportRow);
            }


            rows.addAll(Arrays.asList(dro.getRows()));
            dro.setRows(rows.stream().toArray(ReportRow[]::new));
            //dro.setRows((ReportRow[]) rows.toArray());

        } catch (Exception e) {
            Log.error(this, e);
        }
    }
    private Map<Long, Trade> loadTerminatedTrades() {
        HashMap<Long, Trade> tradesMap = new HashMap<>();

        Calendar firstDayOfMonth = getValDate().asCalendar();
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, firstDayOfMonth.getActualMinimum(Calendar.DAY_OF_MONTH));

        Calendar lastDayOfMonth = getValDate().asCalendar();
        lastDayOfMonth.set(Calendar.DAY_OF_MONTH, lastDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));

        DSConnection dsConn = DSConnection.getDefault();
        try {
            TradeArray tx = dsConn.getRemoteTrade().getTrades(
                    "",
                    " product_desc.product_type IN ('SecLending') "
                            + " AND trade_status in ('TERMINATED') "
                            + " AND ( trunc(product_desc.maturity_date) >= "
                            + Util.date2SQLString(firstDayOfMonth.getTime()) + ")"
                            + " AND ( trunc(product_desc.maturity_date) <= "
                            + Util.date2SQLString(lastDayOfMonth.getTime()) + ") ",
                    //+ " AND trade.PRODUCT_ID = product_desc.PRODUCT_ID ",
                    null, null);


            if (!tx.isEmpty()) {

                for (Trade trade : tx.toList()) {
                    tradesMap.put(trade.getLongId(), trade);
                }
            }

        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
        return tradesMap;
    }

    private TransferArray loadTerminatedTransfers(Set<Long> trades) throws Exception {

        Calendar lastDayOfMonth = getValDate().asCalendar();
        lastDayOfMonth.set(Calendar.DAY_OF_MONTH, lastDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));

        TransferArray result = new TransferArray();
        if (!Util.isEmpty(trades)) {
            DSConnection dsConn = DSConnection.getDefault();
            StringBuilder sb = new StringBuilder();
            String s = getReportTemplate().get("Status");
            if (!Util.isEmpty(s)) {
                Vector v = Util.string2Vector(s);
                sb.append("bo_transfer.transfer_status IN ")
                        .append(Util.collectionToSQLString(v));
            }

            s = getReportTemplate().get("TransferType");
            if (!Util.isEmpty(s)) {
                Vector v = Util.string2Vector(s);
                sb.append(" and bo_transfer.transfer_type IN ")
                        .append(Util.collectionToSQLString(v));
            }

            sb.append(" and trunc(bo_transfer.value_date ) >= ");
            sb.append( Util.date2SQLString(lastDayOfMonth.getTime()));
            sb.append(" and bo_transfer.trade_id in ");
            sb.append(Util.collectionToSQLString(trades));
            result = dsConn.getRemoteBackOffice().getTransfers("", sb.toString(), null );
        }
        return result;
    }
}
