package calypsox.tk.report;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import static calypsox.tk.core.SantPricerMeasure.S_NPV_BASE;
import static com.calypso.tk.core.PricerMeasure.S_NPV;

public class SantMTMAuditReport extends Report {

    private static final long serialVersionUID = -6983394742452896939L;

    public static final String SANT_MTM_AUDIT_REPORT = "SantMTMAuditReport";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    @SuppressWarnings({"unused", "unchecked"})
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

        if (this._reportTemplate == null) {
            return null;
        }
        final Vector<String> errorMsgs = errorMsgsP;

        // initDates();
        final DefaultReportOutput output = new DefaultReportOutput(this);

        final String mtmValDateStr = (String) this._reportTemplate.get(SantMTMAuditReportTemplate.MTM_VAL_DATE);
        if (Util.isEmpty(mtmValDateStr)) {
            // AppUtil.displayError("Please neter MTM Value date", this);
            return null;
        }

        final JDate mtmValDate = Util.stringToJDate(mtmValDateStr);

        String sqlQuery = "select trade.trade_id, bo_audit.modif_date, bo_audit.OLD_VALUE, bo_audit.NEW_VALUE, "
                + "bo_audit.USER_NAME, pl_mark.VALUATION_DATE, bo_audit.entity_id ";

        final String where = buildWhere();
        String from = buildFrom();

        if (Util.isEmpty(from)) {
            from = " from trade" + from + ", bo_audit, pl_mark ";
        } else {
            from = " from trade," + from + ", bo_audit, pl_mark ";
        }

        sqlQuery = sqlQuery + from + where;

        // Get All user Info and create UserAuditItem objects
        ArrayList<SantMTMAuditItem> mtmAuditItems = new ArrayList<SantMTMAuditItem>();
        try {
            mtmAuditItems = getMTMAuditItems(sqlQuery, errorMsgs);

        } catch (final RemoteException e) {
            Log.error(SantMTMAuditReport.class, "Error loading MTM Audit Items", e);
        }

        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

        for (final SantMTMAuditItem item : mtmAuditItems) {
            if (item.getMarkName().equals(S_NPV) || item.getMarkName().equals(S_NPV_BASE)) {
                final ReportRow row = new ReportRow(item);
                row.setProperty(ReportRow.TRADE, item.getTrade());
                row.setProperty(ReportRow.PL_MARK, item.getPlMark());
                row.setProperty(ReportRow.MARGIN_CALL_CONFIG, item.getMarginCallConfig());
                reportRows.add(row);
            }
        }

        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
        return output;
    }

    @SuppressWarnings("rawtypes")
    private ArrayList<SantMTMAuditItem> getMTMAuditItems(final String sql, final Vector errormsgs)
            throws RemoteException {
        // @TODO
        final RemoteSantReportingService santReportingService = SantReportingUtil.getSantReportingService(DSConnection
                .getDefault());
        return santReportingService.getMTMAuditItems(sql, true);

    }

    public String formatDate(final JDatetime dateTime) {
        if (dateTime == null) {
            return null;
        }
        final DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("CET"));
        return formatter.format(dateTime);
    }

    @SuppressWarnings({"deprecation", "rawtypes"})
    protected String buildWhere() {
        if (this._reportTemplate == null) {
            return null;
        }
        final ReportTemplate h = this._reportTemplate;
        final StringBuffer where = new StringBuffer(" WHERE ");
        String s = null;

        // Trade ID
        s = (String) h.get(TradeReportTemplate.TRADE_ID);
        if (!Util.isEmpty(s)) {
            long tradeId = 0;
            try {
                tradeId = Util.getInteger(s);
            } catch (final Exception e) {
                tradeId = 0;
                Log.error(this, e);//Sonar
            }
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" trade.trade_id = " + tradeId);
        }

        s = (String) this._reportTemplate.get(SantMTMAuditReportTemplate.AGR_IDS);
        if (!Util.isEmpty(s)) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" trade.internal_reference in (" + s + ")");
        }

        // Trade Date
        String dateString = " trade.trade_date_time ";
        JDate date = getDate(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
                TradeReportTemplate.START_TENOR);
        if (date != null) {
            JDatetime startOfDay = new JDatetime(date, h.getTimeZone());
            startOfDay = startOfDay.add(-1, 0, 1, 0, 0);
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.datetime2SQLString(startOfDay));
        }
        date = getDate(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);
        if (date != null) {
            JDatetime endOfDay = new JDatetime(date, h.getTimeZone());
            endOfDay = endOfDay.add(0, 0, 0, 59, 999);
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.datetime2SQLString(endOfDay));
        }

        // Settlement Date
        dateString = " trade.settlement_date ";
        date = getDate(TradeReportTemplate.SETTLE_START_DATE, TradeReportTemplate.SETTLE_START_PLUS,
                TradeReportTemplate.SETTLE_START_TENOR);
        if (date != null) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.date2SQLString(date));
        }
        date = getDate(TradeReportTemplate.SETTLE_END_DATE, TradeReportTemplate.SETTLE_END_PLUS,
                TradeReportTemplate.SETTLE_END_TENOR);
        if (date != null) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.date2SQLString(date));
        }

        // Product Maturity Date
        dateString = " product_desc.maturity_date ";
        final JDate minDate = getDate(TradeReportTemplate.MATURITY_START_DATE, TradeReportTemplate.MATURITY_START_PLUS,
                TradeReportTemplate.MATURITY_START_TENOR);

        if (minDate != null) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.date2SQLString(minDate));
        }

        final JDate maxDate = getDate(TradeReportTemplate.MATURITY_END_DATE, TradeReportTemplate.MATURITY_END_PLUS,
                TradeReportTemplate.MATURITY_END_TENOR);
        if (maxDate != null) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.date2SQLString(maxDate));
        }

        s = (String) h.get(TradeReportTemplate.CPTYNAME);
        if (!Util.isEmpty(s)) {
            final Vector ids = Util.string2Vector(s);

            if (ids.size() > 0) {
                if (where.length() > 7) {
                    where.append(" AND ");
                }
                if (ids.size() < ioSQL.MAX_ITEMS_IN_LIST) {
                    where.append(" trade.cpty_id IN (").append(Util.collectionToString(ids)).append(")");
                } else {
                    final List idsStrList = ioSQL.returnStringsOfStrings(ids);
                    where.append("(trade.cpty_id IN (").append(idsStrList.get(0)).append(")");
                    for (int i = 1; i < idsStrList.size(); i++) {
                        where.append(" OR trade.cpty_id IN (").append(idsStrList.get(i)).append(")");
                    }
                    where.append(")");
                }
            }
        }

        // Book
        s = (String) h.get(TradeReportTemplate.BOOK);
        if (!Util.isEmpty(s)) {
            final Vector books = Util.string2Vector(s);
            int booknum = 0;
            for (booknum = 0; booknum < books.size(); booknum++) {
                final Book b = BOCache.getBook(getDSConnection(), (String) books.get(booknum));
                final int book_id = b.getId();
                // we only append "AND" on the first item.
                if ((where.length() > 7) && (booknum == 0)) {
                    where.append(" AND ");
                }

                if (booknum == 0) {
                    where.append(" trade.book_id IN (");
                } else {
                    where.append(", ");
                }

                where.append(book_id);
            }

            if (booknum > 0) {
                where.append(")");
            }
        }
        // 03/08/15. SBNA Multi-PO filter. Report filter only allows ONE PO, added by code for multiple
        s = CollateralUtilities.filterPoIdsByTemplate(h);
        if (Util.isEmpty(s)) {
            s = (String) h.get(TradeReportTemplate.PROCESSING_ORG);
        }
        if (!Util.isEmpty(s)) {
            final Vector ids = Util.string2Vector(s);

            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" trade.book_id = book.book_id AND");
            where.append(" book.legal_entity_id = legal_entity.legal_entity_id AND");
            where.append(" legal_entity.legal_entity_id IN (");
            where.append(Util.collectionToString(ids));
            where.append(")");
        }

        final String productTypesString = (String) this._reportTemplate.get(TradeReportTemplate.PRODUCT_TYPE);
        if (!Util.isEmpty(productTypesString)) {
            final Vector<String> productTypesVect = Util.string2Vector(productTypesString, ",");
            if ((productTypesVect != null) && (productTypesVect.size() > 0)) {
                if (where.length() > 7) {
                    where.append(" AND ");
                }

                where.append(" trade.product_id=product_desc.product_id AND product_desc.product_type in ").append(
                        Util.collectionToSQLString(productTypesVect));
            }
        }

        // MTM currency
        s = (String) h.get(SantMTMAuditReportTemplate.MTM_CURRENCY);
        if (!Util.isEmpty(s)) {
            final Vector mtmCurrVector = Util.string2Vector(s);
            if ((mtmCurrVector != null) && (mtmCurrVector.size() > 0)) {
                if (where.length() > 7) {
                    where.append(" AND ");
                }

                where.append(
                        " exists (select 1 from pl_mark_value where pl_mark_value.mark_id=pl_mark.mark_id AND pl_mark_value.currency IN ")
                        .append(Util.collectionToSQLString(mtmCurrVector)).append(")");

            }
        }

        // Change Reason
        s = (String) h.get(SantMTMAuditReportTemplate.MTM_CHANGE_REASON);
        if (!Util.isEmpty(s)) {
            if (where.length() > 7) {
                where.append(" AND ");
            }
            where.append(" new_value like '%" + s + "%' ");
        }

        if (where.length() > 7) {
            where.append(" AND ");
        }

        final String mtmValDateStr = (String) this._reportTemplate.get(SantMTMAuditReportTemplate.MTM_VAL_DATE);
        final JDate mtmValDate = Util.stringToJDate(mtmValDateStr);

        // Users wanted only Live Trades and NPV pricer measure
        where.append(" trade.trade_status not in ('CANCELED', 'MATURED') AND pl_mark.trade_id=trade.trade_id AND pl_mark.mark_id=bo_audit.entity_id "
                + "AND entity_class_name='PLMark' AND entity_field_name like '_markValues.AMEND%' "
                + " AND trunc(pl_mark.VALUATION_DATE)=to_date('"
                + this.dateFormat.format(mtmValDate.getDate())
                + "', 'MM/DD/YYYY')");

        final String systemUsers = Defaults.getProperty("SKIP_LDAP_AUTH_FOR_USERS");
        if (!Util.isEmpty(systemUsers)) {
            final String systemUsersSqlStr = systemUsers.replace(",", "','");
            where.append(" AND bo_audit.user_name not in ('" + systemUsersSqlStr + "') ");
        }

        return where.toString();
    }

    protected String buildFrom() {
        final ReportTemplate h = this._reportTemplate;
        final StringBuffer from = new StringBuffer();

        final String str = (String) h.get(TradeReportTemplate.PROCESSING_ORG);
        if (!Util.isEmpty(str)) {
            from.append("book,legal_entity");
        }

        final String productTypesString = (String) this._reportTemplate.get(TradeReportTemplate.PRODUCT_TYPE);
        if (!Util.isEmpty(productTypesString)) {
            if (from.length() > 0) {
                from.append(", ");
            }
            from.append(" product_desc ");
        }

        return from.toString();
    }

    @SuppressWarnings("unused")
    public static void main(final String... args) throws ConnectException {
        final DSConnection ds = ConnectionUtil.connect(args, "Test");

        final SantMTMAuditReport report = new SantMTMAuditReport();

    }
}