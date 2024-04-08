package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.collateral.dto.CashAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.*;

public class SantLegChangeControlReport extends SantReport {

    private static final long serialVersionUID = -840811931715908290L;
    private final List<MarginCallEntryDTO> entries = null;

    @Override
    protected ReportOutput loadReport(Vector<String> errorMsgs) {
        try {
            return getReportOutput();
        } catch (final Exception e) {
            Log.error(this, "Cannot load MarginCallEntry", e);
        }
        return null;
    }

    /**
     * Generate the report output. Get the info that its will be shown in the report.
     *
     * @return ReportOutput report output
     * @throws Exception
     */
    private ReportOutput getReportOutput() throws CalypsoServiceException, CollateralServiceException {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final ReportTemplate template = getReportTemplate();
        final JDate valDate = super._valuationDateTime.getJDate(TimeZone.getDefault());
        final List<ReportRow> rows = new ArrayList<>();

        List<MarginCallEntryDTO> entries = loadEntries(template, valDate);

        for (MarginCallEntryDTO entry : entries) {

            if (checkSign(entry) && isMatchingCriteria(template, entry)) {
                List<CashAllocationDTO> cashAllocations = entry.getCashAllocations();
                List<SecurityAllocationDTO> securityAllocations = entry.getSecurityAllocations();

                for (CashAllocationDTO cashAllocation : cashAllocations) {

                    if (cashAllocation.getTradeId() != 0) {
                        final ReportRow row = new ReportRow(entry, "Entry");
                        row.setProperty("CashAllocation", cashAllocation);

                        Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(cashAllocation.getTradeId());
                        row.setProperty("Trade", trade);
                        CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(getDSConnection(),
                                ((MarginCall) trade.getProduct()).getMarginCallId());

                        row.setProperty("SantMarginConfig", mcc);
                        rows.add(row);
                    }
                }

                for (SecurityAllocationDTO securityAllocation : securityAllocations) {
                    if (securityAllocation.getTradeId() != 0) {
                        final ReportRow row = new ReportRow(entry, "Entry");
                        row.setProperty("SecurityAllocation", securityAllocation);

                        Trade trade = DSConnection.getDefault().getRemoteTrade()
                                .getTrade(securityAllocation.getTradeId());
                        row.setProperty("Trade", trade);
                        CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(getDSConnection(),
                                ((MarginCall) trade.getProduct()).getMarginCallId());

                        row.setProperty("SantMarginConfig", mcc);
                        rows.add(row);
                    }
                }

            }
        }

        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    private boolean isMatchingCriteria(ReportTemplate template, MarginCallEntryDTO entry) {
        boolean poAgrResult = true;

        // PO Deals
        String agrStr = template.get(SantGenericTradeReportTemplate.OWNER_DEALS);

        try {
            if (!Util.isEmpty(agrStr) && (agrStr != null)) {
                poAgrResult = false;
                if (entry.getCashAllocations().size() > 0) {
                    List<CashAllocationDTO> allocations = entry.getCashAllocations();

                    Integer poId = DSConnection.getDefault().getRemoteReferenceData()
                            .getBook(allocations.get(0).getBookId()).getProcessingOrgBasedId();
                    if (agrStr.equals(poId.toString())) {
                        poAgrResult = true;
                    }

                } else if (entry.getSecurityAllocations().size() > 0) {
                    List<SecurityAllocationDTO> allocations = entry.getSecurityAllocations();

                    Integer poId = DSConnection.getDefault().getRemoteReferenceData()
                            .getBook(allocations.get(0).getBookId()).getProcessingOrgBasedId();
                    if (agrStr.equals(poId.toString())) {
                        poAgrResult = true;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(e.getCause(), "Couldn't load the PO of the book");
            Log.error(this, e); //sonar
        }

        return poAgrResult;
    }

    private boolean checkSign(MarginCallEntryDTO entry) {
        boolean result = false;

        if (entry.getOpeningBalance() > 0.0) {
            if (entry.getClosingBalance() < 0.0) {
                result = true;
            }
        } else if (entry.getOpeningBalance() < 0.0) {
            if (entry.getClosingBalance() > 0.0) {
                result = true;
            }
        }

        return result;
    }

    private List<MarginCallEntryDTO> loadEntries(final ReportTemplate template, final JDate valDate) throws CollateralServiceException {
        if (!Util.isEmpty(this.entries)) {
            return this.entries;
        }
        final List<String> from = new ArrayList<>();
        final StringBuilder sqlWhere = new StringBuilder();

        buildMarginCallEntriesSQLQuery(template, valDate, from, sqlWhere);

        return new SantMarginCallEntryLoader().loadMarginCallEntriesDTO(from, sqlWhere.toString(), true);
        /*
        MIG V16 OLD
        return SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallEntriesDTO(
                sqlWhere.toString(), from, true);
                */

    }

    @SuppressWarnings("unused")
    private Map<Integer, CollateralConfig> loadContracts(final Set<Integer> contractIds) {
        try {
            return SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigByIds(
                    new ArrayList<Integer>(contractIds));
        } catch (final PersistenceException e) {
            Log.error(this, "Cannot Load Contract", e);
        }
        return null;
    }

    protected void buildMarginCallEntriesSQLQuery(final ReportTemplate template, final JDate valDate,
                                                  final List<String> from, final StringBuilder sqlWhere) {

        from.add(" margin_call_entries ");
        from.add(" mrgcall_config ");

        sqlWhere.append(" mrgcall_config.mrg_call_def = margin_call_entries.mcc_id ");

        // Counterparty
        final String s = (String) template.get(SantGenericTradeReportTemplate.COUNTERPARTY);
        if (!Util.isEmpty(s)) {
            buildCptyQuery(sqlWhere, s);
        }

        // process start date
        JDate processStartDate = getDate(template, valDate, TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
        if (processStartDate != null) {
            processStartDate = processStartDate.addBusinessDays(-1, template.getHolidays());
            sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) >= to_date('");
            sqlWhere.append(processStartDate);
            sqlWhere.append(" 00:00:00','dd/MM/yyyy HH24:MI:SS')");
        }

        // process end date
        final JDate processEndDate = getDate(template, valDate, TradeReportTemplate.END_DATE,
                TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);
        if (processEndDate != null) {
            sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) < to_date('");
            sqlWhere.append(processEndDate);
            sqlWhere.append(" 23:59:59','dd/MM/yyyy HH24:MI:SS')");
        }

        // Agreement Ids
        final String agreementIds = template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementIds)) {
            final Vector<String> agrIds = Util.string2Vector(agreementIds);
            sqlWhere.append(" AND mrgcall_config.mrg_call_def in ");
            sqlWhere.append(Util.collectionToSQLString(agrIds));
        }

        // Agreement type
        final String agrType = template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (!Util.isEmpty(agrType)) {
            sqlWhere.append(" AND mrgcall_config.contract_type = '");
            sqlWhere.append(agrType).append("'");
        }

        // PO Owner
        // GSM 03/08/15. SBNA Multi-PO filter. Adaptation to ST filter
        // final String poAgrStr = (String) template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(this._reportTemplate);
        if (!Util.isEmpty(poAgrStr)) {

            Vector<String> poAgrIds = Util.string2Vector(poAgrStr);

            if (!Util.isEmpty(poAgrIds)) {
                sqlWhere.append(" AND mrgcall_config.process_org_id in ");
                sqlWhere.append(Util.collectionToSQLString(poAgrIds));
            }
        }

        // Base Ccy
        final String ccyStr = template.get(SantGenericTradeReportTemplate.BASE_CURRENCY);
        if (!Util.isEmpty(ccyStr)) {
            sqlWhere.append(" AND mrgcall_config.currency_code = '");
            sqlWhere.append(ccyStr).append("'");
        }
    }

    @SuppressWarnings("unchecked")
    private void buildCptyQuery(final StringBuilder sqlWhere, final String s) {
        final Vector<String> ids = Util.string2Vector(s);

        if (ids.size() < ioSQL.MAX_ITEMS_IN_LIST) {
            sqlWhere.append(" AND mrgcall_config.legal_entity_id IN (").append(Util.collectionToString(ids))
                    .append(")");
        } else {
            final List<String> idsStrList = ioSQL.returnStringsOfStrings(ids);
            sqlWhere.append("(AND mrgcall_config.legal_entity_id IN (").append(idsStrList.get(0)).append(")");
            for (int i = 1; i < idsStrList.size(); i++) {
                sqlWhere.append(" OR mrgcall_config.legal_entity_id IN (").append(idsStrList.get(i)).append(")");
            }
            sqlWhere.append(")");
        }

    }

}
