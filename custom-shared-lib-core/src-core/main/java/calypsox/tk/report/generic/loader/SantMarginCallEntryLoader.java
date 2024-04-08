/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class SantMarginCallEntryLoader extends SantAbstractLoader {

    public List<SantMarginCallEntry> loadSantMarginCallEntries(final ReportTemplate template, final JDate processDate)
            throws CollateralServiceException{
        return loadSantMarginCallEntries(template, processDate, null);
    }

    public List<SantMarginCallEntry> loadSantMarginCallEntries(final ReportTemplate template, JDate processStartDate, JDate processEndDate)
            throws CollateralServiceException {
        return loadSantMarginCallEntries(template, processStartDate, processEndDate, true);
    }

    public List<SantMarginCallEntry> loadSantMarginCallEntries(final ReportTemplate template,
                                          final JDate processDate, boolean getHeavyWeight) throws CollateralServiceException {
        return loadSantMarginCallEntries(template, processDate, null, getHeavyWeight);
    }

    public List<SantMarginCallEntry> loadSantMarginCallEntries(final ReportTemplate template,
                                                                        JDate processStartDate, JDate processEndDate, boolean getHeavyWeight) throws CollateralServiceException {
        final List<MarginCallEntryDTO> entries = loadEntries(template, processStartDate, processEndDate, getHeavyWeight);
        final List<SantMarginCallEntry> santEntries = new ArrayList<>();
        for (final MarginCallEntryDTO entry : entries) {
            CollateralConfig contract = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), entry.getCollateralConfigId());

            if (isEligibleContract(contract, template)) {
                final SantMarginCallEntry santEntry = new SantMarginCallEntry(entry);
                santEntry.setMarginCallConfig(contract);
                santEntries.add(santEntry);
            }
        }
        return santEntries;
    }

    /**
     * @param whereClause
     * @param getHeavyWeight
     * @return
     * @throws CollateralServiceException
     */
    public List<MarginCallEntryDTO> loadMarginCallEntriesDTO(List<String> from, String whereClause, boolean getHeavyWeight) throws CollateralServiceException {
        return loadEntries(from, whereClause, getHeavyWeight);
    }

    private List<MarginCallEntryDTO> loadEntries(final ReportTemplate template, final JDate processDate, boolean getHeavyWeight) throws CollateralServiceException {
        return loadEntries(template, processDate, null, getHeavyWeight);
    }

    private List<MarginCallEntryDTO> loadEntries(final ReportTemplate template, JDate processStartDate, JDate processEndDate, boolean getHeavyWeight) throws CollateralServiceException {
        final List<String> from = new ArrayList<>();
        final StringBuilder sqlWhere = new StringBuilder();
        buildMarginCallEntriesSQLQuery(template, processStartDate, processEndDate, from, sqlWhere);
        return loadEntries(from, sqlWhere.toString(), getHeavyWeight);
    }

    private List<MarginCallEntryDTO> loadEntries(final List<String> from, final String sqlWhere, boolean getHeavyWeight) throws CollateralServiceException {

		/* old implementation
		return SantReportingUtil.getSantReportingService(ds).getMarginCallEntriesDTO(sqlWhere.toString(), from,
				fullLoad);
				*/

        //AAP MIG 16.1 - SantReportingService deprecation
        List<MarginCallEntryDTO> mcEntries = ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallEntries(sqlWhere, from);
        if (getHeavyWeight) {
            List<Integer> entryIds = mcEntries.stream().map(MarginCallEntryDTO::getId).collect(Collectors.toList());
            mcEntries = ServiceRegistry.getDefault().getCollateralServer().loadEntries(entryIds, false);
        }
        return mcEntries;
    }

    // Check made on the code and not SQL because Additional Fields are saved as a BLOB
    private boolean isEligibleContract(CollateralConfig contract, ReportTemplate template) {
        String economicSector = template.get(SantGenericTradeReportTemplate.ECONOMIC_SECTOR);

        if (Util.isEmpty(economicSector)) {
            return true;
        }
        return economicSector.equalsIgnoreCase(contract.getAdditionalField("ECONOMIC_SECTOR"));
    }


    private String buildDateQuery(JDate processStartDate, JDate processEndDate) {
        StringBuilder res = new StringBuilder();
        if (processEndDate == null) {
            res.append(" AND TRUNC(margin_call_entries.process_datetime) = ").append(Util.date2SQLString(processStartDate));
        } else {
            res.append(" AND TRUNC(margin_call_entries.process_datetime) BETWEEN ").append(Util.date2SQLString(processStartDate)).append(" AND ")
                    .append(Util.date2SQLString(processEndDate));
        }
        return res.toString();
    }

    private void buildMarginCallEntriesSQLQuery(final ReportTemplate template, final JDate processDate,
                                                final List<String> from, final StringBuilder sqlWhere) {
        buildMarginCallEntriesSQLQuery(template, processDate, null, from, sqlWhere);
    }

    private void buildMarginCallEntriesSQLQuery(final ReportTemplate template, final JDate processStartDate, JDate processEndDate,
                                                final List<String> from, final StringBuilder sqlWhere) {

        from.add(" margin_call_entries ");
        from.add(" mrgcall_config ");

        sqlWhere.append(" mrgcall_config.mrg_call_def = margin_call_entries.mcc_id ");

        // Counterparty
        final String s = getCounterparty(template);
        if (!Util.isEmpty(s)) {
            buildCptyQuery(sqlWhere, s);
        }
        // process date
        //sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) = ").append(Util.date2SQLString(processDate));
        sqlWhere.append(buildDateQuery(processStartDate, processEndDate));
        // Agreement status
        final String agrStatus = template.get(SantGenericTradeReportTemplate.AGREEMENT_STATUS);
        if (!Util.isEmpty(agrStatus)) {
            final Vector<String> agrStatusV = Util.string2Vector(agrStatus);
            sqlWhere.append(" AND margin_call_entries.status in ").append(Util.collectionToSQLString(agrStatusV));
        }

        // Valuation agent
        final Integer valAgentId = template.get(SantGenericTradeReportTemplate.VALUATION_AGENT);
        if ((valAgentId != null) && (valAgentId > 0)) {
            sqlWhere.append(" AND mrgcall_config.val_agent_id = ").append(valAgentId);
        }

        // Agreement Ids
        final String agreementIds = template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementIds)) {
            final Vector<String> agrIds = Util.string2Vector(agreementIds);
            sqlWhere.append(" AND mrgcall_config.mrg_call_def in ").append(Util.collectionToSQLString(agrIds));
        }

        // Exclude CSA Facade
        final String agrTypes = template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (Util.isEmpty(agrTypes)) {
            sqlWhere.append(" AND mrgcall_config.contract_type <> 'CSA_FACADE' ");
        }


        // Agreement type
        final String agrType = template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (!Util.isEmpty(agrType)) {
            sqlWhere.append(" AND mrgcall_config.contract_type = '").append(agrType).append("'");
        }

        // PO Owner
        // final String poAgrStr = (String) template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        // GSM 30/07/15. SBNA Multi-PO filter
        final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(template);
        Vector<String> poAgrIds = null;
        if (!Util.isEmpty(poAgrStr)) {
            poAgrIds = Util.string2Vector(poAgrStr);
            sqlWhere.append(" AND mrgcall_config.process_org_id in ").append(Util.collectionToSQLString(poAgrIds));
        }

        // Contract Base Currency
        String baseCcy = template.get(SantGenericTradeReportTemplate.BASE_CURRENCY);
        if (!Util.isEmpty(baseCcy)) {
            sqlWhere.append(" AND mrgcall_config.currency_code = '").append(baseCcy).append("'");
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
