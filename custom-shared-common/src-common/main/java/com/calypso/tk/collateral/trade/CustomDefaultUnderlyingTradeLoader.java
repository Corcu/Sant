package com.calypso.tk.collateral.trade;

import com.calypso.tk.collateral.trade.impl.DefaultTradeLoaderFilter;
import com.calypso.tk.collateral.trade.impl.DefaultUnderlyingTradeLoader;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.CollateralContext;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.util.*;

/**
 * @author aalonsop
 * By default in Calypso STC, derivative trades are linked to the same product as the spot one will be.
 * F.e. BondForward trades are linked to a common Security and marked with the "BondForward=true" keyword,
 * this is the only way to identify them.
 * And because Calypso core's behaviour doesn't allow this type of filtering, this customization was done.
 * This loader goes directly to ddbb to do the kwd's filtering in case of a BondForward (or another derivative) perimeter MCContract
 */
public class CustomDefaultUnderlyingTradeLoader extends DefaultUnderlyingTradeLoader {

    @Override
    public List<Trade> loadTrades(DefaultTradeLoaderFilter filter, List<String> messages, boolean backDated) {
        List<Trade> loadedTrades = super.loadTrades(getNonDerivativesFilter(filter), messages, backDated);
        List<Trade> forwardTrades = loadDerivativeTrades(filter);
        loadedTrades.addAll(forwardTrades);
        return loadedTrades;
    }

    protected DefaultTradeLoaderFilter getNonDerivativesFilter(DefaultTradeLoaderFilter filter) {
        return new NonFwdDefaultTradeLoaderFilter(filter.getEntries());
    }

    /**
     * @param filter
     * @return Derivate trades in perimeter
     */
    private List<Trade> loadDerivativeTrades(DefaultTradeLoaderFilter filter) {
        return Optional.ofNullable(filter).filter(this::isBondFwdPerimeter)
                .map(this::loadBondFwdTrades).orElse(new ArrayList<>());
    }

    protected List<Trade> loadBondFwdTrades(DefaultTradeLoaderFilter filter) {
        List<Trade> tradeList = new ArrayList<>();
        TradeFilter tradeFilter = new TradeFilter();
        tradeFilter.setName("MarginCall");
        tradeFilter.setSQLWhereClause(buildBondFwdWhere(filter));
        try {
            TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTrades(tradeFilter, filter.getValueDatetime());
            tradeList = trades.asList();
        } catch (CalypsoServiceException exc) {
            Log.error(this, exc.getCause());
        }
        return tradeList;
    }

    protected boolean isBondFwdPerimeter(DefaultTradeLoaderFilter filter) {
        return filter.getProductList().contains(Product.BOND);
    }

    protected String buildBondFwdWhere(DefaultTradeLoaderFilter filter) {
        return " ( trade.trade_date_time  <= " +
                buildTradeDateSQL(filter) + ")" +
                " AND ( trade.settlement_date  >= " +
                buildSettleDateSQL(filter) + ")" +
                buildCptySQL(filter) +
                " AND product_desc.product_family  = 'Bond'" +
                " AND trade.trade_id in " +
                "(select trade_id from trade_keyword where ( trade_keyword.keyword_name = 'BondForward' ) AND ( trade_keyword.keyword_value = 'true' ))";

    }

    private String buildCptySQL(DefaultTradeLoaderFilter filter){
        String where="";
        List<Integer> cptyIdList=filter.getCptyIdList();
        if (!Util.isEmpty(cptyIdList)
                &&cptyIdList.size()<=CollateralContext.getMaxItemsInQuery()) {
            where=" AND trade.cpty_id in ("
            +Util.collectionToString(filter.getCptyIdList())
            +") ";
        }
        return where;
    }

    private String buildPOSQL(DefaultTradeLoaderFilter filter){
        String where="";
        Set<Integer> poIdList=filter.getPoIdList();
        if (!Util.isEmpty(poIdList)
                &&poIdList.size()<=CollateralContext.getMaxItemsInQuery()) {
            where=" AND book.legal_entity_id in ("
                    +Util.collectionToString(poIdList)
                    +") ";
        }
        return where;
    }

    private String buildSettleDateSQL(DefaultTradeLoaderFilter filter){
        Calendar calendar= buildCalendar(filter,2,0,0,0);
        return Util.datetime2SQLString(calendar.getTime());
    }

    private String buildTradeDateSQL(DefaultTradeLoaderFilter filter){
        Calendar calendar=  buildCalendar(filter,23,59,59,999);
        return Util.datetime2SQLString(calendar.getTime());
    }

    private Calendar buildCalendar(DefaultTradeLoaderFilter filter,int hours,int minutes,int seconds,int millis){
        Calendar dateCalendar=filter.getValueDatetime().asCalendar(TimeZone.getDefault());
        dateCalendar.set(Calendar.HOUR_OF_DAY,hours);
        dateCalendar.set(Calendar.MINUTE,minutes);
        dateCalendar.set(Calendar.SECOND,seconds);
        dateCalendar.set(Calendar.MILLISECOND,millis);
        return dateCalendar;
    }
}
