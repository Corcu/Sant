package calypsox.tk.report;

import calypsox.regulation.util.SantEmirUtil;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

/**
 * @author Felipe Hidalgo
 * @version 1.1
 */
public class ExportSantEmirCLMReport extends ExportMarginCallDetailEntryReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 6228802102476021905L;

    /**
     * Accepted new status when the status change and the old value was VERIFIED.
     */
    private final List<String> AUDIT_STATUSES = Arrays.asList("CANCELED", "TERMINATED", "MATURED", "CHECKED");

    /*
     * Legal Entity CAVALSA.
     */
    //protected LegalEntity leCavalsa;

    /**
     * Override method load to generate the file (the report).
     *
     * @param errorMsgs passed by parameter
     * @return the ReportOutput to generate the report
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(final Vector errorMsgs) {

        final DefaultReportOutput output = new StandardReportOutput(this);

        getReportTemplate().put(MarginCallDetailEntryReportTemplate.START_DATE, this._valuationDateTime);
        getReportTemplate().put(MarginCallDetailEntryReportTemplate.END_DATE, this._valuationDateTime);

        // GSM 31/07/15. SBNA Multi-PO filter
        String poAgrStr = filterPoCollectionByTemplate(getReportTemplate());

        if (!Util.isEmpty(poAgrStr)) {
            final Collection<String> col = Util.string2Vector(poAgrStr);
            if (!Util.isEmpty(col)) {
                getReportTemplate().put(MarginCallEntryDTOReportTemplate.PROCESSING_ORG_IDS, col);
            }
        }

        DefaultReportOutput dro = (DefaultReportOutput) super.load(errorMsgs);
        ReportRow[] reportRows = (dro == null ? null : dro.getRows());
        JDatetime valDatetime = dro.getValDate();

        HashMap<Integer, FieldModification> auditContractChangeTradesIds = new HashMap<>();
        HashMap<Integer, FieldModification> auditStatusChangeTradesIds = new HashMap<>();
        List<Long> allTradeIds = new ArrayList<>();
        Map<Long, Trade> allTrades = new HashMap<>();
        List<Integer> contractIds = new ArrayList<>();
        List<Long> alreadyAddedTradeIds = new ArrayList<>();
        List<ReportRow> newReportRows = new ArrayList<>();

        if (reportRows != null && reportRows.length > 0) {
            try {
                // retrieve only products required for EMIR
                String from = "Trade, PRODUCT_DESC, trade_keyword, book";
                String where = "TRADE.trade_ID = TRADE_KEYWORD.trade_id " +
                        " AND TRADE.book_id = BOOK.book_id " +
                        " AND BOOK.LEGAL_ENTITY_ID in (" + poAgrStr + ")" +
                        " AND ((TRADE_KEYWORD.keyword_name = 'BO_REFERENCE' AND TRADE_KEYWORD.keyword_value is not null)" +
                                " OR (TRADE_KEYWORD.keyword_name = 'UTI_REFERENCE' AND TRADE_KEYWORD.keyword_value is not null))" +
                        " AND PRODUCT_DESC.PRODUCT_ID = TRADE.PRODUCT_ID" +
                        " AND PRODUCT_DESC.PRODUCT_SUB_TYPE in " + SantEmirUtil.getEmirSubProductTypesFromDV() +
                        " AND (PRODUCT_DESC.PRODUCT_TYPE like " + SantEmirUtil.getEmirProductTypesFromDV2PDTable() + ") " +
                        " AND TRADE.TRADE_STATUS IN " + SantEmirUtil.getEmirAcceptedStatusFromDV() +
                        " AND TRUNC(TRADE.UPDATE_DATE_TIME) >= " + Util.date2SQLString(valDatetime.add(-1));

                TradeArray allTradesArray = DSConnection.getDefault().getRemoteTrade().getTrades(from, where, null, null);

                for (int i = 0; i < allTradesArray.size(); i++) {
                    if (!(SantEmirUtil.isInternal(allTradesArray.get(i))) && !(allTradeIds.contains(allTradesArray.get(i).getLongId()))) {
                        // get audit values
                        where = "entity_field_name in('MODKEY#MC_CONTRACT_NUMBER','_status') AND entity_class_name='Trade' AND TRUNC(modif_date) = "
                                + Util.date2SQLString(valDatetime) + " AND entity_id = " + allTradesArray.get(i).getLongId();

                        Vector<AuditValue> auditValues = DSConnection.getDefault().getRemoteTrade().getAudit(where, "version_num DESC, modif_date ASC", null);

                        for (AuditValue av : auditValues) {
                            av.getEntityId();
                            FieldModification fm = av.getField();
                            if ("MODKEY#MC_CONTRACT_NUMBER".equals(fm.getName())) {
                                auditContractChangeTradesIds.put(av.getEntityId(), fm);
                            }
                            if ("_status".equals(fm.getName()) && AUDIT_STATUSES.contains(fm.getNewValue()) && "VERIFIED".equals(fm.getOldValue())) {
                                auditStatusChangeTradesIds.put(av.getEntityId(), fm);
                            }
                        }
                        allTradeIds.add(allTradesArray.get(i).getLongId());
                        allTrades.put(allTradesArray.get(i).getLongId(), allTradesArray.get(i));
                    }
                }
                for (ReportRow row : reportRows) {
                    MarginCallDetailEntryDTO detailEntry = row.getProperty("Default");

                    // se excluyen los trades para los mensajes CLM de contratos con cavalsa
                    CollateralConfig col_conf = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), detailEntry.getMarginCallConfigId());

                    if ((col_conf != null) && (isTradeReportable(col_conf))) {
                        SantEmirCLM newSantEMIRCLM = new SantEmirCLM(detailEntry);
                        row.setProperty("Default", newSantEMIRCLM);

                        if (!allTradeIds.contains(detailEntry.getTradeId())) {
                            continue;
                        }
                        if (auditStatusChangeTradesIds.containsKey(detailEntry.getTradeId())) {
                            row.setProperty("StatusChange", auditStatusChangeTradesIds.get(detailEntry.getTradeId()));
                        }
                        if (!alreadyAddedTradeIds.contains(detailEntry.getTradeId())) {
                            newReportRows.add(row);
                            alreadyAddedTradeIds.add(detailEntry.getTradeId());
                        }
                        if (!contractIds.contains(detailEntry.getMarginCallConfigId())) {
                            contractIds.add(detailEntry.getMarginCallConfigId());
                        }
                        if (auditContractChangeTradesIds.containsKey(detailEntry.getTradeId())) {
                            newSantEMIRCLM = new SantEmirCLM(detailEntry);
                            newSantEMIRCLM.setId(-detailEntry.getId());
                            ReportRow newRow = row.clone();
                            newRow.setProperty("Default", newSantEMIRCLM);
                            newRow.setProperty("ContratChange", auditContractChangeTradesIds.get(detailEntry.getTradeId()));
                            newReportRows.add(newRow);
                        }
                    }
                }
                for (ReportRow row : newReportRows) {
                    SantEmirCLM santEMIRCLM = row.getProperty("Default");
                    long tradeId = santEMIRCLM.getMarginCallDetailEntyDTO().getTradeId();
                    if (allTrades.get(tradeId) != null) {
                        santEMIRCLM.setTrade(allTrades.get(tradeId));
                    }
                    row.setProperty("SantEmirCLMReportItem", createItem(santEMIRCLM.getTrade(), santEMIRCLM.getMarginCallDetailEntyDTO(),
                            CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                                    santEMIRCLM.getMarginCallDetailEntyDTO().getMarginCallConfigId()), valDatetime));
                }
            } catch (RemoteException e) {
                Log.error(SantEmirCLMReport.class.getName(), e);
                return null;
            }
        }
        dro.setRows(newReportRows.toArray(new ReportRow[newReportRows.size()]));

        output.setRows(dro.getRows());
        return output;
    }

    /**
     * @param reportTemplate .
     * @return set of POs admited, first by ST or by the collection of IDs from the core report
     */
    @SuppressWarnings("rawtypes")
    private String filterPoCollectionByTemplate(final ReportTemplate reportTemplate) {
        Object obj = reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

        if (obj == null) {
            obj = reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS_COL);
        }
        if (obj != null) {
            if (obj instanceof String) {
                return (String) obj;
            } else if (obj instanceof Collection) {
                Collection c = (Collection) obj;
                if (!Util.isEmpty(c)) {
                    return Util.collectionToEscapedString(c, ",");
                }
            }
        }
        return null;
    }

    /**
     * @param col .
     * @return the sql string to search the PO owner of a contract in the trade
     */
    // GSM 31/07/15. SBNA Multi-PO filter
    @SuppressWarnings("unused")
    private String appendPOFilterSQL(Collection<String> col) {
        final StringBuffer sb = new StringBuffer();

        sb.append(" AND trade_keyword.trade_id = trade.trade_id AND trade_keyword.keyword_name =");
        sb.append(Util.string2SQLString(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
        sb.append(" AND to_number(trade_keyword.keyword_value) = mrgcall_config.mrg_call_def AND");
        sb.append(" mrgcall_config.process_org_id=legal_entity.legal_entity_id AND");
        sb.append(" legal_entity.legal_entity_id IN ").append(Util.collectionToSQLString(col));

        return sb.toString();
    }

    /**
     * create the item by a given trade
     *
     * @param trade trade
     */
    private SantEmirCLMReportItem createItem(final Trade trade, final MarginCallDetailEntryDTO mcd, CollateralConfig colaterallConfig, JDatetime valDatetime) {
        SantEmirCLMReportLogic emirLogic;
        SantEmirCLMReportItem item = new SantEmirCLMReportItem(trade);
        try {
            emirLogic = new SantEmirCLMReportLogic(trade, mcd, valDatetime, colaterallConfig, DSConnection.getDefault().getUserDefaults().getHolidays());
            item = emirLogic.fillItem();
        } catch (RemoteException e) {
            Log.error(this, e);
        }
        return item;
    }

    /**
     * For knowing if the trade is reportable or not depending on its contract.
     *
     * @param cc CollateralConfig
     * @return true or false
     */
    private boolean isTradeReportable(final CollateralConfig cc) {
        boolean rst = false;

        if (!("WK15".equals(cc.getProcessingOrg().getAuthName()))) {
            if (("HEAD").equals(cc.getAdditionalField("HEAD_CLONE"))) {
                rst = true;
            } else if (("CLONE").equals(cc.getAdditionalField("HEAD_CLONE"))
                    && (Util.isEmpty(cc.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE"))
                    || ("YES").equalsIgnoreCase(cc.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE")))) {
                rst = true;
            }
        }
        return rst;
    }
}

