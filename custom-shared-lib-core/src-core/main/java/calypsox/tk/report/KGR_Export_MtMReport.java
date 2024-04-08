package calypsox.tk.report;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.core.SantPricerMeasure.S_NPV_BASE;

public class KGR_Export_MtMReport extends SantReport {
    /**
     * Serial UID
     */
    private static final long serialVersionUID = 3116641869367277639L;

    public static final String KGR_EXPORT_MTM_REPORT = "KGR_Export_MtMReport";
    public static final String DATE_EXPORT = "Date to export";
    public static final String FORMAT_EXPORT = "Format date to export";
    public static final String EXCLUDED_TRANSFER_TYPE_CONTRACT_IA = "CONTRACT_IA";
    public static final String EXCLUDED_TRANSFER_TYPE_DISPUTE_ADJUSTMENT = "DISPUTE_ADJUSTMENT";
    private static final String EXCLUDE_USERS = "KGR_MtM_Report.ExcludeUsers";

    @Override
    @SuppressWarnings("unchecked")
    public ReportOutput loadReport(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

        if (this._reportTemplate == null) {
            return null;
        }

        final Vector<String> errorMsgs = errorMsgsP;

        // initDates();
        final DefaultReportOutput output = new DefaultReportOutput(this);

        String sqlQuery = "select trade.trade_id, bo_audit.modif_date, bo_audit.OLD_VALUE, bo_audit.NEW_VALUE, "
                + "bo_audit.USER_NAME, pl_mark.VALUATION_DATE, bo_audit.entity_id ";

        final HashSet<String> fromSet = new HashSet<String>();
        String where;
        //GSM 23/03/2016 - v14 Migration - small adaptation
        where = buildWhere(fromSet, errorMsgsP);

        String from = " from ";
        from = from + Util.collectionToString(fromSet, ", ");

        sqlQuery = sqlQuery + from + where;

        // Get Audit Info and create UserAuditItem objects
        List<SantMTMAuditItem> mtmAuditItems = new ArrayList<>();
        try {
            mtmAuditItems = getMTMAuditItems(sqlQuery, errorMsgs);
            buildRates(mtmAuditItems);

        } catch (final RemoteException e) {
            Log.error(SantMTMAuditReport.class, "Error loading MTM Audit Items", e);
        }

        final List<ReportRow> reportRows = new ArrayList<>();

        List<Long> calypsoIds = new ArrayList<>();
        for (final SantMTMAuditItem item : mtmAuditItems) {
            if (!calypsoIds.contains(item.getTrade().getLongId()) && !isSecLendingFicticio(item.getTrade())) {
                calypsoIds.add(item.getTrade().getLongId());
                if (item.getMarkName().equals(S_NPV_BASE)) {
                    final ReportRow row = new ReportRow(item);
                    row.setProperty(ReportRow.DEFAULT, null);
                    row.setProperty(ReportRow.TRADE, item.getTrade());
                    row.setProperty(ReportRow.MARGIN_CALL_CONFIG, item.getMarginCallConfig());
                    row.setProperty(SantMTMAuditItem.SANT_MTM_AUDIT_ITEM, item);
                    row.setProperty(ReportRow.PL_MARK, item.getPlMark());
                    row.setProperty(SantTradeBrowserReportTemplate.VAL_DATE, JDate.valueOf(item.getMtmValDate()));
                    reportRows.add(row);
                }
            }
        }

        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
        return output;
    }

    /*
     * Date entered in the Report is ProcessDate and we need to convert this to ValDate
     */
    private JDate convertProcessDateToValDate(JDate procDate) {
        int calculationOffSet = ServiceRegistry.getDefaultContext().getValueDateDays() * -1;
        //BAU ACD
        Vector<String> holiday_calendar = getReportTemplate().getHolidays();
        if (Util.isEmpty(holiday_calendar)) {
            holiday_calendar = new Vector<>();
            holiday_calendar.add("SYSTEM");
        }
        JDate valDate = procDate.addBusinessDays(calculationOffSet, holiday_calendar);

//		JDate valDate = Holiday.getCurrent().addBusinessDays(procDate,
//				DSConnection.getDefault().getUserDefaults().getHolidays(), calculationOffSet);

        return valDate;
    }

    private ArrayList<SantMTMAuditItem> getMTMAuditItems(final String sql,
                                                         @SuppressWarnings("rawtypes") final Vector errormsgs) throws RemoteException {

        ArrayList<SantMTMAuditItem> mtmAuditItems = new ArrayList<SantMTMAuditItem>();

        final RemoteSantReportingService santReportingService = SantReportingUtil.getSantReportingService(DSConnection
                .getDefault());
        ArrayList<SantMTMAuditItem> tempMtmAuditItems = santReportingService.getMTMAuditItems(sql, true);

        // Filter out non NPV_BASE ones
        for (final SantMTMAuditItem item : tempMtmAuditItems) {
            if (item.getMarkName().equals(S_NPV_BASE)) {
                mtmAuditItems.add(item);
            }
        }

        return mtmAuditItems;
    }

    private void buildRates(List<SantMTMAuditItem> mtmAuditItems) {

        Map<JDate, List<QuoteValue>> quotesValuesMap = loadQuoteValues(mtmAuditItems);

        for (final SantMTMAuditItem item : mtmAuditItems) {
            JDate mtmValDate = JDate.valueOf(item.getMtmValDate());
            List<QuoteValue> QuoteValues = quotesValuesMap.get(mtmValDate);
            item.buildFXRates(QuoteValues);
        }
    }

    private Map<JDate, List<QuoteValue>> loadQuoteValues(List<SantMTMAuditItem> mtmAuditItems) {
        SantGenericQuotesLoader quotesLoaderThread = null;

        HashMap<JDate, List<QuoteValue>> map = new HashMap<JDate, List<QuoteValue>>();
        for (final SantMTMAuditItem item : mtmAuditItems) {
            JDate mtmValDate = JDate.valueOf(item.getMtmValDate());
            if (map.get(mtmValDate) == null) {
                // Load Quotes here for that day.
                quotesLoaderThread = new SantGenericQuotesLoader(false, getFXQuotesSqlWhereList(mtmValDate));
                quotesLoaderThread.load();
                map.put(mtmValDate, quotesLoaderThread.getDataAsList());
            }
        }

        return map;
    }

    protected ArrayList<String> getFXQuotesSqlWhereList(JDate valDate) {
        ArrayList<String> whereList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(" quote_set_name= '")
                .append(getPricingEnv().getQuoteSet().getParent().getName()).append("'")
                .append(" and quote_name like 'FX.%' and length(quote_name) = 10 and TRUNC(quote_date) = ")
                .append(Util.date2SQLString(valDate));

        whereList.add(sb.toString());
        return whereList;
    }

    protected String buildWhere(final HashSet<String> fromSet, final Vector<String> errorMsgsP) {
        if (this._reportTemplate == null) {
            return null;
        }

        final ReportTemplate h = this._reportTemplate;
        final StringBuffer where = new StringBuffer(" WHERE ");
        String s = null;

        final String tradeTable = "trade";
        fromSet.add(tradeTable);

        // Trade ID - It is actually trade External Reference
        s = (String) h.get(SantGenericTradeReportTemplate.TRADE_ID);
        if (!Util.isEmpty(s)) {

            if (where.length() > 7) {
                where.append(" AND ");
            }

            where.append("trade.external_reference=" + Util.string2SQLString(s));
        }

        s = (String) h.get(SantGenericTradeReportTemplate.COUNTERPARTY);
        if (!Util.isEmpty(s)) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" trade.cpty_id IN (").append(s).append(")");
        }

        s = (String) h.get(SantGenericTradeReportTemplate.OWNER_DEALS);
        if (!Util.isEmpty(s)) {

            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" trade.book_id = book.book_id AND");
            where.append(" book.legal_entity_id = legal_entity.legal_entity_id AND");
            where.append(" legal_entity.legal_entity_id IN (");
            where.append(s);
            where.append(")");

            fromSet.add("book");
            fromSet.add("legal_entity");
        }

        s = (String) h.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(s)) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" trade.internal_reference in (" + s + ")");
        }

        // 19/08/15. SBNA Multi-PO filter adapted to ST filter too
        s = CollateralUtilities.filterPoIdsByTemplate(h);

        if (!Util.isEmpty(s)) {

            Vector<String> posIds = Util.string2Vector(s);

            if (!Util.isEmpty(posIds)) {
                if (where.length() > 7) {
                    where.append(" AND ");
                }
                where.append(" trade_keyword.trade_id = trade.trade_id AND trade_keyword.keyword_name = ");
                where.append(Util.string2SQLString(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
                where.append(" AND to_number(trade_keyword.keyword_value) = mrgcall_config.mrg_call_def AND");
                where.append(" mrgcall_config.process_org_id=legal_entity.legal_entity_id AND");
                where.append(" legal_entity.legal_entity_id IN ");
                where.append(Util.collectionToSQLString(posIds)).append(" ");

                fromSet.add("trade_keyword");
                fromSet.add("mrgcall_config");
                fromSet.add("legal_entity");
            }
        }

        JDate valDateStart = convertProcessDateToValDate(getProcessStartDate());
        JDate valDateEnd = convertProcessDateToValDate(getProcessEndDate());
        if (where.length() > 7) {
            where.append(" AND ");
        }
        // Users wanted only Live Trades and NPV pricer measure
        where.append(" trade.trade_status not in ('CANCELED', 'MATURED') AND pl_mark.trade_id=trade.trade_id AND pl_mark.mark_id=bo_audit.entity_id "
                + "AND entity_class_name='PLMark' AND entity_field_name like '_markValues.AMEND%' "
                + " AND trunc(pl_mark.VALUATION_DATE) between "
                + Util.date2SQLString(valDateStart)
                + " AND "
                + Util.date2SQLString(valDateEnd));

        fromSet.add("pl_mark");
        fromSet.add("bo_audit");

        final Boolean manuallyModified = (Boolean) h.get(SantGenericTradeReportTemplate.MANUALLY_MODIFIED);

        //GSM 23/03/2016 - v14 Migration - small adaptation
        if ((manuallyModified != null) && (manuallyModified.booleanValue() == true)) {
            //BAU fixing 10/12/2019 - we get the users we do not want to show their changes from a DomainValue
            // instead of getting it from the properties file.

            final Vector<String> systemUsersDV = LocalCache.getDomainValues(DSConnection.getDefault(), EXCLUDE_USERS);
            if (!Util.isEmpty(systemUsersDV)) {
                if (where.length() > 7) {
                    where.append(" AND ");
                    final String systemUsersSqlStr = Util.collectionToSQLString(systemUsersDV);
                    where.append(" bo_audit.user_name not in " + systemUsersSqlStr);
                }
            } else {
                errorMsgsP.add("DomainValue KGR_MtM_Report.ExcludeUsers is empty");
            }

        }
        return where.toString();
    }

    private boolean isSecLendingFicticio(Trade trade) {
        if ("FICTICIO".equalsIgnoreCase(trade.getKeywordValue("SecLendingTrade"))) {
            return true;
        }
        return false;
    }

//	public static void main(final String... args) throws ConnectException {
//		ConnectionUtil.connect(args, "Test");
//
//		new SantMTMAuditReport();
//	}
}
