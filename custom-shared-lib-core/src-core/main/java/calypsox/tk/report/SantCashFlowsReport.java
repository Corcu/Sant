package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantTradesLoader;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TransferArray;

import java.rmi.RemoteException;
import java.util.*;

public class SantCashFlowsReport extends SantReport {

    private static final long serialVersionUID = -840811931715908290L;

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
    private ReportOutput getReportOutput() throws Exception {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final ReportTemplate template = getReportTemplate();
        final JDate valDate = super._valuationDateTime.getJDate(TimeZone.getDefault());
        final SantTradesLoader loader = new SantTradesLoader();

        final JDate startRange = getDate(template, valDate, TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        final JDate endRange = getDate(template, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
                TradeReportTemplate.END_TENOR);

        final StringBuffer whereTemplate = buildWhereTemplateTrades(template);
        final Vector<String> fromTemplate = buildFromTemplateTrades(template);

        final Collection<SantTradeBrowserItem> tradeBrowserItems = loader.getTradeBrowserItems("MarginCall",
                "COLLATERAL", whereTemplate, fromTemplate);

        final List<ReportRow> rows = new ArrayList<ReportRow>();

        if ((tradeBrowserItems != null) && (tradeBrowserItems.size() > 0)) {
            for (final SantTradeBrowserItem item : tradeBrowserItems) {
                if ((item.getTrade() != null)
                        && (isValidStatus(item.getTrade()) && (isValidTradeDate(item.getTrade(), startRange, endRange)))) {
                    MarginCall prdMarginCall = (MarginCall) item.getTrade().getProduct();
                    final CollateralConfig marginCall = CacheCollateralClient.getCollateralConfig(getDSConnection(),
                            prdMarginCall.getMarginCallId());

                    if (isMatchingCriteria(marginCall, item.getTrade())) {
                        final ReportRow row = new ReportRow(item.getTrade(), "Trade");
                        row.setProperty("SantMarginCallConfig", marginCall);
                        row.setProperty("Account", getAccount(item.getMarginCall(), item.getTrade()));
                        rows.add(row);
                    }

                }
            }
        }

        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    private boolean isMatchingCriteria(final CollateralConfig marginCall, final Trade trade) {
        boolean resultPo = false;
        boolean resultPoAgre = false;
        boolean resultAgrName = false;
        boolean resultAgrType = false;
        boolean resultCallAccount = false;

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
        // final String poAgrStr = (String) this._reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
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

        // AgrName
        final String agreementIds = (String) this._reportTemplate.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementIds)) {
            final Vector<String> agrIds = Util.string2Vector(agreementIds);
            for (final String agr : agrIds) {
                if (Integer.valueOf(marginCall.getId()).toString().equals(agr)) {
                    resultAgrName = true;
                }
            }
        } else {
            resultAgrName = true;
        }

        // Agreement type
        final String agrType = (String) this._reportTemplate.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (!Util.isEmpty(agrType)) {
            if (marginCall.getContractType().equals(agrType)) {
                resultAgrType = true;
            }
        } else {
            resultAgrType = true;
        }

        // Call Account
        final String callAccountStr = (String) this._reportTemplate.get(SantCashFlowsReportTemplate.CALL_ACCOUNT);
        Vector<String> callAccountIds = null;
        if (!Util.isEmpty(callAccountStr)) {
            callAccountIds = Util.string2Vector(callAccountStr);

            for (final String callAccount : callAccountIds) {
                if (Integer.valueOf(getAccount(marginCall, trade).getId()).toString().equals(callAccount)) {
                    resultCallAccount = true;
                }
            }
        } else {
            resultCallAccount = true;
        }

        if (resultPo && resultAgrName && resultAgrType && resultCallAccount && resultPoAgre
                && !"TRUE".equals(trade.getKeywordValue("IS_MIGRATION"))) {
            return true;
        } else {
            return false;
        }
    }

    private Vector<String> buildFromTemplateTrades(final ReportTemplate template) {
        final Vector<String> from = new Vector<String>();
        from.add("legal_entity");
        return from;
    }

    private StringBuffer buildWhereTemplateTrades(final ReportTemplate template) {
        final StringBuffer whereTemplate = new StringBuffer("");
        // Counterparty
        final String s = (String) template.get(SantGenericTradeReportTemplate.COUNTERPARTY);
        if (!Util.isEmpty(s)) {
            buildCptyQuery(whereTemplate, s);
        }

        // PortFolio
        final String portFolioStr = (String) template.get(SantGenericTradeReportTemplate.PORTFOLIO);
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

    private boolean isValidTradeDate(final Trade trade, JDate startDate, final JDate endDate) {
        final JDatetime tradeDate = trade.getTradeDate();
        boolean result = false;
        startDate = startDate.addBusinessDays(-1, getReportTemplate().getHolidays());
        if ((startDate != null) && (endDate != null)) {
            if ((tradeDate.after(startDate.getJDatetime(TimeZone.getDefault())) || tradeDate.equals(startDate
                    .getJDatetime(TimeZone.getDefault())))
                    && (tradeDate.before(endDate.getJDatetime(TimeZone.getDefault())) || tradeDate.equals(endDate
                    .getJDatetime(TimeZone.getDefault())))) {
                result = true;
            }
        } else if ((startDate == null)) {
            if (tradeDate.before(endDate.getJDatetime(TimeZone.getDefault()))
                    || tradeDate.equals(endDate.getJDatetime(TimeZone.getDefault()))) {
                result = true;
            }
        } else {
            if (tradeDate.after(startDate.getJDatetime(TimeZone.getDefault()))
                    || tradeDate.equals(startDate.getJDatetime(TimeZone.getDefault()))) {
                result = true;
            }

        }

        return result;
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

    /**
     * Get the account of a contract. Check if the contract has associated an account, if not, check the transfer of the
     * trade and try to get it
     *
     * @param marginCallConfig Contract
     * @param trade            Trade
     * @return account
     */
    @SuppressWarnings("unchecked")
    private Account getAccount(final CollateralConfig marginCallConfig, final Trade trade) {
        Account account = null;
        if (marginCallConfig != null) {
            account = marginCallConfig.getAccount();
        }

        if (account == null) {
            try {
                if (trade != null) {
                    final TransferArray transfers = DSConnection.getDefault().getRemoteBO()
                            .getTransfers(null, "trade_id = " + trade.getLongId(), null);

                    final Vector<BOTransfer> vectorTransfer = transfers.toVector();
                    for (final BOTransfer transfer : vectorTransfer) {
                        if (account == null) {
                            account = BOCache.getAccount(DSConnection.getDefault(), transfer.getGLAccountNumber());
                        }
                    }
                }
            } catch (final RemoteException e) {
                final StringBuffer message = new StringBuffer("Error loading the transfer for the trade= "
                        + trade.getLongId() + " in SantCashFlowsReport ");
                Log.error(message, e.getCause());
                Log.error(this, e); //sonar
            }
        }
        return account;
    }

}
