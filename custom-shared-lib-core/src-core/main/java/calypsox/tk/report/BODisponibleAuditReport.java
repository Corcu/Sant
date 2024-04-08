package calypsox.tk.report;

import calypsox.tk.refdata.SantMarginCallStaticDataFilter;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.GenericCommentLight;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.AuditReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * @author acd
 */
public class BODisponibleAuditReport extends AuditReport {

    public static final Integer THREADS = 10;
    private ConcurrentHashMap<Long, List<ReportRow>> groupByTradeId;
    private ConcurrentHashMap<Long, List<ReportRow>> groupByTransferId;
    private ConcurrentLinkedQueue<ReportRow> finalReportRows;
    private List<ReportRow> loadedReportRows;

    @Override
    public ReportOutput load(SQLQuery query, Vector errorMsgs) {
        init();
        DefaultReportOutput defaultReportOutput = loadAuditPerHour(query,errorMsgs);
        if (!Util.isEmpty(defaultReportOutput.getRows())) {
            Arrays.stream(defaultReportOutput.getRows()).parallel()
                    .forEach(row -> {
                        Long boTransferId = new Long(((AuditValue) row.getProperty("Default")).getEntityId());
                        groupByTransferId.computeIfAbsent(boTransferId, k -> Collections.synchronizedList(new ArrayList<>()))
                                .add(row);
                    });

            processInBatch(groupByTransferId.keySet(), this::processTransferBatch);
            processInBatch(groupByTradeId.keySet(), this::processTradeBatch);

            Arrays.stream(defaultReportOutput.getRows()).parallel().forEach(row -> {
                if(isBond(row.getProperty("Product"))){
                    finalReportRows.add(row);
                }
            });

            defaultReportOutput.setRows(finalReportRows.toArray(new ReportRow[0]));
        }

        return defaultReportOutput;
    }


    /**
     * Load all audit launching one query per hour
     * @param query
     * @param errorMsgs
     * @return Reportoutput
     */
    private DefaultReportOutput loadAuditPerHour(SQLQuery query, Vector errorMsgs) {
        DefaultReportOutput defaultReportOutput = new DefaultReportOutput(this);

        IntStream.range(0, 24).forEach(hora -> executeQueryForHour(query, buildJDatetime(getValDate(), hora, 0, 0, 0),
                buildJDatetime(getValDate(), hora, 59, 59, 999), errorMsgs));
        defaultReportOutput.setRows(loadedReportRows.toArray(new ReportRow[0]));
        return defaultReportOutput;
    }

    /**
     * Execute botransfer and trade
     * @param ids
     * @param batchProcessor
     */
    private void processInBatch(Set<Long> ids, Consumer<List<Long>> batchProcessor) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Long> idList = new ArrayList<>(ids);
        int batchSize = 999;
        CountDownLatch latch = new CountDownLatch((idList.size() + batchSize - 1) / batchSize);

        for (int i = 0; i < idList.size(); i += batchSize) {
            final List<Long> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
            executor.execute(() -> {
                batchProcessor.accept(batch);
                latch.countDown();
            });
        }

        awaitLatch(latch);
        executor.shutdown();
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.error(this, "Execution was interrupted: " + e.getMessage());
        }
    }

    /**
     * Load all BOTransfers from audit changes, and set all existing trade ids on groupByTradeId
     * @param batch
     */
    private void processTransferBatch(List<Long> batch) {
        boolean excludeNetting = Boolean.parseBoolean(getReportTemplate().get("Exclude Netting"));

        try {
            TransferArray boTransfers = DSConnection.getDefault().getRemoteBO()
                    .getBOTransfers("TRANSFER_ID IN " + Util.collectionToSQLString(batch), null);

            for (BOTransfer transfer : boTransfers.getTransfers()) {
                if(transfer.getTransferType().equals("SECURITY") && transfer.getProductId() > 0){
                    Product securityProduct = getProductFromTransfer(transfer.getProductId());
                    String lastFallidasLastComment = getLastFallidasLastComment(transfer.getLongId());

                    List<ReportRow> relevantRows = groupByTransferId.get(transfer.getLongId());
                    if (relevantRows != null) {
                        relevantRows.forEach(row -> {
                            row.setProperty("BOTransfer", transfer);
                            row.setProperty("Product", securityProduct);
                            row.setProperty("Transfer.Comment.Fallidas", lastFallidasLastComment);
                        });
                        boolean nettingTransfer = excludeNetting ? !isNettingTransfer(transfer) : true;

                        if(isBond(securityProduct) && nettingTransfer){
                            long tradeLongId = transfer.getTradeLongId();
                            if (tradeLongId > 0) {
                                groupByTradeId.computeIfAbsent(tradeLongId, k -> Collections.synchronizedList(new ArrayList<>())).addAll(relevantRows);
                            }
                        }
                    }
                }
            }

        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading transfers: " + e.getMessage());
        }
    }

    /**
     * Load all Trades from BOTransfers
     * @param batch
     */
    private void processTradeBatch(List<Long> batch) {
        try {
            TradeArray trades = DSConnection.getDefault().getRemoteTrade()
                    .getTrades(batch.stream().mapToLong(Long::longValue).toArray());
            for (Trade trade : trades.getTrades()) {
                List<ReportRow> relevantRows = groupByTradeId.get(trade.getLongId());
                if (relevantRows != null) {
                    relevantRows.forEach(row -> row.setProperty("Trade", trade));
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading trades: " + e.getMessage());
        }
    }

    private boolean isBond(Product product){
        return product!=null && (product instanceof Bond || "Bond".equalsIgnoreCase(product.getProductFamily()));
    }

    private boolean isNettingTransfer(BOTransfer transfer){
        return transfer.getNettedTransfer();
    }

    private String getLastFallidasLastComment(long transferId){
        try {
            List<GenericCommentLight> listaComment = DSConnection.getDefault().getRemoteBO().getLatestGenericComments(transferId, "Transfer", Util.stringToList("Fallidas"));
            return Optional.ofNullable(listaComment)
                    .filter(comments -> !comments.isEmpty())
                    .map(comments -> comments.get(comments.size() - 1).getComment())
                    .orElse("");
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(),"Error: " + e);
        }

        return "";
    }

    private Product getProductFromTransfer(int productId){
        Product product=null;
        try {
            product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), productId);
            if (product == null) {
                product = DSConnection.getDefault().getRemoteProduct().getProduct(productId);
            }
            if (product != null) {
                product.condenseFlows();
            }
        } catch (CalypsoServiceException exc) {
            Log.error(SantMarginCallStaticDataFilter.class.getSimpleName(), "Could not get product " + productId, exc);
        }
        return product;
    }

    private void init(){
        loadedReportRows = new ArrayList<>();
        groupByTradeId = new ConcurrentHashMap<>();
        groupByTransferId = new ConcurrentHashMap<>();
        finalReportRows = new ConcurrentLinkedQueue<>();
    }

    private void executeQueryForHour(SQLQuery query, Date fechaInicio, Date fechaFin, Vector errorMsgs) {
        removeDateRangeCondition(query);
        query.appendWhereClause("MODIF_DATE BETWEEN "
                + Util.datetime2SQLString(fechaInicio) + " AND " + Util.datetime2SQLString(fechaFin));
        DefaultReportOutput defaultReportOutput = (DefaultReportOutput)  super.load(query,errorMsgs);
        if(null!=defaultReportOutput && !Util.isEmpty(defaultReportOutput.getRows())){
            loadedReportRows.addAll(Arrays.asList(defaultReportOutput.getRows()));
        }
    }

    public void removeDateRangeCondition(SQLQuery query){
        String modifiedString = StringUtils.substringBeforeLast(query.getWhereClause(), "AND");
        modifiedString = StringUtils.substringBeforeLast(modifiedString, "AND");
        query.setWhereClause(modifiedString.trim());
    }

    private JDatetime buildJDatetime(JDate jdate, int hour, int minutes, int seconds, int millis){
        return new JDatetime(JDate.valueOf(jdate.getYear(), jdate.getMonth(),jdate.getDayOfMonth()), hour, minutes, seconds, millis,TimeZone.getDefault());
    }

}
