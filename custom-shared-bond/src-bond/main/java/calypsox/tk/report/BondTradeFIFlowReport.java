package calypsox.tk.report;

import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import calypsox.tk.bo.fiflow.factory.BondCalypsoToTCyCCMsgBuilder;
import calypsox.tk.bo.fiflow.model.CalypsoToTCyCCBean;
import calypsox.tk.report.carteras.CarterasTaskHandler;
import calypsox.tk.util.ScheduledTaskCarterasReport;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.FdnUtilProvider;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author dmenendd
 * <p>
 * A file containing FI open trades is generated to be sent to TCyCC system.
 */
public class BondTradeFIFlowReport extends TradeReport implements PlatformAllocationTradeFilterAdapter {

    static final String ROW_PROP_NAME = "TCyCCBean";

    private static final long serialVersionUID = -1655127533046540816L;

    private CarterasTaskHandler taskHandler;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        if (!_countOnly) {
            this.taskHandler = initTaskHandler();
            List<Long> currentTradeIdList = new ArrayList<>();

            //Add multithreading in case of performance issues
            ReportRow[] outputSettleDate = Optional.ofNullable(loadBySettleDate(errorMsgs))
                    .map(rp -> ((DefaultReportOutput) rp).getRows()).orElse(new ReportRow[0]);
            ReportRow[] outputModifications = Optional.ofNullable(loadByPartenonModifications(errorMsgs))
                    .map(rp -> ((DefaultReportOutput) rp).getRows()).orElse(new ReportRow[0]);
            ReportRow[] outputCancelations = Optional.ofNullable(loadCancelations(errorMsgs))
                    .map(rp -> ((DefaultReportOutput) rp).getRows()).orElse(new ReportRow[0]);
            ReportRow[] outputBlockTradeAllocations = Optional.ofNullable(loadBlockTradeAllocations(errorMsgs))
                    .map(rp -> ((DefaultReportOutput) rp).getRows()).orElse(new ReportRow[0]);


            ReportRow[] rows = Stream.of(Arrays.stream(outputSettleDate),
                            Arrays.stream(outputModifications), Arrays.stream(outputCancelations), Arrays.stream(outputBlockTradeAllocations))
                    .flatMap(row -> row).toArray(ReportRow[]::new);

            List<ReportRow> filteredList = filterTradeList(rows, currentTradeIdList);
            return initCarterasReportOutput(filteredList);
        } else {
            return super.load(errorMsgs);
        }
    }

    private CarterasTaskHandler initTaskHandler() {
        return new CarterasTaskHandler(this.getValuationDatetime(), this.getReportTemplate().getBoolean(ScheduledTaskCarterasReport.TASK_MARKING_FLAG));
    }

    /**
     * This method is far from being efficient and it may be redundant
     *
     * @param rows
     * @param currentTradeIdList
     * @return
     */
    private List<ReportRow> filterTradeList(ReportRow[] rows, List<Long> currentTradeIdList) {
        List<ReportRow> filteredList = new ArrayList<>();
        for (ReportRow row : rows) {
            Trade trade = row.getProperty(Trade.class.getSimpleName());
            if (!currentTradeIdList.contains(trade.getLongId())
                    && acceptCarterasTradeByType(trade)) {
                if (acceptCarterasModifTrade(trade)) {
                    addCancelationRow(trade, filteredList);
                    currentTradeIdList.add(trade.getLongId());
                    filteredList.add(row);
                } else if (acceptCarterasAllocatedBlockTrade(trade) || acceptCarterasCanceledTrade(trade)) {
                    addCancelationRow(trade, filteredList);
                    currentTradeIdList.add(trade.getLongId());
                } else if (acceptCarterasAliveTrade(trade)) {
                    currentTradeIdList.add(trade.getLongId());
                    filteredList.add(row);
                }
            }
        }
        return filteredList;
    }


    public ReportOutput loadBySettleDate(Vector<String> errorMsgs) {
        this.getReportTemplate().put("SettleStartTenor", getPreviousBusinessDayTenor(this.getValDate()) + "D");
        return super.load(errorMsgs);
    }

    public ReportOutput loadBlockTradeAllocations(Vector<String> errorMsgs) {
        //Feature toogle, new code look for allocation changes
        if (isNewCodeEnabled()) {
            AuditReport auditReport = initAuditReport();
            SQLQuery auditQuery = auditReport.buildQuery(errorMsgs);
            enrichAuditBlockTradeAllocationsReportQuery(auditQuery);
            return loadAuditReport(auditReport, auditQuery, errorMsgs);
        } else {
            return null;
        }
    }

    public ReportOutput loadCancelations(Vector<String> errorMsgs) {
        AuditReport auditReport = initAuditReport();
        SQLQuery auditQuery = auditReport.buildQuery(errorMsgs);
        enrichAuditCancelationsReportQuery(auditQuery);
        return loadAuditReport(auditReport, auditQuery, errorMsgs);
    }

    public ReportOutput loadByPartenonModifications(Vector<String> errorMsgs) {
        AuditReport auditReport = initAuditReport();
        SQLQuery auditQuery = auditReport.buildQuery(errorMsgs);
        enrichAuditModificationsReportQuery(auditQuery);
        return loadAuditReport(auditReport, auditQuery, errorMsgs);
    }

    private DefaultReportOutput loadAuditReport(AuditReport auditReport, SQLQuery auditQuery, Vector<String> errorMsgs) {
        Map<Long, AuditTradeWrapper> auditMap = filterAndMapAuditReport((DefaultReportOutput) auditReport.load(auditQuery, errorMsgs));
        ReportRow[] rows = new ReportRow[auditMap.size()];
        int i = 0;
        for (Map.Entry<Long, AuditTradeWrapper> entry : auditMap.entrySet()) {
            rows[i] = new ReportRow(entry.getValue().trade);
            i = i + 1;
        }
        DefaultReportOutput output = new DefaultReportOutput(this);
        output.setRows(rows);
        return output;
    }

    private AuditReport initAuditReport() {
        AuditReport auditReport = new AuditReport();
        AuditReportTemplate auditReportTemplate = new AuditReportTemplate();
        auditReportTemplate.put("StartPlus", "-");
        auditReportTemplate.put("StartTenor", getEnteredDateStartTenor(this.getValDate()) + "D");
        auditReportTemplate.put("EndPlus", "-");
        auditReportTemplate.put("EndTenor", this.getReportTemplate().get("SettleEndTenor"));
        auditReportTemplate.put("Type", Trade.class.getSimpleName());
        auditReport.setReportTemplate(auditReportTemplate);
        auditReport.setValuationDatetime(this.getValuationDatetime());
        return auditReport;
    }

    private void enrichAuditModificationsReportQuery(SQLQuery auditQuery) {
        auditQuery.appendWhereClause("ENTITY_FIELD_NAME IN ('ADDKEY#PartenonAccountingID', 'MODKEY#PartenonAccountingID')");
        buildPOAuditClause(auditQuery);
    }

    private void enrichAuditBlockTradeAllocationsReportQuery(SQLQuery auditQuery) {
        auditQuery.appendWhereClause("ENTITY_FIELD_NAME IN ('_status')");
        auditQuery.appendWhereClause("OLD_VALUE = 'VERIFIED'");
        auditQuery.appendWhereClause("AUDIT_ACTION = 'ALLOCATE'");
        buildPOAuditClause(auditQuery);
    }

    private void enrichAuditCancelationsReportQuery(SQLQuery auditQuery) {
        auditQuery.appendWhereClause("ENTITY_FIELD_NAME IN ('_status')");
        auditQuery.appendWhereClause("NEW_VALUE = 'CANCELED'");
        buildPOAuditClause(auditQuery);
    }

    private void buildPOAuditClause(SQLQuery auditQuery) {
        String po = (String) Optional.ofNullable(this.getReportTemplate().get("ProcessingOrg")).orElse("");
        if (!Util.isEmpty(po)) {
            auditQuery.appendWhereClause("PO_ID = " + po);
        }
    }

    private Map<Long, AuditTradeWrapper> filterAndMapAuditReport(DefaultReportOutput output) {
        Map<Long, AuditTradeWrapper> auditMap = new HashMap<>();
        for (ReportRow row : output.getRows()) {
            AuditValue auditValue = row.getProperty("AuditValue");
            if (auditMap.get(auditValue.getEntityLongId()) == null) {
                try {
                    Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(auditValue.getEntityLongId());
                    if (acceptModificacionTrade(trade)) {
                        auditMap.put(auditValue.getEntityLongId(), new AuditTradeWrapper(trade, auditValue));
                    }
                } catch (CalypsoServiceException exc) {
                    Log.error(this, exc.getCause());
                }
            }
        }
        return auditMap;
    }

    private boolean acceptCarterasAllocatedBlockTrade(Trade trade) {
        return isNotCancelTrade(trade)
                && taskHandler.isSentToCarteras(trade)
                && isAllocatedStatus(trade);

    }

    private boolean isAllocatedStatus(Trade trade) {
        return trade.getStatus().equals(Status.valueOf("ALLOCATED"))
                || trade.getStatus().equals(Status.valueOf("PARTIAL_ALLOC"))
                || trade.getStatus().equals(Status.valueOf("DUMMY_FULL_ALLOC"));
    }

    private boolean acceptModificacionTrade(Trade trade) {
        return trade != null && trade.getProduct() instanceof Bond
                && trade.getSettleDate().before(this.getValDate());
    }

    private StandardReportOutput initCarterasReportOutput(List<ReportRow> rows) {
        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        ReportRow[] rowArray = Optional.ofNullable(rows).map(r -> rows.toArray(new ReportRow[0])).orElse(new ReportRow[0]);
        standardReportOutput.setRows(rowArray);
        initTCyCCBeans(standardReportOutput);
        return standardReportOutput;
    }

    private void initTCyCCBeans(DefaultReportOutput output) {
        if (output != null) {
            Arrays.stream(output.getRows()).parallel()
                    .forEach(this::enrichReportRow);
        }
    }


    private void enrichReportRow(ReportRow originalReportRow) {
        Trade trade = originalReportRow.getProperty(Trade.class.getSimpleName());
        BondCalypsoToTCyCCMsgBuilder msgBuilder =
                new BondCalypsoToTCyCCMsgBuilder(trade, getValDate(), this.getPricingEnv());
        CalypsoToTCyCCBean carterasBean = msgBuilder.build();
        originalReportRow.setProperty(ROW_PROP_NAME, carterasBean);
        taskHandler.publishTaskIfNotExists(trade);
    }

    /**
     * Block trades are filtered out
     *
     * @param trade
     * @return true is case of not being a block trade
     */
    private boolean acceptCarterasTradeByType(Trade trade) {
        //Feature toogle, new code doesnt filter out blockTrades
        if (isNewCodeEnabled()) {
            return true;
        } else {
            return !isPlatformOrCTMBlockTrade(trade);
        }
    }

    private boolean acceptCarterasTradeByExportStatus(Trade trade) {
        return acceptCarterasAliveTrade(trade) ||
                acceptCarterasCanceledTrade(trade) ||
                acceptCarterasModifTrade(trade);
    }

    private boolean acceptCarterasAliveTrade(Trade trade) {
        return isNotCancelTrade(trade)
                && !isAllocatedStatus(trade)
                && taskHandler.isFirstTimeExport(trade);
    }

    private boolean acceptCarterasCanceledTrade(Trade trade) {
        return isCancelTrade(trade) &&
                taskHandler.isSentToCarteras(trade);
    }

    private boolean acceptCarterasModifTrade(Trade trade) {
        return isNotCancelTrade(trade) &&
                taskHandler.isModifExportNeeded(trade);
    }

    private boolean isNotCancelTrade(Trade trade) {
        return !isCancelTrade(trade);
    }

    private boolean isCancelTrade(Trade trade) {
        return Optional.ofNullable(trade)
                .map(Trade::getStatus)
                .map(Status::getStatus)
                .map(Status.CANCELED::equals).orElse(false);
    }

    private int getPreviousBusinessDayTenor(JDate valDate) {
        Vector<String> holidays = new Vector<>();
        holidays.add("TARGET");
        JDate previousDate = FdnUtilProvider.getDateUtil().previousBusinessDay(valDate, holidays);
        previousDate = previousDate.addDays(1);
        return JDate.getTenor(previousDate, valDate);
    }


    private int getEnteredDateStartTenor(JDate valDate) {
        return getPreviousBusinessDayTenor(valDate) + 1;
    }


    private void addCancelationRow(Trade trade, List<ReportRow> filteredList) {
        String lastCarterasPartenon = this.taskHandler.getLastCarterasPartenon(trade);
        TradePartenonBuilder builder = new TradePartenonBuilder(lastCarterasPartenon);
        if (!Util.isEmpty(builder.buildFullPartenon())) {
            Trade clonedTrade = trade.clone();
            clonedTrade.setStatus(Status.S_CANCELED);
            clonedTrade.setAction(Action.CANCEL);
            clonedTrade.addKeyword("PartenonAccountingID", builder.buildFullPartenon());
            filteredList.add(new ReportRow(clonedTrade));
        }
    }

    @Override
    public PricingEnv getPricingEnv() {
        return Optional.ofNullable(super.getPricingEnv())
                .orElseGet(() -> PricingEnv.loadPE("OFFICIAL_ACCOUNTING", this.getValuationDatetime()));
    }


    public boolean isNewCodeEnabled() {
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "ACTIVATE_NEW_CARTERAS");
        return Boolean.parseBoolean(activationFlag);
    }

    private static class AuditTradeWrapper {
        AuditValue av;
        Trade trade;

        AuditTradeWrapper(Trade tradeIn, AuditValue value) {
            av = value;
            trade = tradeIn;
        }
    }
}
