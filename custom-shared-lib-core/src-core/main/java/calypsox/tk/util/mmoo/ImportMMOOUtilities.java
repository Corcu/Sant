/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util.mmoo;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MMOO Utilities
 *
 * @author David Porras
 * @version 2.0
 */
// GSM: added change default book
public class ImportMMOOUtilities {

    private static final String CURRENCY = "EUR";
    // private static String MMOO_BOOK = "MMOO";
    protected final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Get trade
     *
     * @param camara
     * @param productSubtype
     * @param tradeId
     * @param cpty
     * @param tradeDate
     * @return
     */
    public static Trade getTrade(final String camara, final String productSubtype, final String tradeId,
                                 final String cpty, final JDate valueDate, final Book colConfigBook) {

        Trade trade = null;

        TradeArray ta = getMatchingTrades(camara, tradeId, productSubtype);
        if ((ta != null) && (ta.size() > 0)) {
            // use existing trade
            trade = ta.firstElement();
            trade.setAction(Action.AMEND);
            trade.addKeyword(TradeInterfaceUtils.TRANS_TRADE_KWD_MTM_DATE, valueDate);
        } else {
            // create new trade
            trade = createTrade(camara, productSubtype, tradeId, cpty, valueDate, colConfigBook);
        }

        return trade;

    }

    /**
     * @param boSystem    Back office system of the received trade
     * @param boReference Back office reference of the received trade
     * @return the trade(s) with the given bo_system and bo_reference
     */
    public static TradeArray getMatchingTrades(String foSystem, String extReference, String productSubtype) {
        TradeArray existingTrades = null;
        try {
            existingTrades = DSConnection
                    .getDefault()
                    .getRemoteTrade()
                    .getTrades(
                            "trade, trade_keyword, product_desc",
                            "trade.trade_id=trade_keyword.trade_id and "
                                    + "product_desc.product_id = trade.product_id and product_desc.product_sub_type="
                                    + Util.string2SQLString(productSubtype)
                                    + " and trade.trade_status<>'CANCELED' and "
                                    + "trade_keyword.keyword_name='FO_SYSTEM' and trade_keyword.keyword_value="
                                    + Util.string2SQLString(foSystem) + " and trade.external_reference="
                                    + Util.string2SQLString(extReference), null, null);

        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }

        return existingTrades;

    }

    /**
     * Create trade
     *
     * @param camara
     * @param productSubtype
     * @param tradeId
     * @param cpty
     * @param tradeDate
     * @return
     */
    public static Trade createTrade(final String camara, final String productSubtype, final String tradeId,
                                    final String cpty, final JDate valueDate, final Book colConfigBook) {

        Trade trade = new Trade();

        // create trade's product
        CollateralExposure product = new CollateralExposure();
        product.setSubType(productSubtype);
        product.setUnderlyingType(productSubtype);
        product.setStartDate(valueDate);

        // GSM: Added to avoid saving at 11.59. If not, these trades won't appear on the collateral Manager

        try {
            trade.setSettleDate(null);
            trade.setTradeDate(null);

            // calculate JDTime for the trade
            JDatetime jdtTradeDate = valueDate.getJDatetime(TimeZone.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(jdtTradeDate.getTime());
            cal.set(Calendar.AM_PM, Calendar.AM);
            cal.set(Calendar.HOUR, 2);

            JDatetime tradeJDTime = new JDatetime(cal.getTime());
            trade.setTradeDate(tradeJDTime);
            trade.setSettleDate(jdtTradeDate.getJDate(TimeZone.getDefault()));

            product.setEnteredDatetime(new JDatetime());
            product.setStartDate(jdtTradeDate.getJDate(TimeZone.getDefault()));

        } catch (Exception e) {
            Log.error(ImportMMOOUtilities.class, e);
        }

        product.setMaturityDate(JDate.valueOf("31/12/2390"));
        product.setDirection("Buy", trade);
        product.setCurrency(CURRENCY);
        product.setPrincipal(1000000);
        trade.setProduct(product);

        // set trade properties
        trade.setAction(Action.NEW);
        trade.setStatus(Status.S_NONE);
        trade.setExternalReference(tradeId);
        trade.setTraderName(DSConnection.getDefault().getUser());
        // GSM: 14/10/14. Incidence, use MCContract book
        trade.setBook(colConfigBook);
        // trade.setBook(BOCache.getBook(DSConnection.getDefault(), MMOO_BOOK));
        trade.setCounterParty(BOCache.getLegalEntity(DSConnection.getDefault(), cpty));
        // cuidadito con esto!!! ->
        // trade.setTradeDate(valueDate.getJDatetime(TimeZone.getDefault()));
        // trade.setSettleDate(valueDate);
        trade.setTradeCurrency(CURRENCY);
        trade.setSettleCurrency(CURRENCY);
        trade.setEnteredUser(DSConnection.getDefault().getUser());
        trade.setSalesPerson("NONE");

        // set trade keywords
        trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_FO_SYSTEM, camara);
        trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_BO_SYSTEM, camara);
        trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE, tradeId);
        trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_NUM_FRONT_ID, tradeId);
        trade.addKeyword(TradeInterfaceUtils.TRANS_TRADE_KWD_MTM_DATE, valueDate.toString());

        return trade;

    }

    /**
     * Create cash margin call trade
     *
     * @param amount
     * @param cpty
     * @param valueDate
     * @return
     */
    public static Trade createCashMarginCallTrade(final Double amount, final CollateralConfig mcc, final JDate valueDate) {

        // create mc product
        MarginCall mc = new MarginCall();
        mc.setCurrencyCash(mcc.getCurrency());
        mc.setPrincipal(amount);
        mc.setLinkedLongId(mcc.getId());
        mc.setFlowType("COLLATERAL");

        // create mc trade
        Trade mcTrade = new Trade();
        mcTrade.setProduct(mc);
        mcTrade.setQuantity(amount);
        mcTrade.setTraderName(DSConnection.getDefault().getUser());
        mcTrade.setCounterParty(BOCache.getLegalEntity(DSConnection.getDefault(), mcc.getLeId()));
        mcTrade.setTradeCurrency(mcc.getCurrency());
        mcTrade.setSettleCurrency(mcc.getCurrency());
        mcTrade.setAction(Action.NEW);
        mcTrade.setStatus(Status.S_NONE);
        // GSM: 14/10/14. Incidence, use MCContract book
        // Book book = BOCache.getBook(DSConnection.getDefault(), MMOO_BOOK);
        // mcTrade.setBook(book);
        mcTrade.setBook(mcc.getBook());

        // set trade & product dates
        try {
            mcTrade.setSettleDate(null);
            mcTrade.setTradeDate(null);

            // calculate JDTime for the trade
            JDatetime jdtTradeDate = valueDate.getJDatetime(TimeZone.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(jdtTradeDate.getTime());
            cal.set(Calendar.AM_PM, Calendar.AM);
            cal.set(Calendar.HOUR, 2);

            JDatetime tradeJDTime = new JDatetime(cal.getTime());
            mcTrade.setTradeDate(tradeJDTime);
            mcTrade.setSettleDate(jdtTradeDate.getJDate(TimeZone.getDefault()));
            mc.setEnteredDatetime(new JDatetime());

        } catch (Exception e) {
            Log.error(ImportMMOOUtilities.class, e);
        }

        return mcTrade;

    }

    /**
     * Create cash margin call trade
     *
     * @param amount of the movement for today
     * @param mcc    CollateralConfig
     * @return
     */
    public static Trade createCashMarginCallTrade(final Double amount, final CollateralConfig mcc) {

        // get contract
        if (mcc == null) {
            return null;
        }

        // create mc product
        MarginCall mc = new MarginCall();
        mc.setCurrencyCash(mcc.getCurrency());
        mc.setPrincipal(amount);
        mc.setLinkedLongId(mcc.getId());
        mc.setFlowType("COLLATERAL");

        // create mc trade
        Trade mcTrade = new Trade();
        mcTrade.setProduct(mc);
        mcTrade.setQuantity(amount);
        mcTrade.setTraderName(DSConnection.getDefault().getUser());
        mcTrade.setCounterParty(BOCache.getLegalEntity(DSConnection.getDefault(), mcc.getLeId()));
        mcTrade.setTradeCurrency(mcc.getCurrency());
        mcTrade.setSettleCurrency(mcc.getCurrency());
        mcTrade.setAction(Action.NEW);
        mcTrade.setStatus(Status.S_NONE);
        // GSM: 14/10/14. Incidence, use MCContract book
        // Book book = BOCache.getBook(DSConnection.getDefault(), MMOO_BOOK);
        // mcTrade.setBook(book);
        mcTrade.setBook(mcc.getBook());

        // set trade & product dates
        try {
            mcTrade.setSettleDate(null);
            mcTrade.setTradeDate(null);

            JDate valueDate = JDate.getNow();
            // calculate JDTime for the trade
            JDatetime jdtTradeDate = valueDate.getJDatetime(TimeZone.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(jdtTradeDate.getTime());
            cal.set(Calendar.AM_PM, Calendar.AM);
            cal.set(Calendar.HOUR, 2);

            JDatetime tradeJDTime = new JDatetime(cal.getTime());
            mcTrade.setTradeDate(tradeJDTime);
            mcTrade.setSettleDate(jdtTradeDate.getJDate(TimeZone.getDefault()));
            mc.setEnteredDatetime(new JDatetime());

        } catch (Exception e) {
            Log.error(ImportMMOOUtilities.class, e);
        }

        return mcTrade;

    }

    /**
     * Create security margin call trade
     *
     * @param amount
     * @param cpty
     * @param valueDate
     * @return
     */
    public static Trade createSecMarginCallTrade(final Product product, final Double amount,
                                                 final CollateralConfig mcc, final JDate valueDate) {

        // create mc product
        MarginCall mc = new MarginCall();
        mc.setSecurity(product);
        mc.setLinkedLongId(mcc.getId());
        mc.setFlowType("COLLATERAL");

        // create mc trade
        Trade mcTrade = new Trade();
        mcTrade.setProduct(mc);
        if (product instanceof Bond) {
            mcTrade.setQuantity(amount / ((Bond) product).getFaceValue(valueDate));
        } else if (product instanceof Equity) {
            mcTrade.setQuantity(amount);
        }
        mcTrade.setTraderName(DSConnection.getDefault().getUser());
        mcTrade.setCounterParty(BOCache.getLegalEntity(DSConnection.getDefault(), mcc.getLeId()));
        mcTrade.setTradeCurrency(mcc.getCurrency());
        mcTrade.setSettleCurrency(mcc.getCurrency());
        mcTrade.setAction(Action.NEW);
        mcTrade.setStatus(Status.S_NONE);
        // GSM: 14/10/14. Incidence, use MCContract book
        // Book book = BOCache.getBook(DSConnection.getDefault(), MMOO_BOOK);
        // mcTrade.setBook(book);
        mcTrade.setBook(mcc.getBook());

        // set trade and product dates
        try {
            mcTrade.setSettleDate(null);
            mcTrade.setTradeDate(null);

            // calculate JDTime for the trade
            JDatetime jdtTradeDate = valueDate.getJDatetime(TimeZone.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(jdtTradeDate.getTime());
            cal.set(Calendar.AM_PM, Calendar.AM);
            cal.set(Calendar.HOUR, 2);

            JDatetime tradeJDTime = new JDatetime(cal.getTime());
            mcTrade.setTradeDate(tradeJDTime);
            mcTrade.setSettleDate(jdtTradeDate.getJDate(TimeZone.getDefault()));
            mc.setEnteredDatetime(new JDatetime());

        } catch (Exception e) {
            Log.error(ImportMMOOUtilities.class, e);
        }

        return mcTrade;

    }

    /**
     * @param contract
     * @param product
     * @return the settled FO position for a specific Bond
     * @throws Exception if any error occurred during DS call
     */
    public static double fecthTodayBondMarginCallPosition(final JDate valueDate, final CollateralConfig contract,
                                                          final Product product, final List<String> errors) {

        final StringBuilder where = new StringBuilder();
        where.append(" inv_secposition.internal_external = 'MARGIN_CALL' ");
        where.append(" AND date_type = 'SETTLE' ");
        where.append(" AND inv_secposition.position_type = 'ACTUAL'");
        where.append(" AND inv_secposition.config_id = ").append(contract.getId());
        where.append(" AND inv_secposition.security_id = ").append(product.getId());
        // GSM: 14/10/14. Incidence, use MCContract book
        // Book b = BOCache.getBook(DSConnection.getDefault(), MMOO_BOOK);
        Book b = contract.getBook();

        where.append(" AND inv_secposition.book_id = ").append(b.getId());

        // FROM inv_secposition
        // WHERE position_date <= {d '2014-08-29'}
        // AND position_type = 'ACTUAL'
        // AND date_type = 'SETTLE' AND
        // internal_external = 'MARGIN_CALL'
        // AND config_id = 20976
        // AND security_id IN (9586)

        // attach past dates positions
        where.append(" AND inv_secposition.position_date = ");
        where.append(" (");// BEGIN SELECT
        where.append(" select MAX(temp.position_date) from inv_secposition temp ");
        where.append(" WHERE inv_secposition.internal_external = temp.internal_external ");
        where.append(" AND inv_secposition.date_type = temp.date_type ");
        where.append(" AND inv_secposition.position_type = temp.position_type ");
        where.append(" AND inv_secposition.config_id = temp.config_id "); // added
        where.append(" AND inv_secposition.security_id = temp.security_id ");
        where.append(" AND inv_secposition.book_id = temp.book_id ");
        where.append(" AND TRUNC(temp.position_date) <= ").append(Util.date2SQLString(valueDate));
        where.append(" )");// AND SELECT

        InventorySecurityPositionArray secPositions;
        try {
            secPositions = DSConnection.getDefault().getRemoteBO().getInventorySecurityPositions("", where.toString(), null);
        } catch (RemoteException e) {
            errors.add("DS error: not Possible to recover Bond position for contract " + contract.getId()
                    + " and Bond " + product.getId());
            Log.error(ImportMMOOUtilities.class, e);
            return 0.0;
        }

        double total = 0;
        for (int i = 0; i < secPositions.size(); i++) {
            total += secPositions.get(i).getTotal();
        }

        if (product instanceof Bond) {
            total *= ((Bond) product).getFaceValue(valueDate);
        }

        return total;
    }

    /**
     * @param contract
     * @return the settled FO Margin Call Cash position
     * @throws Exception if any error occurred during DS call
     */
    public static double fecthTodayCashMarginCallPosition(final JDate valueDate, final CollateralConfig contract,
                                                          final List<String> errors) {

        final StringBuilder where = new StringBuilder();
        where.append(" inv_cashposition.internal_external = 'MARGIN_CALL' ");
        where.append(" AND date_type = 'SETTLE' ");
        where.append(" AND inv_cashposition.position_type = 'ACTUAL'");
        where.append(" AND inv_cashposition.config_id = ").append(contract.getId());
        // GSM: 09/10/14. Fix, should use default contract book
        final Book b = contract.getBook(); // BOCache.getBook(DSConnection.getDefault(), MMOO_BOOK);
        where.append(" AND inv_cashposition.book_id = ").append(b.getId());

        // FROM inv_cashposition
        // WHERE position_date <= {d '2014-08-29'}
        // AND position_type = 'ACTUAL'
        // AND date_type = 'SETTLE'
        // AND internal_external = 'MARGIN_CALL'
        // AND config_id = 20976

        // attach past dates positions
        where.append(" AND inv_cashposition.position_date = ");
        where.append(" (");// BEGIN SELECT
        where.append(" select MAX(temp.position_date) from inv_cashposition temp ");
        where.append(" WHERE inv_cashposition.internal_external = temp.internal_external ");
        where.append(" AND inv_cashposition.date_type = temp.date_type ");
        where.append(" AND inv_cashposition.position_type = temp.position_type ");
        where.append(" AND inv_cashposition.config_id = temp.config_id ");
        where.append(" AND inv_cashposition.book_id = temp.book_id ");
        where.append(" AND TRUNC(temp.position_date) <= ").append(Util.date2SQLString(valueDate));
        where.append(" )");// AND SELECT

        InventoryCashPositionArray cashPositions;
        try {
            cashPositions = DSConnection.getDefault().getRemoteBO().getInventoryCashPositions("", where.toString(), null);
        } catch (RemoteException e) {
            errors.add("DS error: not Possible to recover cash position for contract " + contract.getId());
            Log.error(ImportMMOOUtilities.class, e);
            return 0.0;
        }

        double total = 0;
        for (int i = 0; i < cashPositions.size(); i++) {
            total += cashPositions.get(i).getTotal();
        }
        return total;
    }

    /**
     * Get MMOO MEFF contract (expected only one)
     *
     * @return
     */
    public static CollateralConfig getContract(final String legalEntity, final ArrayList<String> logDetails) {

        CollateralConfig contract = null;

        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
        mcFilter.setLegalEntity(legalEntity);
        List<String> types = new ArrayList<String>();
        types.add("MMOO");
        mcFilter.setContractTypes(types);

        List<CollateralConfig> collateralConfigs = null;
        try {
            collateralConfigs = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
        } catch (RemoteException e) {
            Log.error(ImportMMOOUtilities.class, e); //sonar
        }
        if (!Util.isEmpty(collateralConfigs)) {
            contract = collateralConfigs.get(0);
        } else {
            logDetails.add("Cannot get MEFF contract.\n");
        }

        return contract;

    }

    /**
     * Save trade returning id
     *
     * @param trade
     * @param line
     * @param logDetails
     * @return
     */
    public static long saveTrade(final Trade trade, int line, final List<String> logDetails) {

        long id = 0;
        try {
            id = DSConnection.getDefault().getRemoteTrade().save(trade);
        } catch (RemoteException e) {
            Log.error(ImportMMOOUtilities.class, e); //sonar
        }

        if (id > 0) {
            if (line == -1) {
                logDetails.add("Total cash trade saved with id=" + id + ".\n");
            } else {
                logDetails.add("Line " + line + ": Sec. trade saved with id=" + id + ".\n");
            }

        } else {
            if (line == -1) {
                logDetails.add("Error saving total cash trade.\n");
            } else {
                logDetails.add("Line " + line + ": Error saving sec. trade.\n");
            }

        }

        return id;

    }

    /**
     * Replaces the default MMOO Trades book. In case the book doesn't exit, it will fail
     *
     * @param bookName
     * @return book does exist
     */
    // public static boolean putMMOOBook(final String bookName, final List<String> logDetails) {
    //
    // Book b = BOCache.getBook(DSConnection.getDefault(), bookName.trim());
    // if (b != null) {
    // MMOO_BOOK = bookName.trim();
    // return true;
    // }
    // logDetails.add("Book " + bookName + " not found on the system.");
    // return false;
    // }

    /**
     * Update and save MEFF trade and its PLMark values
     *
     * @param trade
     * @param tradeMtm
     * @param valueDate
     * @param errors
     */
    public static long updateTradeAndPLMark(final Trade trade, final Double tradeMtm, final JDate valueDate,
                                            final String pricingEnvName, final ArrayList<String> errors) {

        Trade savedTrade = null;

        try {

            RemoteSantCollateralService remoteSantColService = (RemoteSantCollateralService) DSConnection.getDefault()
                    .getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);
            if (remoteSantColService == null) {
                errors.add("Error getting Remote Collateral Service.\n");
                return -3;
            }

            PricingEnv pricingEnv = null;
            try {
                pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(pricingEnvName);
            } catch (RemoteException e) {
                errors.add("Error getting Pricing Enviroment.\n");
                Log.error(ImportMMOOUtilities.class, e); //sonar
                return -2;
            }

            // save trade
            // savedTrade = remoteSantColService.saveTrade(trade, errors);
            final long id = DSConnection.getDefault().getRemoteTrade().save(trade);
            savedTrade = DSConnection.getDefault().getRemoteTrade().getTrade(id);
            Trade savedTradeClone = (Trade) savedTrade.clone();

            // get trade PLMark
            PLMark plMark = getTradePLMark(savedTradeClone, valueDate, pricingEnv, errors);
            if (plMark == null) {
                return id;
            }

            // calculate PLMark values
            calculateTradePLMarkValues(savedTradeClone, plMark, tradeMtm, valueDate, pricingEnv, errors);
            if (errors.size() > 0) {
                return id;
            }

            // save PLMark & trade again
            savedTradeClone.setAction(Action.AMEND);
            remoteSantColService.saveTradeWithPLMarks(savedTradeClone, plMark, errors);
            return id;

        } catch (RemoteException e) {
            errors.add(e.getMessage());
            Log.error(ImportMMOOUtilities.class, e); //sonar
        }
        return -1;
    }

    /**
     * Fills the information of the log
     *
     * @param tradeId1
     * @param tradeId2
     * @param errors
     * @param processDate
     */
    public static void scheduledTaskLog(long tradeId1, long tradeId2, final List<String> errors,
                                        final String logPath, final String logName, final String extRef, final JDate processDate) {

        LogUtilMMOO log = new LogUtilMMOO(logPath, true, extRef, logName);
        Vector<String> inf = new Vector<>();

        if (tradeId1 != 0) {
            log.IncrementNumberOfTradeProcessed();
            if (tradeId1 > 0) {
                inf.add("Trade Mtm id: " + tradeId1 + " saved.");
                log.IncrementNumberOfTradeOK();
            } else {
                log.IncrementNumberOfTradeERROR();
            }
        }

        if (tradeId2 != 0) {
            log.IncrementNumberOfTradeProcessed();
            if (tradeId2 > 0) {
                inf.add("Trade Mtm id: " + tradeId2 + " saved.");
                log.IncrementNumberOfTradeOK();

            } else {
                log.IncrementNumberOfTradeERROR();
            }
        }

        if (errors.isEmpty()) {
            inf.add("PlMarks for " + processDate.toString() + " saved.");

        } else {
            for (String s : errors) {
                inf.add(s);
            }
        }

        log.attachLogDetails(inf);
        log.WriteLog();
    }

    /**
     * Fills the information of the log -> used only for MEFF depo
     *
     * @param tradeId1
     * @param tradeId2
     * @param logDetails
     * @param processDate
     */
    public static void scheduledTaskLog_Depo(List<Long> tradeIds, List<String> logDetails, String logPath,
                                             String logName, String extRef, JDate processDate) {

        LogUtilMMOO log = new LogUtilMMOO(logPath, true, extRef, logName);
        Vector<String> inf = new Vector<>();

        for (long tradeId : tradeIds) {
            log.IncrementNumberOfTradeProcessed();
            if (tradeId > 0) {
                log.IncrementNumberOfTradeOK();
            } else if (tradeId == 0) {
                log.IncrementNumberOfTradeOK();
                logDetails.add("Position is already the same. \n");
            } else {
                log.IncrementNumberOfTradeERROR();
            }
        }

        for (String s : logDetails) {
            inf.add(s);
        }

        log.attachLogDetails(inf);
        log.WriteLog();
    }

    /**
     * Get PLMark for trade
     *
     * @param trade
     * @param valueDate
     * @param errors
     * @return
     */
    public static PLMark getTradePLMark(Trade trade, JDate valueDate, PricingEnv pricingEnv, ArrayList<String> errors) {

        PLMark plMark = null;

        try {
            plMark = CollateralUtilities.createPLMarkIfNotExists(trade, DSConnection.getDefault(),
                    pricingEnv.getName(), valueDate);
            plMark.setTradeLongId(trade.getLongId());
            plMark.setBookId(trade.getBookId());
            plMark.setPricingEnvName(pricingEnv.getName());
            plMark.setValDate(valueDate);

        } catch (RemoteException e) {
            errors.add("Error retieving plMark for trade = " + trade.getExternalReference() + ", valueDate = "
                    + valueDate.toString() + ".\n");
            Log.error(ImportMMOOUtilities.class, e); //sonar
        }

        return plMark;

    }

    /**
     * Calculate PLMark values for trade
     *
     * @param trade
     * @param plMark
     * @param tradeMtm
     * @param valueDate
     * @param errors
     */
    public static void calculateTradePLMarkValues(Trade trade, PLMark plMark, Double tradeMtm, JDate valueDate,
                                                  PricingEnv pricingEnv, ArrayList<String> errors) {

        // update plmark NPV value
        calculateNPVvalue(plMark, tradeMtm);

        // update plmark NPV_BASE and MARGIN_CALL values
        CollateralUtilities.calculateMCAndNpvBase(DSConnection.getDefault(), trade, plMark, pricingEnv, valueDate, 0.0,
                errors);
    }

    /**
     * Calculate NPV value
     *
     * @param plMark
     * @param tradeMtm
     */
    public static void calculateNPVvalue(PLMark plMark, Double tradeMtm) {

        PLMarkValue plMarkNewValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, CURRENCY, tradeMtm,
                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

        if (plMarkNewValue != null) {
            plMark.addPLMarkValue(plMarkNewValue);
        }

    }

}
