/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.core.EmirReport;
import calypsox.tk.core.GenericReg_EmirReport;
import calypsox.tk.core.TradeUtil;
import calypsox.tk.report.emir.field.EmirFieldBuilderUtil;
import calypsox.tk.report.exception.SantException;
import calypsox.tk.util.FutureTaskThreadPool;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirUtil;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.FXSwap;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.report.TransferReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.Callable;

//CAL_EMIR_026
public class SantEmirValuationReport extends TradeReport implements CheckRowsNumberReport {

  /**
   * num of threads
   */
  private static final int NUM_THREADS = 15;

  /**
   * valuation items
   */
  private List<EmirReport> reportItems = null;

  /**
   * rate_reset status
   */
  private static final String PARTENON_S = "PARTENON";

  /**
   * pending_reset status
   */
  private static final String PENDING_RESET_S = "PENDING_RESET";

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;

  /**
   * EMIR_VALUATION_TYPE field
   */
  private static final String EMIR_VALUATION_TYPE = "VAL";

  /**
   * EMIR_BOTH_TYPE field
   */
  private static final String EMIR_BOTH_TYPE = "BOTH";

  private static final String FAR_LEG = "Far";
  private static final String NEAR_LEG = "Near";

  /**
   * report template
   */
  private ReportTemplate reportTemplate = null;

  private JDate valDate = null;

  /**
   * Used to JUnix setValDate.
   *
   * @param valDate
   *            JDate
   */
  public void setValDate(JDate valDate) {
    this.valDate = valDate;
  }

  /**
   * instance
   */
  private static final SantEmirValuationReport INSTANCE = new SantEmirValuationReport();
  private static SantEmirValuationReport instance = INSTANCE;

  /**
   * Singleton access to GenericReg_SantEmirValuationRTS9Report.
   *
   * @return singleton instance of GenericReg_SantEmirValuationRTS9Report.
   */
  public static SantEmirValuationReport getInstance() {
    return instance;
  }

  /**
   * Just for testing purposes. It should receive a mocked instance of
   * GenericReg_SantEmirValuationRTS9Report.
   *
   * @param mockedUtil
   *            mocked instance of GenericReg_SantEmirValuationRTS9Report.
   */
  public static void setInstance(
      final SantEmirValuationReport mockedUtil) {
    if (mockedUtil == null) {
      instance = INSTANCE;
    } else {
      instance = mockedUtil;
    }
  }

  @SuppressWarnings({ "rawtypes" })
  @Override
  public ReportOutput load(final Vector errors) {
    final DefaultReportOutput rst = new StandardReportOutput(this);
    final ArrayList<ReportRow> rows = new ArrayList<ReportRow>();

    // we need to get MTM on D-1 because is when MIS currently send us the
    // info
    if (_reportTemplate.getHolidays() != null) {
      valDate = JDate.valueOf(getValuationDatetime()).addBusinessDays(-1,
              EmirFieldBuilderUtil.getInstance().castVector(String.class, _reportTemplate.getHolidays()));
    } else {
      valDate = JDate.valueOf(getValuationDatetime()).addBusinessDays(-1, null);
    }

    _valuationDateTime = valDate
        .getJDatetime(TimeZone.getDefault());

    reportTemplate = getReportTemplate();
    final DSConnection ds = getDSConnection();

    final String poReportTemplate = getReportTemplate()
        .get(TransferReportTemplate.PO_NAME).toString();


    final String fixedCode = EmirUtil.getSourceSystem(poReportTemplate);

    // get trades
    final TradeArray trades = getRelevantTrades(ds);

    // CAL_EMIR_041: get delta trades
    final TradeArray deltaTrades = getDeltaTrades(ds);

    // RTS_EMIR_CALYPSO.10: Fix far leg not reported for cancelled trades
    final Map<String, Trade> relevantTradesMap = getTradeMap(trades);
    final Map<String, Trade> deltaTradesMap = getTradeMap(deltaTrades);

    for (final Entry<String, Trade> entry : deltaTradesMap.entrySet()) {
      if (!relevantTradesMap.containsKey(entry.getKey())) {
        trades.add(entry.getValue());
      }
    }

    relevantTradesMap.clear();
    deltaTradesMap.clear();

    // get the valuation info
    final List<EmirReport> items = getReportItems(trades, ds);

    for (int i = 0; i < items.size(); ++i) {
      final EmirReport currentItem = items.get(i);
      final GenericReg_EmirReport currentItemGenericReg = new GenericReg_EmirReport(
          currentItem);
      currentItemGenericReg.setFixedCode(fixedCode);

      final ReportRow currentRow = new ReportRow(currentItem);
      currentRow.setProperty(
          SantEmirValuationReportItem.SANT_EMIR_VALUATION_ITEM,
          currentItemGenericReg);
      rows.add(currentRow);

    }

    rst.setRows(rows.toArray(new ReportRow[rows.size()]));

    //Generate a task exception if the number of rows is out of an umbral defined
    HashMap<String , String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
    checkAndGenerateTaskReport(rst, value);

    return rst;
  }

  /**
   * get the trades relevant for this report
   *
   * @param ds
   *            data server connection
   *
   * @return trade array
   */
  @SuppressWarnings("unchecked")
  public TradeArray getRelevantTrades(final DSConnection ds) {
    final TradeArray trades = new TradeArray();
    try {
      TradeFilter tradeFilterSt = ds.getRemoteReferenceData()
          .getTradeFilter(_tradeFilterSetName);

      if (tradeFilterSt == null) {
        tradeFilterSt = createTradeFilter(ds);
      }

      if (tradeFilterSt == null) {
        tradeFilterSt = new TradeFilter();
      }

      if (reportTemplate == null) {
        reportTemplate = getReportTemplate();
      }

      // adding PO filter
      final String POAuthName = reportTemplate
          .get(TransferReportTemplate.PO_NAME).toString();

      final Vector<String> poNames = new Vector<String>();
      poNames.add(POAuthName);
      tradeFilterSt.setCriterion("bookattr.LegalEntity", poNames, true);

      TradeArray tradesSelected = ds.getRemoteTrade()
          .getTrades(tradeFilterSt, null);

      // Filter Trades per Swap Leg Maturity
      Vector<Trade> filteredTrades = new Vector<>();
      if (!Util.isEmpty(tradesSelected)) {
        final Iterator<Trade> tradeIt = tradesSelected.iterator();
        while (tradeIt.hasNext()) {
          final Trade trade = tradeIt.next();
          if (trade.getProduct() instanceof PerformanceSwap) {
            SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
            if (!pLeg.getMaturityDate().before(valDate)) {
              filteredTrades.add(trade);
            }
          }
        }
      }

      if (!Util.isEmpty(filteredTrades)) {
        trades.addAll(filteredTrades);
      }

    } catch (final RemoteException e) {
      Log.error(this,
          "Coudnt get trades for TradeFilter: "
              + _tradeFilterSetName + " - " + e.getMessage(),
              e);
    }

    return trades;
  }

  /**
   * get the delta trades with the kws terminationDate or cancelationDate in
   * D-1
   *
   * @param ds
   *            data server connection
   *
   * @return trade array
   */
  @SuppressWarnings("unchecked")
  public TradeArray getDeltaTrades(final DSConnection ds) {
    // CAL_EMIR_053
    if (reportTemplate == null) {
      reportTemplate = getReportTemplate();
    }
    // adding PO filter
    final String POAuthName = reportTemplate
        .get(TransferReportTemplate.PO_NAME).toString();
    final TradeArray trades = new TradeArray();
    final JDate cancelTerminationDate = JDate.valueOf(valDate)
        .addBusinessDays(+1, _reportTemplate.getHolidays());
    try {

      final String from = "trade, trade_keyword";
      // retrieve only trades required
      String where = " trade.trade_id = trade_keyword.trade_id and"
          + " keyword_name in ('CancellationDate', 'AccTerminationDate')"
          + " and keyword_value = '"
          + cancelTerminationDate.toString() + "'"
          + " and trunc(trade.entered_date) < to_date('"
          + cancelTerminationDate.toString() + "','dd/mm/yyyy')";

      // DDR MedioDePago
      where = where.concat(addKeywordMedioDePagoCriteria());
      // DDR MedioDePago - End

      final TradeArray tradesSelected = DSConnection.getDefault()
          .getRemoteTrade().getTrades(from, where, null, null);

      // Swaps -> 1 trade Near, 1 trade Far
      final Iterator<Trade> tradeIt = tradesSelected.iterator();

      while (tradeIt.hasNext()) {
        final Trade trade = tradeIt.next();

        //TODO Product Selection
        if (POAuthName.equals(
            trade.getBook().getLegalEntity().getAuthName())) {
          /*
          if (Product.FXSWAP
              .equalsIgnoreCase(trade.getProductType())) {
            // DROP4 - Exclude Fx Swap Near Leg expired -
            // HD0000006838366
            addTradeLegsNotExpired(trade, trades);
            // DROP4 - End - HD0000006838366
            // CAL_371_
          } else if (Product.FXFORWARD
              .equalsIgnoreCase(trade.getProductType())
              || Product.FXNDF
              .equalsIgnoreCase(trade.getProductType())) {
            trades.add(trade);
          }

           */
        }

      }
    } catch (final RemoteException e) {
      Log.error(this, "Coudnt get delta trades : " + e.getMessage(), e);
    }

    return trades;
  }

  /**
   * create trade filter in case the report is launched from Report launcher Window
   *
   * @param ds DS Connection
   * @return trade filter
   * @throws CalypsoServiceException
   */
  private TradeFilter createTradeFilter(final DSConnection ds) throws CalypsoServiceException {

    TradeFilter tradeFilter = ds.getRemoteReferenceData().getTradeFilter("SANT_EMIR_VALUATION");
    return tradeFilter;
  }

  /**
   * Get the report item list for the report given a tradearray
   *
   * @param trades
   *            trade array
   * @param ds
   *            Ds connection
   * @return SantEmirValuationReportItem list
   */
  @SuppressWarnings("unchecked")
  public List<EmirReport> getReportItems(final TradeArray trades,
                                         final DSConnection ds) {
    reportItems = new ArrayList<EmirReport>();

    final int size = trades.size();
    if (size > 0) {
      // 15 threads
      final int blockSize = (size / NUM_THREADS) + 1;

      final FutureTaskThreadPool<SantException> threadPool = new FutureTaskThreadPool<SantException>(
          NUM_THREADS);

      Log.debug(this, "Num of Messages to be processed: " + size + " in "
          + NUM_THREADS + " threads");

      final Iterator<Trade> tradeIt = trades.iterator();

      List<Trade> tradesBlock = new ArrayList<Trade>();

      int i = 0;
      while (tradeIt.hasNext()) {
        final Trade currentTrade = tradeIt.next();

        if (!isInternal(currentTrade)) {
          tradesBlock.add(currentTrade);
        }

        // if end or block max size reached -> Thread starts working
        if (((i == (size - 1))
            || ((i != 0) && ((i % blockSize) == 0)))) {
          threadPool.addTask(
              new SantEmirValuationItemGetter(tradesBlock, ds));

          tradesBlock = new ArrayList<Trade>();
        }
        ++i;
      }

      Log.debug(this, "Waiting threads");

      // no more threads
      threadPool.shutdown();
      // wait until the process of all threads ends
      threadPool.waitForAllTasksToBeCompleted();

      Log.debug(this, "All processing done!");
    }

    return reportItems;
  }

  /**
   * create a Valuation Item for a given trade
   *
   * @param trade
   *            trade
   * @param ds
   *            Ds connection
   * @return snapshot item
   */
  private List<EmirReport> createValuationItem(final Trade trade,
                                               final DSConnection ds) {
    final List<EmirReport> items = new ArrayList<EmirReport>();

    final SantEmirValuationReportLogic emirLogic = new SantEmirValuationReportLogic(trade, _valuationDateTime);

    final SantEmirValuationReportItem valulationItem = emirLogic.fillItem();

    items.addAll(createDbItem(valulationItem, trade));

    return items;
  }

  /**
   * convert item to EmirReport item. Row per tagName
   *
   * @param valuationItem
   *            valuation Item
   * @param trade
   *            trade
   * @return emirReport item
   */
  private List<EmirReport> createDbItem(
      final SantEmirValuationReportItem valuationItem,
      final Trade trade) {
    final List<EmirReport> itemsFromDB = new ArrayList<EmirReport>();

    Set<String> columns = null;

    columns = valuationItem.getColumnNames();

    final long tradeId = trade.getLongId();
    //final String partenonId = getPartenonId(trade, tradeId, null);
    final String murexTradeId = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRADE_ID);

    final String po = trade.getBook().getLegalEntity().getAuthName();

    // final String action = "TEST";
    final String action = "NEW";

    // final String trd = "TEST";
    final String trd = "TRD";

    for (final String currentColumn : columns) {
      String reportType = null;
      String columnValue = null;

      reportType = valuationItem.getReportTypeValue(currentColumn);
      columnValue = String
          .valueOf(valuationItem.getColumnValue(currentColumn));

      // empty fields not saved
      if (!Util.isEmpty(columnValue) && (reportType
          .equalsIgnoreCase(EMIR_BOTH_TYPE)
          || reportType.equalsIgnoreCase(EMIR_VALUATION_TYPE))) {
        final EmirReport newItem = new EmirReport();

        newItem.setTradeVersion(trade.getVersion());
        newItem.setTradeId(tradeId);
        newItem.setReportDate(_valuationDateTime
            .getJDate(TimeZone.getDefault()));

        newItem.setTag(currentColumn);
        newItem.setValue(columnValue);
        newItem.setMurexTradeId(murexTradeId);
        newItem.setPartenonId(murexTradeId);
        newItem.setAction(action);
        newItem.setTranstype(trd);

        newItem.setPo(po);

        // VAL type. If we put BOTH, SNAPSHOT report will show them and
        // it shouldn't
        newItem.setReportType(EMIR_VALUATION_TYPE);

        itemsFromDB.add(newItem);
      }

    }

    return itemsFromDB;
  }

  /**
   * Check if trade is internal or not checking the legal entity attribute LEI
   * for both parties.
   *
   * @param trade
   *            Trade
   * @return true or false if trade is internal or not
   */
  private boolean isInternal(final Trade trade) {

    final DSConnection dsCon = DSConnection.getDefault();

    final LegalEntity cp = trade.getCounterParty();
    final Collection<LegalEntityAttribute> cpAtts = BOCache
        .getLegalEntityAttributes(dsCon, cp.getId());

    final LegalEntity po = trade.getBook().getLegalEntity();
    final Collection<LegalEntityAttribute> poAtts = BOCache
        .getLegalEntityAttributes(dsCon, po.getId());

    final boolean isInternalDeal = TradeUtil.getInstance().isInternal(trade,
        poAtts, cpAtts);

    return isInternalDeal;
  }

  /*
  /**
   * Add the trade to the list of arrays to show.
   *
   * @param trade
   * @param trades
   */
  /*
  private void addTrade(final Trade trade, final TradeArray trades) {
    // Swap
    if (Product.FXSWAP.equalsIgnoreCase(trade.getProductType())) {
      addTradeLegsNotExpired(trade, trades);
    } else { // No-Swap
      trades.add(trade);
    }
  }
*/
  /**
   * Add the trade to the list of arrays if the trade is not expired.
   *
   * @param trade
   * @param trades
   */
  private void addTradeLegsNotExpired(final Trade trade,
      final TradeArray trades) {

    /*
    final FXSwap fxSwap = (FXSwap) trade.getProduct();
    Vector<Trade> tradesSwap = new Vector<Trade>();
    tradesSwap = fxSwap.explodeTrade(trade);
    final JDate settleDate = trade.getSettleDate();

    if (settleDate != null) {

      if (valDate.after(settleDate)) {
        // Add only the far leg
        for (final Trade tradeLeg : tradesSwap) {
          if (SantDTCCGTRUtil.isFarLeg(tradeLeg)) {
            trades.add(tradeLeg);
          }
        }
      } else {
        // Add both legs
        for (final Trade tradeLeg : tradesSwap) {
          trades.add(tradeLeg);
        }
      }
    }
        */
  }

  // DROP4 - End - HD0000006838366

  public class SantEmirValuationItemGetter
  implements Callable<SantException> {
    private List<Trade> trades = null;
    private DSConnection ds = null;

    public SantEmirValuationItemGetter(final List<Trade> trades,
        final DSConnection dsCon) {
      super();

      this.trades = trades;
      ds = dsCon;

    }

    @Override
    public SantException call() throws Exception {
      for (final Trade currentTrade : trades) {

        final List<EmirReport> items = new ArrayList<EmirReport>();

        // if UTI not stored in DB, need to check Uti Kw to see if we've
        // stored the kw using BulkUtiLoad ST
        final List<String> tags = getTagNames(items);
        if (!tags.contains(SantEmirValuationColumns.UTI
            .toString())) {
          items.addAll(createUtiItem(currentTrade));
        }

        // adding the info related just to Valuation report. dont need
        // to save it in the DB because we always get the picture at the
        // end of the day, not events response
        final List<EmirReport> valuationItems = createValuationItem(
            currentTrade, ds);
        addOrReplaceFields(items, valuationItems);

        synchronized (reportItems) {
          reportItems
          .addAll(items);
        }
      }

      return null;
    }

    /**
     * get the value for UTIPREFIX & UTIVALUE tags
     *
     * @param trade
     *            trade to process
     * @return items
     */
    private List<EmirReport> createUtiItem(final Trade trade) {
      final List<EmirReport> items = new ArrayList<EmirReport>();

      final SantEmirValuationReportLogic emirLogic = new SantEmirValuationReportLogic(
          trade, getValuationDatetime());
      final SantEmirValuationReportItem valuationItem = new SantEmirValuationReportItem();

      valuationItem.setColumnValue(
          SantEmirValuationColumns.UTIPREFIX
          .toString(),
          emirLogic.getLogicUtiPrefix());
      valuationItem.setReportTypeValue(
          SantEmirValuationColumns.UTIPREFIX
          .toString(),
          EMIR_BOTH_TYPE);

      valuationItem.setColumnValue(
          SantEmirValuationColumns.UTI.toString(),
          emirLogic.getLogicUtiValue());
      valuationItem.setReportTypeValue(
          SantEmirValuationColumns.UTI.toString(),
          EMIR_BOTH_TYPE);

      items.addAll(createDbItem(valuationItem, trade));

      return items;
    }

    /**
     * get the tag names of the list of items
     *
     * @param items
     *            list of items
     * @return list of tag names
     */
    private List<String> getTagNames(final List<EmirReport> items) {
      final List<String> tagNames = new ArrayList<String>();

      if (!Util.isEmpty(items)) {
        for (final EmirReport currentItem : items) {
          tagNames.add(currentItem.getTag());
        }
      }

      return tagNames;
    }

  }

  /**
   * Add or Replace items from valuation to the perhaps DB items
   *
   * @param items
   *            list of DB items
   * @param valuationItems
   *            list if valutation items
   */
  private void addOrReplaceFields(List<EmirReport> items,
      List<EmirReport> valuationItems) {

    // final List<EmirReport> itemsToAdd = new ArrayList<EmirReport>(items);
    for (final EmirReport valItem : valuationItems) {
      boolean exist = false;
      for (final EmirReport item : items) {
        if (item.getTag().equalsIgnoreCase(valItem.getTag())) {
          if (!item.getValue().equalsIgnoreCase(valItem.getValue())) {
            item.setValue(valItem.getValue());
          }
          exist = true;
          break;
        }
      }
      if (!exist) {
        items.add(valItem);
      }
    }
  }

  // RTS_EMIR_CALYPSO.10: Fix far leg not reported for cancelled trades
  private Map<String, Trade> getTradeMap(TradeArray trades) {
    final Map<String, Trade> tradeMap = new HashMap<String, Trade>();

    for (int iTrade = 0; iTrade < trades.size(); iTrade++) {
      final Trade trade = trades.get(iTrade);
      final String key = getTradeKey(trade);
      tradeMap.put(key, trade);
    }

    return tradeMap;
  }

  // RTS_EMIR_CALYPSO.10: Fix far leg not reported for cancelled trades
  private String getTradeKey(Trade trade) {
    String key = "" + trade.getLongId();

    final String legType = trade.getKeywordValue(FXSwap.LEG_TYPE);
    if (!Util.isEmpty(legType)) {
      key = key + legType;
    }

    return key;
  }

  // DDR MedioDePago
  /**
   * Add Keyword 'MedioDePago' criteria.
   *
   * @return
   */
  private String addKeywordMedioDePagoCriteria() {
    /*
    return " AND trade.trade_id NOT IN (SELECT kw0.trade_id FROM trade_keyword kw0 WHERE kw0.keyword_name = '"
        + KeywordConstantsUtil.TRADE_KEYWORD_MEDIODEPAGO + "' AND kw0.keyword_value = 'S')";
    */
    //TODO
    return "";
  }

  // DDR MedioDePago - End

}
