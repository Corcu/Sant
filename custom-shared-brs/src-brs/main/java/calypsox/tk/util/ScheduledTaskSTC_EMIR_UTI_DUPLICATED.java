package calypsox.tk.util;


import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import calypsox.tk.bo.TradeCache;
import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.sql.StringUtils;


public class ScheduledTaskSTC_EMIR_UTI_DUPLICATED extends ScheduledTask {

	
  private static final long serialVersionUID = -1L;

  
  /** The TradeCache. */
  private final TradeCache tradeCache;


  /** The fixed Task information returned by getTaskInformation method */
  private static final String TASKINFORMATION = "Search Trades with UTI/USI values duplicated.";
  
  
  /** WHERE clause for not COMPLETE tasks **/
  private static final StringBuilder WHERE_TASKS_NOT_COMPLETED = new StringBuilder(" AND ").append(" task_status NOT IN ('" + Task.COMPLETED + "') ");
  
  
  /** Code format **/
  private static final String FORMAT_USI_UTI = "There are USI and UTI duplicate values, %s, for trade %d regarding following trades: %s.";
  private static final String FORMAT_USI = "There is USI duplicate value, %s, for trade %d regarding following trades: %s.";
  private static final String FORMAT_UTI = "There is UTI duplicate value, %s, for trade %d regarding following trades: %s.";
  private static final String S_UTI = "UTI";
  private static final String S_USI = "USI";
  private static final String KEYWORD_UTI_TRADE_ID = "UTI_REFERENCE";
  private static final String KEYWORD_USI_TRADE_ID = "USI_REFERENCE";
  
  private static final String EXCEPTION_TYPE = "EX_DUPLICATED_UTI_USI";
  
  /** Task messages */
  private static final String COMMENT_TASK_COMPLETED = "Task completed.";
  
  
  /** Status Accepted - Domain Value with accepted Trade Status **/
  private static final String DV_ACCEPTED_TRADE_STATUS = "acceptedTradeStatusDuplicatedUTIUSI";

  
  /** Array Trade UTI USI Keywords **/
  private static final String[] UTI_USI_TRADE_KEYWORDS = {
	KEYWORD_UTI_TRADE_ID,
	KEYWORD_USI_TRADE_ID
  };

  
  /**
   * Literal BOT_UTI_AMEND
   */
  private static final String BO_UTI_AMEND = "BO_UTI_AMEND";

  
  /**
   * Before Days
   */
  private static final int DAYS_BEFORE = -14;

  
  /**
   * Text Messages
   */
  private static final String EMPTY_SPACE = "";

  
  /**
   * Instantiates a new scheduled task.
   */
  public ScheduledTaskSTC_EMIR_UTI_DUPLICATED() {
    super();
    tradeCache = new TradeCache();
  }

  
  @Override
  public String getTaskInformation() {
    return TASKINFORMATION;
  }

  
  /**
   * Check the mandatory params.
   *
   * @param messagesP
   *            Vector
   * @return boolean
   */
  @Override
  public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messagesP) {
    return true;
  }

  
  @Override
  public boolean process(DSConnection ds, PSConnection ps) {

    Log.debug(this, "Start processing ScheduledTask SANT_UTI_DUPLICATED.");
    final boolean rst = true;
    final JDatetime jValDatetime = getValuationDatetime();

    // Get trades by time range
    final TradeArray tradesToday = getTradesToday(jValDatetime, ds);
    
    if (tradesToday!=null && !tradesToday.isEmpty()) {
      Log.debug(this, "Trades obtained today (" + jValDatetime.toString() + "): " + tradesToday.size());
      // Compare UTI/USI with previous trades
      final HashMap<Long, List<UsiUtiDuplicatedItem>> map = getTradesWithUtiUsiDuplicated(ds, tradesToday, jValDatetime);
      // Generate Tasks to Task Station with EX_DUPLICATED exception
      createTasks(ds, map);
    }
    else {
    	Log.debug(this, "No trades obtained today (" + jValDatetime.toString() + "]");
    }

    return rst;
  }


  /**
   * Get trades with non-empty UTI or USI values depending of the TimeRange.
   *
   * @param timeRange
   * @param jValDatetime
   * @param ds
   * @return
   */
  protected TradeArray getTradesToday(final JDatetime jValDatetime, final DSConnection ds) {
	  TradeArray tradesToday = new TradeArray();
	  JDatetime startJDatetime = getTimeRangeJDatetime(jValDatetime.add(-1, 0, 0, 0, 0), "16:00:00");
	  JDatetime endJDatetime = getTimeRangeJDatetime(jValDatetime, "15:59:59");

	  // Trades ENTERED_DATE between Time_Range
	  final TradeArray tradesEnteredToday = getTradesEnteredToday(ds, startJDatetime, endJDatetime);
	  if (tradesEnteredToday != null && !tradesEnteredToday.isEmpty()) {
		  tradesToday.add(tradesEnteredToday.getTrades(), tradesEnteredToday.size());
	  }

	  // Trades BO_UTI_AMEND between Time_Range
	  final TradeArray tradesBoUtiAmendToday = getTradesBoUtiAmendToday(ds, startJDatetime, endJDatetime);

	  if (tradesBoUtiAmendToday != null && !tradesBoUtiAmendToday.isEmpty()) {
		  tradesToday.add(tradesBoUtiAmendToday.getTrades(), tradesBoUtiAmendToday.size());
	  }

	  return tradesToday;
  }


  /**
   * Get trades by ENTERED_DATE between two dates with non-empty UTI and USI
   * values.
   *
   * @param ds
   * @param startJDatetime
   * @param endJDatetime
   * @return
   */
  protected TradeArray getTradesEnteredToday(final DSConnection ds, final JDatetime startJDatetime, final JDatetime endJDatetime) {
    TradeArray tArray = null;
    final long[] ids = getTradesIdsByEnteredDate(ds, startJDatetime, endJDatetime);

    if (ids == null || ids.length == 0) {
      Log.info(this,"Couldn't get the Trades Entered between two specific dates.");
      return null;
    }
    Log.info(this, "Total Trades Ids between [" + startJDatetime + "] and [" + endJDatetime + "]: " + ids.length);

    try {
      tArray = SantanderUtil.getInstance().getTradesWithTradeFilter(ids);
    } 
    catch (final RemoteException e) {
      Log.error(this, "Could not get the Trades by ENTERED_DATE between two dates.", e);
    }

    return tArray;
  }


  /**
   * Get trades by audit action BO_UTI_AMEND between two dates.
   *
   * @param ds
   * @param startJDatetime
   * @param endJDatetime
   * @return
   */
  protected TradeArray getTradesBoUtiAmendToday(final DSConnection ds, final JDatetime startJDatetime, final JDatetime endJDatetime) {

    TradeArray trades = null;
    TradeArray filteredTradeArray = new TradeArray();

    // Get audit of Trades with audit action BO_UTI_AMEND
    final HashMap<Long, List<AuditValue>> mapAudit = getMapTradeIdAudit(startJDatetime, endJDatetime, BO_UTI_AMEND);

    if (mapAudit != null && !mapAudit.isEmpty()) {
      final List<Long> tradeIdsList = new ArrayList<Long>(mapAudit.keySet());

      try {
        final long[] tradeIds = new long[tradeIdsList.size()];
        for (int i = 0; i < tradeIds.length; i++) {
          tradeIds[i] = tradeIdsList.get(i);
        }
        trades = SantanderUtil.getInstance().getTradesWithTradeFilter(tradeIds);
      }
      catch (final RemoteException e) {
        Log.error(this, "Could not get the Trades by BO_UTI_AMEND", e);
      }

      // Filter trades with non-empty UTI and USI values.
      if (trades != null && trades.getTrades().length > 0) {
        filteredTradeArray = filterTradesUtiUsiNotEmpty(trades);
      }
    }

    return filteredTradeArray;
  }


  /**
   * Get value time range in JDatetime.
   *
   * @param jValDatetime
   * @param time
   * @return
   */
  protected JDatetime getTimeRangeJDatetime(final JDatetime jValDatetime, final String time) {

    final String valDateFormatted = formatDate(jValDatetime.getJDate(TimeZone.getDefault()), "dd/MM/yyyy");
    final String range = valDateFormatted + " " + time;
    final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    sdf.setTimeZone(TimeZone.getDefault());
    Date d = null;
    JDatetime jDatetime = null;
    
    try {
      d = sdf.parse(range);
      jDatetime = new JDatetime(d);
    } catch (final ParseException e) {
      Log.error(this, String.format("Could not parse the date \"%s\"", range), e);
    }

    return jDatetime;
  }


  /**
   * Get trades Ids by ENTERED_DATE between two dates with non-empty UTI and
   * USI values.
   *
   * @param ds
   * @param startJDatetime
   * @param endJDatetime
   * @return
   */
  protected long[] getTradesIdsByEnteredDate(final DSConnection ds, final JDatetime startJDatetime, final JDatetime endJDatetime) {

    Log.debug(this, "Get Trades Ids between two dates by ENTERED_DATE.");
    long[] ids = null;
    final TradeFilter filter = new TradeFilter();

    final String[] acceptedTradeStatus = getStatusAcceptedFromDV(ds);

    if (acceptedTradeStatus == null || acceptedTradeStatus.length == 0) {
      Log.error(this, "Couldn't get the Status Accepted from Domain Values.");
      return null;
    }

    final String tradeStatus = StringUtils.join(acceptedTradeStatus, "', '");

    // It is used to get Trades with non-emtpy UTI and/or USI values.
    final String tradeKeywords = StringUtils.join(UTI_USI_TRADE_KEYWORDS, "', '");

    final StringBuilder whereClause = new StringBuilder();
    whereClause.append(" trade.trade_status in ('").append(tradeStatus);
    whereClause.append("') ");
    whereClause.append(" AND ");
    whereClause.append(" trade.entered_date > ");
    whereClause.append(Util.datetime2SQLString(startJDatetime));
    whereClause.append(" AND ");
    whereClause.append(" trade.entered_date <= ");
    whereClause.append(Util.datetime2SQLString(endJDatetime));
    whereClause.append(" AND ");
    whereClause.append(" trade_keyword.keyword_name in ('").append(tradeKeywords);
    whereClause.append("') ");
    whereClause.append(" AND ");
    whereClause.append(" trade_keyword.trade_id = trade.trade_id ");
    whereClause.append(" AND ");
    whereClause.append(" trade.product_id = product_desc.product_id ");
    whereClause.append(" AND ");
    whereClause.append(" product_desc.product_type = 'PerformanceSwap'");
    
    final String fromClause = " trade, trade_keyword, product_desc ";
   
    filter.setSQLFromClause(fromClause);
    filter.setSQLWhereClause(whereClause.toString());

    try {
      ids = ds.getRemoteTrade().getTradeIds(filter, getValuationDatetime(), true);
    } catch (final RemoteException e) {
      Log.error(this, "Could not get the Trades Ids by ENTERED_DATE between two dates.", e);
    }

    return ids;
  }


  /**
   * Filter trades with UTI and USI values not empty.
   *
   * @param tArray
   * @return
   */
  protected TradeArray filterTradesUtiUsiNotEmpty(final TradeArray tArray) {
    Log.debug(this, "Start filtering trades with non-empty UTI/USI values.");
    final TradeArray filteredTradeArray = new TradeArray();
    for (int i = 0; i < tArray.getTrades().length; i++) {
      final Trade aTrade = tArray.getTrades()[i];
      // Keyword UTI_REFERENCE
      final String utiKw = aTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
      final boolean uti = !Util.isEmpty(utiKw);
      // Keyword USI_REFERENCE
      final String usiKw = aTrade.getKeywordValue(KEYWORD_USI_TRADE_ID);
      final boolean usi = !Util.isEmpty(usiKw);
      if (uti || usi) {
        filteredTradeArray.add(aTrade);
      }
    }
    return filteredTradeArray;
  }


  /**
   * Search for trades with identical UTI or USI value.
   *
   * @param ds
   * @param tradesByRange
   * @param jValDatetime
   * @return
   */
  protected HashMap<Long, List<UsiUtiDuplicatedItem>> getTradesWithUtiUsiDuplicated(final DSConnection ds, final TradeArray tradesToday, final JDatetime jValDatetime) {

    Log.debug(this, "Start searching for trades with identical UTI or USI value.");
    final HashMap<Long, List<UsiUtiDuplicatedItem>> result = new HashMap<Long, List<UsiUtiDuplicatedItem>>();

    // 1. Get tradeIds between start date and end date with non-empty UTI or USI.
    final long[] oldTrades = getTradeIdsBefore(ds, jValDatetime);
    
    // 2. Iterate the tradeIds up to a maximum of TradeCache.BATCH_SIZE.
    if (oldTrades != null && oldTrades.length > 0) {
      Log.debug(this, "Total Trades Ids: " + oldTrades.length);
      int pos = 0;
      long[] tradeIds = new long[TradeCache.BATCH_SIZE];
      
      for (int i = 0; i < oldTrades.length; i++) {
        tradeIds[pos] = oldTrades[i];
        pos++;
        if (pos == TradeCache.BATCH_SIZE) {
          // 3. Get trades and add to TradeCache.
          addTradesToCache(ds, tradeIds);
          // 4. Compare UTI and USI values.
          addTradeIdsUtiUsiDuplicated(tradesToday, result);
          // 5. Clean TradeCache.
          pos = 0;
          tradeIds = new long[TradeCache.BATCH_SIZE];
        }
      }
      
      // Last batch
      if (pos > 0 && pos < TradeCache.BATCH_SIZE) {
        // 3. Get trades and add to TradeCache.
        addTradesToCache(ds, tradeIds);
        // 4. Compare UTI and USI values.
        addTradeIdsUtiUsiDuplicated(tradesToday, result);
        // 5. Clean TradeCache.
        pos = 0;
        tradeIds = new long[TradeCache.BATCH_SIZE];
      }
    }

    return result;
  }


  /**
   * Get Trade Ids 14 days before the time range actual.
   *
   * @param ds
   * @param jValDatetime
   * @param hour
   * @return
   */
  private long[] getTradeIdsBefore(final DSConnection ds, final JDatetime jValDatetime) {
    JDatetime startJDatetime = getTimeRangeJDatetime(jValDatetime.add(DAYS_BEFORE, 0, 0, 0, 0), "00:00:00");
    final JDatetime endJDatetime = getTimeRangeJDatetime(jValDatetime.add(0, 0, 0, 0, 0), "16:00:00"); 
    Log.info(this, "Get Trades Ids between [" + startJDatetime + "] and [" + endJDatetime + "]");
    final long[] tradeIdsBetweenDates = getTradesIdsByEnteredDate(ds, startJDatetime, endJDatetime);
    return tradeIdsBetweenDates;
  }


  /**
   * Add trades to TradeCache.
   *
   * @param ds
   * @param tradeIds
   */
  protected void addTradesToCache(final DSConnection ds, final long[] tradeIds) {
    Log.debug(this, "Get Trades from their Trade Ids and add them to TradeCache.");
    try {
      final TradeArray tArray = SantanderUtil.getInstance().getTradesWithTradeFilter(tradeIds);
      for (final Trade originalTrade : tArray.getTrades()) {
        tradeCache.add(originalTrade);
      }
    }
    catch (final RemoteException e) {
      Log.error(this, "Could not get the Trades.", e);
    }
  } 


  /**
   * Compare UTI and USI values.
   *
   * @param tradesByRange
   * @param map
   */
  protected void addTradeIdsUtiUsiDuplicated(final TradeArray tradesToday, final HashMap<Long, List<UsiUtiDuplicatedItem>> map) {
    Log.info(this, "Search the UTI and/or USI duplicated.");
    final HashMap<Long, List<UsiUtiDuplicatedItem>> aMap = searchUtiUsiDuplicated(tradesToday, tradeCache);
    if (aMap != null && !aMap.isEmpty()) {
      map.putAll(aMap);
      aMap.clear();
    }
  }


  /**
   * Search for trades with the same UTI or USI (Near or Far) values.
   *
   * @param tradesToday
   * @param TradeCache
   * @return
   */
  protected HashMap<Long, List<UsiUtiDuplicatedItem>> searchUtiUsiDuplicated(final TradeArray tradesToday, final TradeCache cache) {

    final HashMap<Long, Trade> mapTradeCache = cache.getTradeCacheByTradeId();
    final HashMap<Long, List<UsiUtiDuplicatedItem>> aMap = new HashMap<Long, List<UsiUtiDuplicatedItem>>();

    if (tradesToday != null && !tradesToday.isEmpty() && mapTradeCache != null && !mapTradeCache.isEmpty()) {
      for (int i = 0; i < tradesToday.getTrades().length; i++) {
        final Trade tradeToday = tradesToday.elementAt(i);
        for (final Map.Entry<Long, Trade> entry : mapTradeCache.entrySet()) {
          final Trade aTradeCache = entry.getValue();
          // Avoid comparing changes BO_UTI_AMEND
          if (tradeToday.getLongId() == aTradeCache.getLongId()) {
            continue;
          }
          final List<UsiUtiDuplicatedItem> compareList = compareUtiUsiValues(tradeToday, aTradeCache);
          if (compareList != null && !compareList.isEmpty()) {
            if (aMap.containsKey(tradeToday.getLongId())) {
              final List<UsiUtiDuplicatedItem> newCompareList = aMap.get(tradeToday.getLongId());
              newCompareList.addAll(compareList);
              aMap.put(tradeToday.getLongId(), newCompareList);
            } else {
              aMap.put(tradeToday.getLongId(), compareList);
            }
          }
        }
      }
    }

    return aMap;
  }


  /**
   * Compare UTITradeId and USITradeId values.
   *
   * @param aTradeByRange
   * @param aTradeCache
   * @return
   */
  protected List<UsiUtiDuplicatedItem> compareUtiUsiValues(final Trade aTradeByRange, final Trade aTradeCache) {

    Log.debug(this, "Start comparing UTI and USI values between trades [" + aTradeByRange.getLongId() + "] and [" + aTradeCache + "].");
    final List<UsiUtiDuplicatedItem> compareList = new ArrayList<UsiUtiDuplicatedItem>();
    final List<String> listByRange = new ArrayList<String>();
    final List<String> listCache = new ArrayList<String>();

    // Compare UTITradeId
    final String utiByRange = aTradeByRange.getKeywordValue(KEYWORD_UTI_TRADE_ID);
    final String utiCache = aTradeCache.getKeywordValue(KEYWORD_UTI_TRADE_ID);
    listByRange.add(Util.isEmpty(utiByRange) ? EMPTY_SPACE : utiByRange);
    listCache.add(Util.isEmpty(utiCache) ? EMPTY_SPACE : utiCache);

    // Compare USITradeId
    final String usiByRange = aTradeByRange.getKeywordValue(KEYWORD_USI_TRADE_ID);
    final String usiCache = aTradeCache.getKeywordValue(KEYWORD_USI_TRADE_ID);
    listByRange.add(Util.isEmpty(usiByRange) ? EMPTY_SPACE : usiByRange);
    listCache.add(Util.isEmpty(usiCache) ? EMPTY_SPACE : usiCache);

    // Create and populate the comparison matrix.
    final String[][] matrix = new String[listByRange.size()][listCache.size()];
    for (int i = 0; i < listByRange.size(); i++) {
      final String aValueRange = listByRange.get(i).trim();
      for (int j = 0; j < listCache.size(); j++) {
        final String aValueCache = listCache.get(j).trim();
        if (aValueRange.equals(aValueCache)) {
          matrix[i][j] = aValueRange;
        }
        else {
          matrix[i][j] = EMPTY_SPACE;
        }
      }
    }

    // Iterate matrix
    for (int i = 0; i < matrix.length; i++) {
      for (int j = 0; j < matrix[i].length; j++) {
        if (!matrix[i][j].isEmpty()) {
          final UsiUtiDuplicatedItem item = new UsiUtiDuplicatedItem();
          item.setKeywordInvestigatedTrade(UTI_USI_TRADE_KEYWORDS[i]);
          item.setKeywordComparedTrade(UTI_USI_TRADE_KEYWORDS[j]);
          item.setTradeIdInvestigatedTrade(String.valueOf(aTradeByRange.getLongId()));
          item.setTradeIdComparedTrade(String.valueOf(aTradeCache.getLongId()));
          item.setKeywordValue(listByRange.get(i));
          compareList.add(item);
        }
      }
    }

    Log.debug(this, "End comparing UTI and USI values between trades [" + aTradeByRange.getLongId() + "] and [" + aTradeCache + "].");
    return compareList;
  }


  /**
   * Gets all trade ids with specific audit_action value.
   *
   * @param tradeIdList
   *            tradeIdList list.
   * @return HashMap<Integer, List<AuditValue>> containing all trade ids with
   *         audit.
   */
  protected HashMap<Long, List<AuditValue>> getMapTradeIdAudit(final JDatetime startJDatetime, final JDatetime endJDatetime, final String auditAction) {

    final HashMap<Long, List<AuditValue>> mapTradeIdAudit = new HashMap<Long, List<AuditValue>>();
    try {
      final StringBuilder where = new StringBuilder();
      where.append("entity_class_name = 'Trade'");
      where.append(" AND ");
      where.append("modif_date > ");
      where.append(Util.datetime2SQLString(startJDatetime));
      where.append(" AND ");
      where.append("modif_date <= ");
      where.append(Util.datetime2SQLString(endJDatetime));
      where.append(" AND ");
      where.append("audit_action = '" + auditAction + "'");

      final Vector<?> rawAudits = DSConnection.getDefault().getRemoteTrade().getAudit(where.toString(), "entity_id ASC, version_num DESC", null);
      for (final Object rawAudit : rawAudits) {
        if (rawAudit instanceof AuditValue) {
          final AuditValue auditValue = (AuditValue) rawAudit;
          final long tradeId = auditValue.getEntityId();
          if (mapTradeIdAudit.get(tradeId) == null) {
            mapTradeIdAudit.put(tradeId, new ArrayList<AuditValue>());
          }
          final List<AuditValue> tradeAudit = mapTradeIdAudit.get(tradeId);
          tradeAudit.add(auditValue);
        }
      }
    } catch (final RemoteException e) {
      Log.error(this, "Cannot retrieve trade audits from database", e);
    }

    return mapTradeIdAudit;
  }


  /**
   * Checks if the trade status is accepted
   *
   * @param trade
   * @return
   */
  protected String[] getStatusAcceptedFromDV(final DSConnection ds) {
    String[] rst = new String[0];
    final Vector<String> domainValues = LocalCache.getDomainValues(ds, DV_ACCEPTED_TRADE_STATUS);
    if (!Util.isEmpty(domainValues)) {
      rst = domainValues.toArray(new String[domainValues.size()]);
    }
    return rst;
  }


  /**
   * createTasks if is necessary
   *
   * @param ds
   * @param mapTradesUsiUtiDupl
   */
  protected void createTasks(final DSConnection ds, final HashMap<Long, List<UsiUtiDuplicatedItem>> mapTradesUsiUtiDupl) {
    if (!mapTradesUsiUtiDupl.isEmpty()) {
      for (final Map.Entry<Long, List<UsiUtiDuplicatedItem>> entry : mapTradesUsiUtiDupl.entrySet()) {
        final Long tradeId = entry.getKey();
        final List<UsiUtiDuplicatedItem> itemList = entry.getValue();
        // Check if exists active Tasks state != COMPLETED
        final TaskArray tasksNotCompleted = getActiveTasksUtiUsiDuplicated(ds, tradeId, WHERE_TASKS_NOT_COMPLETED.toString());
        if (tasksNotCompleted != null && !tasksNotCompleted.isEmpty()) {
          completeAndSaveTask(ds, tasksNotCompleted, COMMENT_TASK_COMPLETED);
        }
        // Create new task exception
        createTaskException(ds, tradeId, itemList);
      }
    }
  }


  /**
   * Create Task Exception.
   *
   * @param trade
   */
  protected void createTaskException(final DSConnection ds, final long tradeId, final List<UsiUtiDuplicatedItem> itemList) {

    Trade trade = null;
    try {
      trade = ds.getRemoteTrade().getTrade(tradeId);
    }
    catch (final RemoteException e) {
      Log.error(this, String.format("Could not get the trade \"%s\"", tradeId), e);
    }
    
    if(trade==null) {
    	Log.error(this, "Trade is null");
    	return;
    }
    
    String message = getTaskComment(trade, itemList);      
    Task taskException = new Task();
    taskException.setStatus(Task.NEW);
    taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
    taskException.setEventType(EXCEPTION_TYPE);
    taskException.setComment(message);
    taskException.setTradeId(trade.getLongId());
    taskException.setBookId(trade.getBookId());
    
    TaskArray task = new TaskArray();
    task.add(taskException);
    try {
		ds.getRemoteBackOffice().saveAndPublishTasks(task,0L,null);
	}
    catch (CalypsoServiceException e) {
        Log.error(this, "Could not save the exception task.");
	}
    
  }


  /**
   * Get the message for the Task Comment.
   *
   * @param tradeId
   * @param itemList
   * @return
   */
  protected String getTaskComment(final Trade trade, final List<UsiUtiDuplicatedItem> itemList) {

    final long tradeId = trade.getLongId();

    // List TradeKeywords
    final Set<String> tradeKwList = new HashSet<String>();

    // List Compared TradeId
    final Set<String> tradeIdComparedList = new HashSet<String>();

    // List TradeKeyword Values
    final Set<String> tradeKwValuesList = new HashSet<String>();

    for (final UsiUtiDuplicatedItem item : itemList) {
      tradeKwList.add(item.getKeywordInvestigatedTrade());
      tradeKwValuesList.add(trade.getKeywordValue(item.getKeywordInvestigatedTrade()));
      tradeIdComparedList.add(item.getTradeIdComparedTrade());
    }

    // Keywords duplicate
    String[] arrayKws = new String[tradeKwList.size()];
    arrayKws = tradeKwList.toArray(arrayKws);

    // Keywords values duplicate
    String[] arrayKwsValues = new String[tradeKwValuesList.size()];
    arrayKwsValues = tradeKwValuesList.toArray(arrayKwsValues);
    final String utiUsiDuplValues = StringUtils.join(arrayKwsValues, " / ");

    // Trade Ids
    final List<String> sortedList = new ArrayList<String>(tradeIdComparedList);
    Collections.sort(sortedList);
    String[] arrayIds = new String[sortedList.size()];
    arrayIds = sortedList.toArray(arrayIds);
    final String ids = StringUtils.join(arrayIds, ", ");

    // Create the task comment depending on the UTI kw or USI kw or both
    boolean bUsi = false;
    boolean bUti = false;
    for (final String sKw : arrayKws) {
      if (!Util.isEmpty(sKw)) {
        if (sKw.contains(S_USI)) {
          bUsi = true;
        } else if (sKw.contains(S_UTI)) {
          bUti = true;
        }
      }
    }

    String taskComment = EMPTY_SPACE;
    
    if (bUsi && bUti) {
      taskComment = String.format(FORMAT_USI_UTI, utiUsiDuplValues,
          tradeId, ids);
    } else if (bUsi && !bUti) {
      taskComment = String.format(FORMAT_USI, utiUsiDuplValues, tradeId,
          ids);
    } else if (!bUsi && bUti) {
      taskComment = String.format(FORMAT_UTI, utiUsiDuplValues, tradeId,
          ids);
    } else {
      Log.info(this, "Error getting information of UTI/USI.");
    }

    return taskComment;
  }


  /**
   * Get string date with a specified format.
   *
   * @param jDate
   * @param format
   * @return
   */
  protected String formatDate(final JDate jDate, final String format) {
    final Date date = jDate.getDate(TimeZone.getDefault());
    final SimpleDateFormat sdf = new SimpleDateFormat(format);
    return sdf.format(date);
  }


  /**
   * Get tasks the trade have just created a Task previously
   *
   * @param trade
   * @return
   */
  protected TaskArray getActiveTasksUtiUsiDuplicated(final DSConnection ds, final long tradeId, final String addClause) {
    TaskArray tasks = new TaskArray();
    final StringBuilder whereClause = new StringBuilder();
    whereClause.append("trade_id = ").append(tradeId);
    whereClause.append(" AND ");
    whereClause.append("event_class = 'Exception'");
    whereClause.append(" AND ");
    whereClause.append("event_type = 'EX_DUPLICATED_UTI_USI'");

    if (addClause != null && !addClause.isEmpty()) {
      whereClause.append(addClause);
    }

    try {
      tasks = ds.getRemoteBO().getTasks(whereClause.toString(), null);
    }
    catch (final RemoteException e) {
      Log.error(this, String.format("Error retrieving tasks from BBDD."), e);
    }

    return tasks;
  }


  /**
   * complete tasks and save it
   *
   * @param task
   *            task to complete
   * @param comment
   *            comment
   * @throws RemoteException
   */
  public void completeAndSaveTask(final DSConnection ds, final TaskArray tasks, final String comment) {

    if (tasks != null) {
      for (int i = 0; i < tasks.size(); i++) {
        final Task task = tasks.get(i);
        task.setOwner(ds.getUser());
        task.setCompletedDatetime(new JDatetime());
        task.setStatus(Task.COMPLETED);
        task.setUserComment(comment);
        saveTask(ds, task);
      }
    }
  }


  /**
   * Save one task
   *
   * @param task
   */
  protected void saveTask(final DSConnection ds, final Task task) {
    try {
      ds.getRemoteBO().save(task);
    } catch (final RemoteException e) {
      Log.error(this, e);
    }
  }


}





class UsiUtiDuplicatedItem {

	
  String keywordInvestigatedTrade;
  String keywordComparedTrade;
  String tradeIdInvestigatedTrade;
  String tradeIdComparedTrade;
  String keywordValue;

  
  public UsiUtiDuplicatedItem() {
    keywordInvestigatedTrade = "";
    keywordComparedTrade = "";
    tradeIdInvestigatedTrade = "";
    tradeIdComparedTrade = "";
    keywordValue = "";
  }

  
  public String getKeywordInvestigatedTrade() {
    return keywordInvestigatedTrade;
  }

  
  public String getKeywordComparedTrade() {
    return keywordComparedTrade;
  }

  
  public String getTradeIdInvestigatedTrade() {
    return tradeIdInvestigatedTrade;
  }

  
  public String getTradeIdComparedTrade() {
    return tradeIdComparedTrade;
  }

  
  public String getKeywordValue() {
    return keywordValue;
  }

  
  public void setKeywordInvestigatedTrade(String keywordInvestigatedTrade) {
    this.keywordInvestigatedTrade = keywordInvestigatedTrade;
  }

  
  public void setKeywordComparedTrade(String keywordComparedTrade) {
    this.keywordComparedTrade = keywordComparedTrade;
  }

  
  public void setTradeIdInvestigatedTrade(String tradeIdInvestigatedTrade) {
    this.tradeIdInvestigatedTrade = tradeIdInvestigatedTrade;
  }

  
  public void setTradeIdComparedTrade(String tradeIdComparedTrade) {
    this.tradeIdComparedTrade = tradeIdComparedTrade;
  }

  
  public void setKeywordValue(String keywordValue) {
    this.keywordValue = keywordValue;
  }


}
