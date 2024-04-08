/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallAllocationBuilder;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallAllocationEntry;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.util.*;

public class SantMarginCallAllocationEntriesLoader extends SantAbstractLoader {

    private final Set<Integer> contractIds = new HashSet<>();

    public Collection<SantMarginCallAllocationEntry> load(final ReportTemplate template, final JDate valDate)
            throws Exception {

        final List<MarginCallEntryDTO> entries = loadEntries(template, valDate);


        for (final MarginCallEntryDTO entry : entries) {
            this.contractIds.add(entry.getCollateralConfigId());
        }
        final Map<Integer, CollateralConfig> contractsMap = loadContracts(this.contractIds);

        final SantMarginCallAllocationBuilder builder = new SantMarginCallAllocationBuilder();
        builder.build(entries, contractsMap, template);

        return builder.getSantAllocations();
    }

    public Collection<SantMarginCallAllocationEntry> loadWithDummy(final ReportTemplate template, final JDate valDate)
            throws Exception {


        final List<MarginCallEntryDTO> entries = loadEntries(template, valDate);

        for (final MarginCallEntryDTO entry : entries) {
            this.contractIds.add(entry.getCollateralConfigId());
        }
        final Map<Integer, CollateralConfig> contractsMap = loadContracts(this.contractIds);

        final SantMarginCallAllocationBuilder builder = new SantMarginCallAllocationBuilder();
        builder.build(entries, contractsMap, template, true);

        return builder.getSantAllocations();
    }

    private Map<Integer, CollateralConfig> loadContracts(final Set<Integer> contractIds) {
        try {
            return SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigByIds(
                    new ArrayList<Integer>(contractIds));
        } catch (final PersistenceException e) {
            Log.error(this, "Cannot Load Contract", e);
        }
        return null;
    }

    private List<MarginCallEntryDTO> loadEntries(final ReportTemplate template, final JDate valDate) throws CollateralServiceException {
        final List<String> from = new ArrayList<>();
        final StringBuilder sqlWhere = new StringBuilder();

        buildMarginCallEntriesSQLQuery(template, valDate, from, sqlWhere);

        return new SantMarginCallEntryLoader().loadMarginCallEntriesDTO(from, sqlWhere.toString(), true);
    }

    protected void buildMarginCallEntriesSQLQuery(final ReportTemplate template, final JDate valDate,
                                                  final List<String> from, final StringBuilder sqlWhere) {

        from.add(" margin_call_entries ");
        from.add(" mrgcall_config ");

        sqlWhere.append(" mrgcall_config.mrg_call_def = margin_call_entries.mcc_id ");

        // Counterparty or Funds
        final String s = getCounterparty(template);
        if (!Util.isEmpty(s)) {
            buildCptyQuery(sqlWhere, s);
        } else {
            final boolean isFund = getFund(template);
            if (isFund) {
                from.add(" legal_entity_role role ");
                sqlWhere.append(" AND mrgcall_config.legal_entity_id = role.legal_entity_id ");
                sqlWhere.append(" AND role.ROLE_NAME = 'Fund' ");
            }
        }

        // process start date
        final JDate processStartDate = getDate(template, valDate, TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
        if (processStartDate != null) {
            sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) >= ");
            sqlWhere.append(Util.date2SQLString(processStartDate));
        }

        // process end date
        final JDate processEndDate = getDate(template, valDate, TradeReportTemplate.END_DATE,
                TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);
        if (processEndDate != null) {
            sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) <= ");
            sqlWhere.append(Util.date2SQLString(processEndDate));
        }

        // Agreement status
        final String agrStatus = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_STATUS);
        if (!Util.isEmpty(agrStatus)) {
            final Vector<String> agrStatusV = Util.string2Vector(agrStatus);
            sqlWhere.append(" AND margin_call_entries.status in ");
            sqlWhere.append(Util.collectionToSQLString(agrStatusV));
        }

        // Valuation agent
        final Integer valAgentId = (Integer) template.get(SantGenericTradeReportTemplate.VALUATION_AGENT);
        if ((valAgentId != null) && (valAgentId > 0)) {
            sqlWhere.append(" AND mrgcall_config.val_agent_id = ");
            sqlWhere.append(valAgentId);
        }

        // Agreement Ids
        final String agreementIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementIds)) {
            final Vector<String> agrIds = Util.string2Vector(agreementIds);
            sqlWhere.append(" AND mrgcall_config.mrg_call_def in ");
            sqlWhere.append(Util.collectionToSQLString(agrIds));
        }

        // Exclude CSA Facade
        final String agrTypes = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (Util.isEmpty(agrTypes)) {
            sqlWhere.append(" AND mrgcall_config.contract_type <> 'CSA_FACADE' ");
        }

        // Agreement type
        final String agrType = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (!Util.isEmpty(agrType)) {
            sqlWhere.append(" AND mrgcall_config.contract_type = '");
            sqlWhere.append(agrType).append("'");
        }

        // PO Owner
        // GSM 05/08/15. SBNA Multi-PO filter
        final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(template);
        // final String poAgrStr = (String) template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        if (!Util.isEmpty(poAgrStr)) {
            final Vector<String> poAgrIds = Util.string2Vector(poAgrStr);
            if (!Util.isEmpty(poAgrIds)) {
                sqlWhere.append(" AND mrgcall_config.process_org_id in ");
                sqlWhere.append(Util.collectionToSQLString(poAgrIds));
            }
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
