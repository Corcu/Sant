package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class SantCashAllocationReport extends SantReport {
    private static final long serialVersionUID = 1L;
    private static final int MAX_TRADES_PER_STEP = 1000;
    private StringBuffer whereTemplate = null;
    private Vector<String> fromTemplate = null;
    public final static String CCY_CASH = "CCY_CASH";

    @Override
    protected ReportOutput loadReport(Vector<String> errors) {
        // Check if the process date is empty. Cannot be empty
        computeProcessStartDate(errors);
        computeProcessEndDate(errors);

        if (errors.isEmpty()) {
            DefaultReportOutput reportOutput = getReportOutput();
            return reportOutput;
        }

        return null;
    }

    private void computeProcessStartDate(Vector<String> errors) {
        JDate processStartDate = null;
        processStartDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        if (processStartDate == null) {
            errors.add("Process Start Date cannot be empty.");
        }
    }

    private void computeProcessEndDate(Vector<String> errors) {
        JDate processEndDate = null;
        processEndDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()), TradeReportTemplate.END_DATE,
                TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);

        if (processEndDate == null) {
            errors.add("Process End Date cannot be empty.");
        }

    }

    private DefaultReportOutput getReportOutput() {

        // 27/07/15. SBNA Multi-PO filter
        Set<String> posIdsAllowed = new HashSet<String>(Util.string2Vector(CollateralUtilities.filterPoIdsByTemplate(super
                .getReportTemplate())));

        DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        try {
            final ReportTemplate template = getReportTemplate();

            long[] tradeIds = getTradesId(template);

            Collection<SantCashAllocationItem> itemPerStep = new ArrayList<SantCashAllocationItem>();

            if (tradeIds != null) {
                final int SQL_IN_ITEM_COUNT = SantCashAllocationReport.MAX_TRADES_PER_STEP;
                int start = 0;

                List<Long> tradeIdsList = Util.longArrayAsList(tradeIds);

                for (int i = 0; i <= (tradeIdsList.size() / SQL_IN_ITEM_COUNT); i++) {
                    int end = (i + 1) * SQL_IN_ITEM_COUNT;
                    if (end > tradeIdsList.size()) {
                        end = tradeIdsList.size();
                    }
                    final List<Long> subList = tradeIdsList.subList(start, end);
                    start = end;

                    itemPerStep.addAll(getDataForRows(subList, posIdsAllowed));

                }

            }
            // tradeBrowserItems = filterAliveTrades(tradeBrowserItems);
            Collection<ReportRow> rows = new ArrayList<ReportRow>();
            itemPerStep = filterTradesByCriteria(itemPerStep, template);

            for (SantCashAllocationItem item : itemPerStep) {
                ReportRow row = new ReportRow(item);
                rows.add(row);
            }

            reportOutput.setRows(rows.toArray(new ReportRow[rows.size()]));

        } catch (final Exception e) {
            reportOutput = null;
            Log.error("Could not load trades from database", e.getCause() + "\n" + e); //sonar
        }

        return reportOutput;
    }

    private Collection<SantCashAllocationItem> getDataForRows(Collection<Long> tradesToGet, Set<String> posIdsAllowed) {

        Set<SantCashAllocationItem> items = new HashSet<>();

        try {
            for (Long tradeId : tradesToGet) {
                SantCashAllocationItem item = new SantCashAllocationItem();

                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);

                // GSM: 28/06/2013 - deprecated new core.
                final CollateralConfig mcc = getContractFromTrade(trade);

                // 28/07/15. SBNA Multi-PO filter
                if (mcc == null) {
                    continue;

                }
                if (CollateralUtilities.filterOwners(posIdsAllowed, mcc)) {
                    continue;
                }

                if (mcc != null) {
                    PLMark mark = getPLMark(trade);
                    Amount mtm = new Amount();
                    if (mark != null) {
                        // MIGRATION V14.4 18/01/2015
                        PLMarkValue markValue = mark.getPLMarkValueFromList("NPV");
                        if (markValue != null) {
                            mtm = new Amount(markValue.getMarkValue());
                        }
                    }

                    item.setTrade(trade);
                    item.setCollateralConfig(mcc);
                    item.setMtm(mtm);
                    item.setPricingEnv(this._pricingEnv);

                    items.add(item);
                }
            }
        } catch (RemoteException e) {
            Log.error("Coudn't load a trade", e.getCause() + "\n" + e); //sonar
        }

        return items;
    }

    /**
     * returns the contract for the trade. First tries to use the internal ref. If not found, tries to use the keyword.
     *
     * @param trade
     * @return the contract if found
     */
    // GSM: 28/06/2013 - deprecated new core. We need to retrieve the CollateralConfig
    private CollateralConfig getContractFromTrade(Trade trade) {

        CollateralConfig ret = null;

        if (trade == null) {
            return null;
        }

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
        int contractId = -1;

        // try to get the contract id, either from internal reference or keyword MC_CONTRACT_NUMBER
        try {

            contractId = ((MarginCall) trade.getProduct()).getMarginCallId();

            if (contractId < 0) {
                contractId = Integer.parseInt(trade.getInternalReference().trim());
            }

            if (contractId < 0) {
                contractId = Integer.parseInt(trade.getKeywordValue("MC_CONTRACT_NUMBER"));
            }

        } catch (Exception e) {
            Log.error(this, e); //sonar
            try {
                contractId = Integer.parseInt(trade.getKeywordValue("MC_CONTRACT_NUMBER"));
            } catch (Exception e2) {
                Log.error(this, e2); //sonar
                return null;
            }
        }

        // retrieve the contract
        try {

            ret = srvReg.getCollateralDataServer().getMarginCallConfig(contractId);

        } catch (RemoteException e) {
            // DB error, should not happen
            Log.error(SantCashAllocationReport.class, e.getLocalizedMessage());
            Log.error(this, e); //sonar
            return null;
        }

        return ret;
    }

    @SuppressWarnings("deprecation")
    private PLMark getPLMark(Trade trade) {
        PLMark mark = null;
        try {
            StringBuilder whereClause = new StringBuilder();
            whereClause.append("trade_id =" + trade.getLongId());

            Collection<PLMark> marks = DSConnection.getDefault().getRemoteMarketData()
                    .getPLMarks(whereClause.toString(), Collections.EMPTY_LIST);

            for (PLMark currentMark : marks) {
                if (currentMark.getValDate().equals(trade.getSettleDate())) {
                    mark = currentMark;
                }
            }
        } catch (RemoteException e) {
            Log.error("Coudn't get the PLMark", e.getCause() + "\n" + e); //sonar
        }

        return mark;
    }

    private long[] getTradesId(ReportTemplate template) {
        long[] tradeIds = null;
        this.fromTemplate = new Vector<>();
        try {
            this.whereTemplate = buildWhereTemplateTrades(template);

            StringBuffer fromStr = new StringBuffer();
            for (String from : this.fromTemplate) {
                fromStr.append(from).append(",");
            }

            if (fromStr.length() > 0) {
                fromStr = fromStr.deleteCharAt(fromStr.length() - 1);
            }

            tradeIds = DSConnection.getDefault().getRemoteTrade()
                    .getTradeIds(fromStr.toString(), this.whereTemplate.toString(), 0, 0, null, null);

        } catch (RemoteException e) {
            Log.error("Cannot get the Trades Ids", e.getCause() + "\n" + e); //sonar
        }

        return tradeIds;
    }

    /**
     * Construct the where SQL Clause apply filters that are available in the trade table of the DataBase
     *
     * @param template
     * @return
     */
    private StringBuffer buildWhereTemplateTrades(final ReportTemplate template) {
        final StringBuffer whereTemplate = new StringBuffer("");

        // Product Type
        whereTemplate.append(" trade.product_id=product_desc.product_id");
        whereTemplate.append(" AND ");
        whereTemplate.append(" product_desc.product_type = 'MarginCall'");
        whereTemplate.append(" AND ");
        whereTemplate.append(" product_desc.product_sub_type = 'COLLATERAL'");

        // Process Date
        // process start date
        final JDate processStartDate = getDate(template, getValDate(), TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
        if (processStartDate != null) {
            whereTemplate.append(" AND trade.settlement_date >=  to_date('");
            whereTemplate.append(processStartDate);
            whereTemplate.append(" 00:00:00','dd/MM/yyyy HH24:MI:SS')");
        }

        // process end date
        final JDate processEndDate = getDate(template, getValDate(), TradeReportTemplate.END_DATE,
                TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);
        if (processEndDate != null) {
            whereTemplate.append(" AND trade.settlement_date <= to_date('");
            whereTemplate.append(processEndDate);
            whereTemplate.append(" 23:59:59','dd/MM/yyyy HH24:MI:SS')");
        }

        // PO Deals
        final String poDealsStr = (String) template.get(SantGenericTradeReportTemplate.OWNER_DEALS);
        Vector<String> poDealsIds = null;
        if ((poDealsStr != null) && !Util.isEmpty(poDealsStr)) {
            this.fromTemplate.add("book");
            poDealsIds = Util.string2Vector(poDealsStr);
            whereTemplate.append(" AND trade.book_id = book.book_id AND book.legal_entity_id in (");
            whereTemplate.append(Util.collectionToSQLString(poDealsIds));
            whereTemplate.append(")");
        }

        // Counterparty
        final String s = (String) template.get(SantGenericTradeReportTemplate.COUNTERPARTY);
        if (!Util.isEmpty(s)) {
            buildCptyQuery(whereTemplate, s);
        }

        // Status
        final String statusStr = (String) template.get(SantGenericTradeReportTemplate.TRADE_STATUS);
        Vector<String> statusIds = null;
        if ((statusStr != null) && !Util.isEmpty(statusStr)) {
            statusIds = Util.string2Vector(statusStr);
            whereTemplate.append(" AND trade.trade_status in ");
            whereTemplate.append(Util.collectionToSQLString(statusIds)).append("");
        }

        // Currency Cash
        final String ccyStr = (String) template.get(CCY_CASH);
        if ((ccyStr != null) && !Util.isEmpty(ccyStr)) {
            whereTemplate.append(" AND trade.trade_currency = '");
            whereTemplate.append(ccyStr).append("'");
        }

        // 27/07/15. SBNA Multi-PO filter
        // ORA-00918: columna definida de forma ambigua
        // String owners = CollateralUtilities.filterPoByTemplate(template);
        // if (!Util.isEmpty(owners)) {
        //
        // HashSet<String> allowedPOs = new HashSet<String>(Util.string2Vector(owners));
        // if (!Util.isEmpty(allowedPOs)) {
        // this.fromTemplate.add("legal_entity");
        // this.fromTemplate.add("MRGCALL_CONFIG");
        // this.fromTemplate.add("trade_keyword");
        // whereTemplate.append(" AND trade_keyword.trade_id = trade.trade_id AND trade_keyword.keyword_name =");
        // whereTemplate.append(Util.string2SQLString(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
        // whereTemplate.append(" AND to_number(trade_keyword.keyword_value) = mrgcall_config.mrg_call_def AND");
        // whereTemplate.append(" mrgcall_config.process_org_id=legal_entity.legal_entity_id AND");
        // whereTemplate.append(" legal_entity.legal_entity_id IN ");
        // whereTemplate.append(Util.collectionToSQLString(allowedPOs));
        // whereTemplate.append(" ");
        // }
        //
        // }

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

    private Collection<SantCashAllocationItem> filterTradesByCriteria(Collection<SantCashAllocationItem> items,
                                                                      ReportTemplate template) {
        Collection<SantCashAllocationItem> tradesFiltered = new ArrayList<SantCashAllocationItem>();

        for (SantCashAllocationItem item : items) {
            CollateralConfig marginCall = item.getCollateralConfig();
            boolean candidate = true;

            // Agreements Name && Agreement type && base ccy
            final String agreementIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
            final String agrType = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
            final String base_ccy = (String) template.get(SantGenericTradeReportTemplate.BASE_CURRENCY);

            // filter just by agr name
            if (!Util.isEmpty(agreementIds) && candidate) {
                Vector<String> agreementIdsVec = Util.string2Vector(agreementIds);

                if (!agreementIdsVec.contains(String.valueOf(marginCall.getId()))) {
                    candidate = false;
                }
            }
            // filter just by agr type
            if (!Util.isEmpty(agrType) && candidate) {
                if (!agrType.equals(marginCall.getContractType())) {
                    candidate = false;
                }
            }
            // filter just by base_ccy
            if (!Util.isEmpty(base_ccy) && candidate) {
                if (!base_ccy.equals(marginCall.getCurrency())) {
                    candidate = false;
                }
            }

            // filtering migrated trades
            if (candidate && !"TRUE".equals(item.getTrade().getKeywordValue("IS_MIGRATION"))) {
                tradesFiltered.add(item);
            }
        }

        return tradesFiltered;
    }

}