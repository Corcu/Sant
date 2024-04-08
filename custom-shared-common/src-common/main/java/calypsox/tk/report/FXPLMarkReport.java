package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.PLMarkReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.util.TradeArray;

import java.util.*;

public class FXPLMarkReport extends PLMarkReport {

    @Override
    public void setValuationDatetime(JDatetime datetime) {
        this._valuationDateTime = datetime;
    }

    @Override
    public ReportOutput load(Vector errorMsgs) {
        boolean inclTrade = true;
        boolean inclPosition = true;
        String bookName = null;
        JDate fromDate = null;
        JDate toDate = null;
        Collection plMarks = null;
        Collection indPLMarks = null;
        String whereClause = "";
        List<CalypsoBindVariable> bindVariables = new ArrayList();
        //this.initDates();
        DefaultReportOutput output = new DefaultReportOutput(this);
        if (this._reportTemplate != null) {
            inclTrade = (Boolean) this._reportTemplate.get("IncTrade");
            inclPosition = (Boolean) this._reportTemplate.get("IncPosition");
            bookName = (String) this._reportTemplate.get("Book");
            fromDate = _valuationDateTime.getJDate(TimeZone.getDefault());

            toDate = fromDate;
            if (inclTrade && !inclPosition) {
                whereClause = "position_or_trade like '%Trade' ";
            }

            if (!inclTrade && inclPosition) {
                whereClause = "position_or_trade not like '%Trade' ";
            }

            if (!inclTrade && !inclPosition) {
                whereClause = "position_or_trade not like '%Trade' AND position_or_trade not like '%Position' ";
            }

            if (!Util.isEmpty(bookName) && !bookName.equals("ALL")) {
                Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
                if (!Util.isEmpty(whereClause)) {
                    whereClause = whereClause + " AND ";
                }

                whereClause = whereClause + "(book_id = ?  OR book_id = 0)";
                bindVariables.add(new CalypsoBindVariable(4, book.getId()));
            }

            if (fromDate != null) {
                if (!Util.isEmpty(whereClause)) {
                    whereClause = whereClause + " AND ";
                }

                whereClause = whereClause + " valuation_date >= ? ";
                bindVariables.add(new CalypsoBindVariable(3001, fromDate));
            }

            if (toDate != null) {
                if (!Util.isEmpty(whereClause)) {
                    whereClause = whereClause + " AND ";
                }

                whereClause = whereClause + " valuation_date <= ? ";
                bindVariables.add(new CalypsoBindVariable(3001, toDate));
            }

            String externalReference = (String) this._reportTemplate.get("External Reference");
            if (!Util.isEmpty(externalReference)) {
                whereClause = whereClause + " AND trade_id in (select trade_id from trade where external_reference = ? )";
                bindVariables.add(new CalypsoBindVariable(12, externalReference));
            }

            Boolean adjustmentsOnly = this._reportTemplate.getBoolean("Adjustments Only");
            String envName;
            if (adjustmentsOnly) {
                envName = (String) this._reportTemplate.get("Trade Type");
                String likeClause = null;
                if ("Missing Trade Adjustments".equals(envName)) {
                    likeClause = ioSQL.buildLike("position_or_trade", "dummy", false, true);
                } else if ("Trade/Position Adjustment".equals(envName)) {
                    likeClause = "lower(position_or_trade) Not Like '%dummy%'";
                }

                if (likeClause != null) {
                    whereClause = whereClause + " AND " + likeClause;
                }
            }

            if (this._reportTemplate.get("Trade Id") != null) {
                Long tradeId = (Long) this._reportTemplate.get("Trade Id");
                if (tradeId >= 0L) {
                    whereClause = whereClause + " AND trade_id = ? ";
                    bindVariables.add(new CalypsoBindVariable(3000, tradeId));
                }
            }

            envName = (String) this._reportTemplate.get("Pricing Environment");
            if (!Util.isEmpty(envName)) {
                whereClause = whereClause + " AND pricing_env_name = ? ";
                bindVariables.add(new CalypsoBindVariable(12, envName));
            }
            // added products
            String validStatus = getPrettyStatus((String) getReportTemplate().getAttributes().getAttributes().get("Status"));
            whereClause = whereClause + "AND TRADE_ID IN (SELECT TRADE_ID FROM TRADE WHERE TRADE_STATUS IN ("+validStatus+ ") AND PRODUCT_ID IN (SELECT PRODUCT_ID FROM PRODUCT_DESC WHERE product_family IN ('Bond')))";
            try {
                RemoteMarketData api = this.getDSConnection().getRemoteMarketData();
                plMarks = api.getPLMarks(whereClause, bindVariables);
                if (plMarks != null && plMarks.size() > 0) {
                    filterRows(output, plMarks);
                }
            } catch (Exception var19) {
                Log.error(this, var19);
            }
        }



        return output;
    }

    private String getPrettyStatus(String status){
        StringBuilder builder = new StringBuilder();
        String[] split = status.split(",");
        for(int i=0; i< split.length; i++){
            builder.append("'");
            builder.append(split[i]);
            builder.append("'");
            builder.append(",");
        }
        return builder.toString().substring(0, builder.toString().length()-1);
    }

    private ReportOutput filterRows(DefaultReportOutput output, Collection<PLMark> plMarks) {

        List<ReportRow> finalRows = new ArrayList<>();
        if (plMarks == null || plMarks.size() == 0) {
            return output;
        }
        Vector<String> validMarks = LocalCache.getDomainValues(DSConnection.getDefault(), "PLMarkReport");
        for (PLMark plMark : plMarks) {
            for (String mark : validMarks) {
                if (plMark.getPLMarkValueByName(mark) != null) {
                    ReportRow row = new ReportRow(plMark);
                    Trade trade = getTradeFromPLMark(row);
                    row.setProperty("Trade", trade);
                    row.setProperty("NPV", plMark.getPLMarkValueByName(mark).getMarkValue());
                    row.setProperty("CCY", plMark.getPLMarkValueByName(mark).getCurrency());
                    finalRows.add(row);
                    addChildrenTrades(finalRows, trade, row);
                }
            }
        }

        ReportRow[] reportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
        output.setRows(reportRows);
        return output;
    }

    private Trade getTradeFromPLMark(ReportRow row) {
        PLMark mark = (PLMark) row.getProperty("PLMark");
        if (mark.getTradeLongId() > 0) {
            try {
                return DSConnection.getDefault().getRemoteTrade().getTrade(mark.getTradeLongId());
            } catch (CalypsoServiceException e) {
                org.jfree.util.Log.error(this, e);
            }
        }
        return null;
    }

    private void addChildrenTrades(List<ReportRow> finalRows, Trade parentTrade, ReportRow row) {
        double parentQuantity, daughterQuantity, daughterNPV;
        Vector v = parentTrade.getRoleAllocations();
        parentQuantity = parentTrade.getAllocatedQuantity();
        double motherNPV = (double) row.getProperty("NPV");
        if (v == null || v.isEmpty()) {
            return;
        }
        for (int i = 0; i < v.size(); i++) {

            TradeRoleAllocation tr = (TradeRoleAllocation) v.get(i);
            Trade relatedTrade = tr.getRelatedTrade();
            if (isInCorrectStatus(relatedTrade)) {
                daughterQuantity = tr.getAmount();
                daughterNPV = daughterQuantity / parentQuantity * motherNPV;
                ReportRow childrenRow = new ReportRow(relatedTrade);
                childrenRow.setProperty("NPV", daughterNPV);
                childrenRow.setProperty("CCY", row.getProperty("CCY"));
                childrenRow.setProperty("Trade", relatedTrade);
                finalRows.add(childrenRow);
            }
        }
    }

    private boolean isInCorrectStatus(Trade trade){
        String status = (String) getReportTemplate().getAttributes().getAttributes().get("Status");
        String [] status_split = status.split(",");
        for(int i=0; i<status_split.length; i++){
            if(trade.getStatus().toString().equals(status_split[i])){
                return true;
            }
        }
        return false;
    }
}
