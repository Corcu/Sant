package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantTradesLoader;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class SantSecuritiesFlowReport extends SantReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 4565635551545419683L;

    private ReportTemplate template;

    @Override
    protected String getCustomProcessDateName() {
        return "Trade";
    }

    @Override
    protected boolean checkCustomProcessStartDate() {
        return true;
    }

    @Override
    protected boolean checkCustomProcessEndDate() {
        return true;
    }

    @Override
    protected ReportOutput loadReport(Vector<String> errorMsgs) {
        try {
            return getReportOutput();
        } catch (final Exception e) {
            Log.error(this, "Cannot get the output", e);
        }

        return null;
    }

    /**
     * Generate the report output. Get the info that its will be shown in the
     * report.
     *
     * @return ReportOutput report output
     * @throws Exception
     */
    private ReportOutput getReportOutput() throws Exception {
        final DefaultReportOutput output = new DefaultReportOutput(this);
        this.template = getReportTemplate();
        final JDate valDate = getValDate();
        final SantTradesLoader loader = new SantTradesLoader();

        JDate valueStartDate = this.template.get("ValueDateStartRange");
        if (valueStartDate == null) {
            valueStartDate = getDate(null, "", (String) this.template.get("ValueDateStartRangePlus"),
                    (String) this.template.get("ValueDateStartRangeTenor"));
        }

        JDate valueEndDate = this.template.get("ValueDateEndRange");
        if (valueEndDate == null) {
            valueEndDate = getDate(null, "", (String) this.template.get("ValueDateEndRangePlus"),
                    (String) this.template.get("ValueDateEndRangeTenor"));
        }

        this.template.put("ValueDateStartRange", valueStartDate);
        this.template.put("ValueDateEndRange", valueEndDate);

        JDate tradeStartDate = this.template.get("TradeDateStartRange");
        if (tradeStartDate == null) {
            tradeStartDate = getDate(null, "", (String) this.template.get("TradeDateStartRangePlus"),
                    (String) this.template.get("TradeDateStartRangeTenor"));
        }

        JDate tradeEndDate = this.template.get("TradeDateEndRange");
        if (tradeEndDate == null) {
            tradeEndDate = getDate(null, "", (String) this.template.get("TradeDateEndRangePlus"),
                    (String) this.template.get("TradeDateEndRangeTenor"));
        }
        this.template.put("TradeDateStartRange", tradeStartDate);
        this.template.put("TradeDateEndRange", tradeEndDate);

        final StringBuffer whereTemplate = buildWhereTemplateTrades(this.template);
        final Vector<String> fromTemplate = buildFromTemplateTrades(this.template);

        final Collection<SantTradeBrowserItem> tradeBrowserItems = loader.getTradeBrowserItems("MarginCall", "SECURITY",
                whereTemplate, fromTemplate);

        final List<ReportRow> rows = new ArrayList<>();

        if (!Util.isEmpty(tradeBrowserItems)) {
            for (final SantTradeBrowserItem item : tradeBrowserItems) {

                Trade trade = item.getTrade();
                if ((trade != null) && (isValidStatus(trade))) {
                    final CollateralConfig marginCall = loader.getMarginCallConfig(trade.getLongId());

                    setRowPorperties(trade, marginCall, valDate, rows);
                }
            }
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    /**
     * Sets row properties (Trade, SantMarginCallConfig, Bond, valDate and
     * PricingEnv) for security product
     *
     * @param trade
     * @param marginCall
     * @param valDate
     * @param rows
     */
    private void setRowPorperties(final Trade trade, final CollateralConfig marginCall, final JDate valDate,
                                  final List<ReportRow> rows) {

        Product security = getSecurity(trade);
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            if (isMatchingCriteria(marginCall, trade, bond)) {
                final ReportRow row = new ReportRow(trade, "Trade");
                row.setProperty("SantMarginCallConfig", marginCall);
                row.setProperty("Bond", bond);
                row.setProperty("valDate", valDate);
                row.setProperty("PricingEnv", getPricingEnv());
                rows.add(row);
            }
        } else if (security instanceof Equity) {
            Equity equity = (Equity) security;
            if (isMatchingCriteriaEquity(marginCall, trade, equity)) {
                final ReportRow row = new ReportRow(trade, "Trade");
                row.setProperty("SantMarginCallConfig", marginCall);
                row.setProperty("Equity", equity);
                row.setProperty("valDate", valDate);
                row.setProperty("PricingEnv", getPricingEnv());
                rows.add(row);
            }
        }
    }

    /**
     * @param trade
     * @return retrieve security from the product MarginCall
     */
    private Product getSecurity(final Trade trade) {
        Product security = null;
        Product product = trade.getProduct();
        if (product instanceof MarginCall) {
            MarginCall marginCallProduct = (MarginCall) product;
            security = marginCallProduct.getSecurity();
        }
        return security;
    }

    private boolean isMatchingCriteria(final CollateralConfig marginCall, final Trade trade, final Bond bond) {
        boolean resultPo = false;
        boolean resultPoAgre = false;
        boolean resultBaseCcy = false;
        boolean resultBond = false;
        boolean resultBuySell = false;

        // PO Owner Agreement
        final String poDealsStr = (String) this._reportTemplate.get(SantGenericTradeReportTemplate.OWNER_DEALS);
        Vector<String> poDealsIds = null;
        if (!Util.isEmpty(poDealsStr)) {
            poDealsIds = Util.string2Vector(poDealsStr);
            for (final String po : poDealsIds) {
                if (Integer.valueOf(trade.getBook().getProcessingOrgBasedId()).toString().equals(po)) {
                    resultPoAgre = true;
                }
            }
        } else {
            resultPoAgre = true;
        }

        // PO Owner Agreement (Contracts)
        // 27/07/15. SBNA Multi-PO filter
        final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(this._reportTemplate);
        // final String poAgrStr = (String)
        // this._reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        Vector<String> poAgrIds = null;
        if (!Util.isEmpty(poAgrStr)) {
            poAgrIds = Util.string2Vector(poAgrStr);
            for (final String po : poAgrIds) {
                if (Integer.valueOf(marginCall.getPoId()).toString().equals(po)) {
                    resultPo = true;
                }
            }
        } else {
            resultPo = true;
        }

        // Base Ccy
        final String baseCcy = this._reportTemplate.get(SantGenericTradeReportTemplate.BASE_CURRENCY);
        if (!Util.isEmpty(baseCcy)) {
            if (bond.getCurrency().equals(baseCcy)) {
                resultBaseCcy = true;
            }
        } else {
            resultBaseCcy = true;
        }

        // Bond
        final String bondStr = this._reportTemplate.get("Bonds");
        Vector<String> bondIds = null;
        if (!Util.isEmpty(bondStr)) {
            bondIds = Util.string2Vector(bondStr);
            for (final String bondId : bondIds) {
                if (bond.getSecCode("ISIN").equals(bondId)) {
                    resultBond = true;
                }
            }
        } else {
            resultBond = true;
        }

        // Buy/Sell
        final String buySellStr = this._reportTemplate.get("BuySell");
        if (!Util.isEmpty(buySellStr)) {
            final int result = bond.getBuySell(trade);
            if (result > 0) {
                if ("Sell".equals(buySellStr)) {
                    resultBuySell = true;
                }
            } else {
                if ("Buy".equals(buySellStr)) {
                    resultBuySell = true;
                }
            }
        } else {
            resultBuySell = true;
        }

        if (resultPo && resultPoAgre && resultBaseCcy && resultBond && resultBuySell
                && !"TRUE".equals(trade.getKeywordValue("IS_MIGRATION"))) {
            return resultPo && resultPoAgre && resultBaseCcy && resultBond && resultBuySell;
        } else {
            return false;
        }
    }

    private boolean isMatchingCriteriaEquity(final CollateralConfig marginCall, final Trade trade,
                                             final Equity equity) {
        boolean resultPo = false;
        boolean resultPoAgre = false;
        boolean resultBaseCcy = false;
        boolean resultBond = false;
        boolean resultBuySell = false;

        // PO Owner Agreement
        final String poDealsStr = this._reportTemplate.get(SantGenericTradeReportTemplate.OWNER_DEALS);
        Vector<String> poDealsIds = null;
        if (!Util.isEmpty(poDealsStr)) {
            poDealsIds = Util.string2Vector(poDealsStr);
            for (final String po : poDealsIds) {
                if (Integer.valueOf(trade.getBook().getProcessingOrgBasedId()).toString().equals(po)) {
                    resultPoAgre = true;
                }
            }
        } else {
            resultPoAgre = true;
        }

        // PO Owner Agreement (Contracts)
        // 27/07/15. SBNA Multi-PO filter
        final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(this._reportTemplate);
        // final String poAgrStr = (String)
        // this._reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        Vector<String> poAgrIds = null;
        if (!Util.isEmpty(poAgrStr)) {
            poAgrIds = Util.string2Vector(poAgrStr);
            for (final String po : poAgrIds) {
                if (Integer.valueOf(marginCall.getPoId()).toString().equals(po)) {
                    resultPo = true;
                }
            }
        } else {
            resultPo = true;
        }

        // Base Ccy
        final String baseCcy = this._reportTemplate.get(SantGenericTradeReportTemplate.BASE_CURRENCY);
        if (!Util.isEmpty(baseCcy)) {
            if (equity.getCurrency().equals(baseCcy)) {
                resultBaseCcy = true;
            }
        } else {
            resultBaseCcy = true;
        }

        // Bond
        final String bondStr = this._reportTemplate.get("Bonds");
        Vector<String> bondIds = null;
        if (!Util.isEmpty(bondStr)) {
            bondIds = Util.string2Vector(bondStr);
            for (final String bondId : bondIds) {
                if (equity.getSecCode("ISIN").equals(bondId)) {
                    resultBond = true;
                }
            }
        } else {
            resultBond = true;
        }

        // Buy/Sell
        final String buySellStr = this._reportTemplate.get("BuySell");
        if (!Util.isEmpty(buySellStr)) {
            final int result = equity.getBuySell(trade);
            if (result > 0) {
                if ("Sell".equals(buySellStr)) {
                    resultBuySell = true;
                }
            } else {
                if ("Buy".equals(buySellStr)) {
                    resultBuySell = true;
                }
            }
        } else {
            resultBuySell = true;
        }

        if (resultPo && resultPoAgre && resultBaseCcy && resultBond && resultBuySell
                && !"TRUE".equals(trade.getKeywordValue("IS_MIGRATION"))) {
            return resultPo && resultPoAgre && resultBaseCcy && resultBond && resultBuySell;
        } else {
            return false;
        }
    }

    private Vector<String> buildFromTemplateTrades(final ReportTemplate template) {
        final Vector<String> from = new Vector<>();
        // 29/07/15. SBNA Multi-PO filter
        from.add("legal_entity");
        return from;
    }

    private StringBuffer buildWhereTemplateTrades(final ReportTemplate template) {
        final StringBuffer whereTemplate = new StringBuffer("");
        // Trade Date
        final JDate tradeDateStart = template.get("TradeDateStartRange");
        final JDate tradeDateEnd = template.get("TradeDateEndRange");
        if (tradeDateStart != null) {
            whereTemplate.append(" AND trade.trade_date_time >= to_date('");
            whereTemplate.append(tradeDateStart);
            whereTemplate.append(" 00:00:00','dd/MM/yyyy HH24:MI:SS')");
        }
        if (tradeDateEnd != null) {
            whereTemplate.append(" AND trade.trade_date_time <= to_date('");
            whereTemplate.append(tradeDateEnd);
            whereTemplate.append(" 23:59:59','dd/MM/yyyy HH24:MI:SS')");
        }

        // Value Date
        final JDate valueDateStart = template.get("ValueDateStartRange");
        final JDate valueDateEnd = template.get("ValueDateEndRange");
        if (valueDateStart != null) {
            whereTemplate.append(" AND trade.settlement_date >= to_date('");
            whereTemplate.append(valueDateStart);
            whereTemplate.append(" 00:00:00','dd/MM/yyyy HH24:MI:SS')");
        }
        if (valueDateEnd != null) {
            whereTemplate.append(" AND trade.settlement_date <= to_date('");
            whereTemplate.append(valueDateEnd);
            whereTemplate.append(" 23:59:59','dd/MM/yyyy HH24:MI:SS')");
        }

        // Counterparty
        final String s = template.get(SantGenericTradeReportTemplate.COUNTERPARTY);
        if (!Util.isEmpty(s)) {
            buildCptyQuery(whereTemplate, s);
        }

        // PortFolio
        final String portFolioStr = template.get(SantGenericTradeReportTemplate.PORTFOLIO);
        Vector<String> portFolioIds = null;
        if (!Util.isEmpty(portFolioStr)) {
            portFolioIds = Util.string2Vector(portFolioStr);
            whereTemplate.append(" AND trade.book_id in ");
            whereTemplate.append(Util.collectionToSQLString(portFolioIds));
        }

        return whereTemplate;
    }

    @SuppressWarnings("unchecked")
    private void buildCptyQuery(final StringBuffer whereTemplate, final String s) {
        final Vector<String> ids = Util.string2Vector(s);

        if (ids.size() < ioSQL.MAX_ITEMS_IN_LIST) {
            whereTemplate.append(" AND trade.cpty_id IN (").append(Util.collectionToString(ids)).append(")");
        } else {
            final List<String> idsStrList = ioSQL.returnStringsOfStrings(ids);
            whereTemplate.append("(AND trade.cpty_id IN (").append(idsStrList.get(0)).append(")");
            for (int i = 1; i < idsStrList.size(); i++) {
                whereTemplate.append(" OR trade.cpty_id IN (").append(idsStrList.get(i)).append(")");
            }
            whereTemplate.append(")");
        }
    }


    private boolean isValidStatus(final Trade trade) {
        boolean result = false;
        final Status tradeStatus = trade.getStatus();

        if ("VERIFIED".equals(tradeStatus.toString()) || "SENT".equals(tradeStatus.toString())
                || "EXPORTED".equals(tradeStatus.toString()) || "ERROR".equals(tradeStatus.toString())) {
            result = true;
        }
        return result;
    }

    private JDate getDate(final JDate jdate, final String s, final String s1, final String s2) {
        if (this.template == null) {
            return null;
        }
        final String s3 = s;
        final String s4 = s1;
        final String s5 = s2;

        if (Util.isEmpty(s3) && Util.isEmpty(s4) && Util.isEmpty(s5)) {
            return null;
        }
        try {
            if (!Util.isEmpty(s3)) {
                return Util.stringToJDate(s3);
            }
        } catch (final Exception exception) {
            Log.error(this, exception);//Sonar
            return getValDate();
        }

        return adjustDate(this.template, getValDate(), s4, s5);
    }

    @SuppressWarnings("rawtypes")
    protected static JDate adjustDate(final ReportTemplate reporttemplate, JDate jdate, final String s, final String s1) {
        if (Util.isEmpty(s1)) {
            return jdate;
        }
        Vector vector = null;
        boolean flag = false;
        if (reporttemplate != null) {
            vector = reporttemplate.getHolidays();
            flag = reporttemplate.getBusDays();
        }
        final Vector vector1 = LocalCache.getDomainValues(DSConnection.getDefault(), "DateRuleReportTemplate");
        if (!Util.isEmpty(vector1) && vector1.contains(s1)) {
            final DateRule daterule = BOCache.getDateRule(DSConnection.getDefault(), s1);
            if (daterule != null) {
                if (s.equals("-")) {
                    jdate = daterule.previous(jdate);
                } else {
                    jdate = daterule.next(jdate);
                }
                return jdate;
            }
        }
        final Tenor tenor = new Tenor(s1);
        if (vector != null) {
            if ((s != null) && s.equals("-")) {
                if (!flag) {
                    return Holiday.getCurrent().addCalendarDays(jdate, vector, -1 * tenor.getCode());
                } else {
                    return Holiday.getCurrent().addBusinessDays(jdate, vector, -1 * tenor.getCode());
                }
            }
            if (!flag) {
                return Holiday.getCurrent().addCalendarDays(jdate, vector, tenor.getCode());
            } else {
                return Holiday.getCurrent().addBusinessDays(jdate, vector, tenor.getCode());
            }
        }
        if (s1.trim().length() > 0) {
            if ((s != null) && s.equals("-")) {
                jdate = jdate.substractTenor(tenor);
            } else {
                jdate = jdate.addTenor(tenor);
            }
        }
        return jdate;
    }

}
