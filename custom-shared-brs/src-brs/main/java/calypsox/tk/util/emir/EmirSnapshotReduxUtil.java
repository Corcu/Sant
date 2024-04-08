package calypsox.tk.util.emir;

import calypsox.tk.core.EmirReport;
import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.core.TradeUtil;
import calypsox.tk.report.SantEmirSnapshotReportItem;
import calypsox.tk.report.emir.field.EmirFieldBuilderUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.FXSwap;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class EmirSnapshotReduxUtil {

  private static final String[] REPORTABLE_PRODUCTS = { Product.PERFORMANCESWAP };
  private static final String[] NOT_REPORTABLE_STATUSES = { Status.MATURED };
  private static final String[] NOT_REPORTABLE_POS = { "1AVB" };

  public static final Status[] ACCEPTED_TRADE_STATUSES = { Status.S_PENDING, Status.S_VERIFIED,
          Status.valueOf("PARTENON"), Status.S_TERMINATED, Status.S_CANCELED };

  /**
   * Tells if the trade is a non reportable trade that is a reissue or
   * novation of a reportable trade.
   */
  private static final String TRADE_KEYWORD_REISSUE_REPORTABLE = "EMIR_IsReissueOfReportableTrade";

  private static EmirSnapshotReduxUtil instance = null;

  private Map<Integer, Collection<LegalEntityAttribute>> leAttributesMap = null;

  private EmirSnapshotReduxUtil() {
    leAttributesMap = new TreeMap<Integer, Collection<LegalEntityAttribute>>();
  }

  public static EmirSnapshotReduxUtil getInstance() {
    if (instance == null) {
      instance = new EmirSnapshotReduxUtil();
    }

    return instance;
  }

  public static void setInstance(
          EmirSnapshotReduxUtil mockedInstance) {
    instance = mockedInstance;
  }

  public List<AuditValue> getFullTradeAudit(JDate date) {
    final List<AuditValue> fullTradeAudit = new ArrayList<AuditValue>();

    try {
      final String where = buildFullAuditWhere();

      final Vector<?> rawAudit = DSConnection.getDefault().getRemoteTrade()
              .getAudit(where, "modif_date ASC", null);
      for (final Object rawAuditValue : rawAudit) {
        if (rawAuditValue instanceof AuditValue) {
          fullTradeAudit.add((AuditValue) rawAuditValue);
        }
      }
    } catch (final RemoteException e) {
      Log.error(
              this,
              "Could not retrieve Audit Values on date "
                      + date.toString(), e);
    }

    return fullTradeAudit;
  }

  private String buildFullAuditWhere() {
    // Date lowerDate = getLowerDate(date);
    // Date upperDate = getUpperDate(date);

    final SimpleDateFormat dateFormat = new SimpleDateFormat(
            EmirSnapshotReduxConstants.DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone
            .getTimeZone(EmirSnapshotReduxConstants.TIMEZONE_UTC));

    final StringBuilder where = new StringBuilder();
    where.append("entity_class_name = 'Trade'");
    // where.append(" AND ");
    // where.append("modif_date >= TO_DATE('");
    // where.append(dateFormat.format(lowerDate));
    // where.append("', 'dd/mm/yyyy hh24:mi:ss')");
    // where.append(" AND ");
    // where.append("modif_date < TO_DATE('");
    // where.append(upperDate);
    // where.append("', 'dd/mm/yyyy hh24:mi:ss')");

    return where.toString();
  }

  public Map<Long, List<AuditValue>> getMapByTradeId(
          Collection<AuditValue> fullAudit) {
    final Map<Long, List<AuditValue>> mapByTradeId = new TreeMap<Long, List<AuditValue>>();

    for (final AuditValue auditValue : fullAudit) {
      final long tradeId = auditValue.getEntityLongId();
      List<AuditValue> tradeAudit = mapByTradeId.get(tradeId);
      if (tradeAudit == null) {
        tradeAudit = new ArrayList<AuditValue>();
        mapByTradeId.put(tradeId, tradeAudit);
      }

      tradeAudit.add(auditValue);
    }

    return mapByTradeId;
  }

  public Date getLowerDate(JDate date) {
    final Vector<String> holidays = new Vector<String>();
    holidays.add(EmirSnapshotReduxConstants.SYSTEM_CALENDAR);
    JDate lowerJDate = Holiday.getCurrent().addBusinessDays(date, holidays,
            -1);
    lowerJDate = lowerJDate.addDays(1);

    final Calendar calendar = new GregorianCalendar(
            TimeZone.getTimeZone(EmirSnapshotReduxConstants.MADRID_TIMEZONE));
    calendar.setTimeInMillis(0);
    calendar.set(Calendar.DAY_OF_MONTH, lowerJDate.getDayOfMonth());
    calendar.set(Calendar.MONTH, lowerJDate.getMonth() - 1);
    calendar.set(Calendar.YEAR, lowerJDate.getYear());
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);

    // SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    // dateFormat.setTimeZone(TimeZone.getTimeZone(EmirSnapshotConstants.TIMEZONE_UTC));
    //
    // lowerDate = dateFormat.format(calendar.getTime());
    //
    // return lowerDate;

    return calendar.getTime();
  }

  public Date getUpperDate(JDate date) {
    final JDate upperJDate = date.addDays(1);

    final Calendar calendar = new GregorianCalendar(
            TimeZone.getTimeZone(EmirSnapshotReduxConstants.MADRID_TIMEZONE));
    calendar.setTimeInMillis(0);
    calendar.set(Calendar.DAY_OF_MONTH, upperJDate.getDayOfMonth());
    calendar.set(Calendar.MONTH, upperJDate.getMonth() - 1);
    calendar.set(Calendar.YEAR, upperJDate.getYear());
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);

    // SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    // dateFormat.setTimeZone(TimeZone.getTimeZone(EmirSnapshotConstants.TIMEZONE_UTC));
    //
    // upperDate = dateFormat.format(calendar.getTime());
    //
    // return upperDate;

    return calendar.getTime();
  }

  public List<Trade> getAllTrades(Map<Integer, List<AuditValue>> tradeAuditMap) {
    final List<Trade> trades = new ArrayList<Trade>();

    final Set<Integer> tradeIdsSet = tradeAuditMap.keySet();
    final long[] tradeIdsArray = new long[tradeIdsSet.size()];

    int iTradeId = 0;// TODO
    for (final Integer tradeId : tradeIdsSet) {
      tradeIdsArray[iTradeId] = tradeId;
      iTradeId++;
    }

    try {
      //final TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(tradeIdsArray);
      final TradeArray tradeArray = SantanderUtil.getInstance().getTradesWithTradeFilter(tradeIdsArray);
      for (int iTrade = 0; iTrade < tradeArray.size(); iTrade++) {
        trades.add(tradeArray.get(iTrade));
      }
    } catch (final RemoteException e) {
      Log.error(this, "Could not retrieve trades from database", e);
      final StringBuilder errorMessage = new StringBuilder();
      Log.error(this, errorMessage.toString(), e);
    }

    return trades;
  }

  public List<Long> getTradeIdsWithChanges(JDate date) {

    Log.info(this, "START getTradeIdsWithChanges - Get trade ids with changes on ValDate: " + date.toString());

    final List<Long> tradeIds = new ArrayList<Long>();

    final Date lowerDate = getLowerDate(date);
    final Date upperDate = getUpperDate(date);

    final SimpleDateFormat dateFormat = new SimpleDateFormat(
            EmirSnapshotReduxConstants.DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone
            .getTimeZone(EmirSnapshotReduxConstants.TIMEZONE_UTC));

    final StringBuilder query = new StringBuilder();
    query.append("SELECT DISTINCT entity_id ");
    query.append("FROM bo_audit ");
    query.append("WHERE modif_date >= TO_DATE(' ");
    query.append(dateFormat.format(lowerDate));
    query.append("', 'dd/mm/yyyy hh24:mi:ss') ");
    query.append("AND modif_date < TO_DATE(' ");
    query.append(dateFormat.format(upperDate));
    query.append("', 'dd/mm/yyyy hh24:mi:ss') ");
    query.append("AND entity_class_name = 'Trade' ");
    query.append("ORDER BY entity_id ASC");

    try {
      //final Vector<?> rawResultSet = DSConnection.getDefault().getRemoteAccess().executeSelectSQL(query.toString(), null);
      final Vector<?> rawResultSet =
              SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                      .executeSelectSQL(query.toString());

      if (rawResultSet.size() > 2) {
        final Vector<Vector<Long>> result = SantanderUtil.getInstance().getDataFixedResultSetWithType(rawResultSet,
                Long.class);
        for (final Vector<Long> v : result) {
          final Long tradeId = v.get(0);
          tradeIds.add(tradeId);
        }
      }
    } catch (final RemoteException e) {
      Log.error(this, "Could not execute query.", e);
    }

    Log.info(this, "END getTradeIdsWithChanges - Get trade ids with changes on ValDate: " + date.toString()
            + " - Total TradeIds: [" + tradeIds.size() + "]");

    return tradeIds;
  }

  public List<AuditValue> getTradeAudit(Long tradeId) {
    final List<Long> listTradeId = new ArrayList<Long>();
    listTradeId.add(tradeId);
    return getTradeAudits(listTradeId);
  }

  public List<AuditValue> getTradeAudits(List<Long> tradeIds) {
    final StringBuilder where = new StringBuilder();
    final String fullWhere = buildFullAuditWhere();
    where.append(fullWhere);
    where.append(" AND entity_id IN ('");
    for (int iTradeId = 0; iTradeId < tradeIds.size(); iTradeId++) {
      where.append(tradeIds.get(iTradeId));
      if (iTradeId < tradeIds.size() - 1) {
        where.append('\'').append(',').append('\'');
      }
    }
    where.append("')");

    final List<AuditValue> partialTradeAudit = new ArrayList<AuditValue>();

    try {
      final Vector<?> rawAudit = DSConnection.getDefault().getRemoteTrade()
              .getAudit(where.toString(), "modif_date ASC", null);
      for (final Object rawAuditValue : rawAudit) {
        if (rawAuditValue instanceof AuditValue) {
          partialTradeAudit.add((AuditValue) rawAuditValue);
        }
      }
    } catch (final RemoteException e) {
      Log.error(this, "Could not retrieve Audit Values", e);
    }

    return partialTradeAudit;
  }

  public List<Trade> getTrades(List<Long> tradeIds, final JDate valDate) {
    final long[] tradeIdsArray = new long[tradeIds.size()];
    int iTradeId = 0;
    for (final Long tradeId : tradeIds) {
      tradeIdsArray[iTradeId] = tradeId;
      iTradeId++;
    }

    final List<Trade> trades = new ArrayList<Trade>();

    try {
      //final TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(tradeIdsArray);
      final TradeArray tradeArray = SantanderUtil.getInstance().getTradesWithTradeFilter(tradeIdsArray);
      for (int iTrade = 0; iTrade < tradeArray.size(); iTrade++) {
        final Trade trade = tradeArray.get(iTrade);
        if (isTradeReportable(trade) || isTradeNonReportableByLeiChange(trade, valDate)) {
          trades.add(trade);
        }
      }
    } catch (final RemoteException e) {
      Log.error(this, "Could not retrieve trades from database", e);
    }

    return trades;
  }

  public List<Trade> getTrades(final List<Long> tradeIds, String poShortNames, final JDate valDate) {
    final List<Trade> trades = new ArrayList<Trade>();

    final String ids = StringUtils.join(tradeIds.toArray(), "', '");
    poShortNames = poShortNames.replaceAll(
            EmirSnapshotReduxConstants.DELIMITER, "', '");

    final TradeFilter filter = new TradeFilter();

    final StringBuilder whereClause = new StringBuilder();
    whereClause.append(" trade.trade_id IN ('");
    whereClause.append(ids);
    whereClause.append("') ");
    whereClause.append(" AND ");
    whereClause.append(" legal_entity.short_name IN ('");
    whereClause.append(poShortNames);
    whereClause.append("') ");
    whereClause.append(" AND ");
    whereClause.append(" trade.book_id = book.book_id ");
    whereClause.append(" AND ");
    whereClause
            .append(" book.legal_entity_id = legal_entity.legal_entity_id ");

    filter.setSQLFromClause(" trade, book, legal_entity");
    filter.setSQLWhereClause(whereClause.toString());

    try {
      final TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade()
              .getTrades(filter, null);

      for (int iTrade = 0; iTrade < tradeArray.size(); iTrade++) {
        final Trade trade = tradeArray.get(iTrade);
        if (isTradeReportable(trade) || isTradeNonReportableByLeiChange(trade, valDate)) {
          trades.add(trade);
        }
      }
    } catch (final RemoteException e) {
      Log.error(this, "Error getting trades from RemoteTrade.", e);
    }

    return trades;
  }

  public List<Trade> filterDelegateTrades(List<Trade> trades) {
    final List<Trade> delegateTrades = new ArrayList<Trade>();

    for (final Trade trade : trades) {
      if (isFullDelegation(trade)) {
        delegateTrades.add(trade);
      }
    }

    return delegateTrades;
  }

  public boolean isTradeReportable(Trade trade) {
    return checkAttrsTradeReportable(trade) && !isInternal(trade);
  }

  private boolean checkAttrsTradeReportable(final Trade trade) {
    boolean isReportable = true;

    isReportable = isReportable
            && Arrays.asList(REPORTABLE_PRODUCTS).contains(
            trade.getProductType());
    isReportable = isReportable
            && !Arrays.asList(NOT_REPORTABLE_POS).contains(
            trade.getBook().getLegalEntity().getCode());
    isReportable = isReportable
            && !Arrays.asList(NOT_REPORTABLE_STATUSES).contains(
            trade.getStatus().toString());

    return isReportable;
  }

  public boolean isInternal(Trade trade) {
    final int poId = trade.getBook().getLegalEntity().getId();
    final int cptyId = trade.getCounterParty().getId();

    Collection<LegalEntityAttribute> poAttributes = leAttributesMap
            .get(poId);
    if (poAttributes == null) {
      poAttributes = getLegalEntityAttributes(poId);
      leAttributesMap.put(poId, poAttributes);
    }

    Collection<LegalEntityAttribute> cptyAttributes = leAttributesMap
            .get(cptyId);
    if (cptyAttributes == null) {
      cptyAttributes = getLegalEntityAttributes(cptyId);
      leAttributesMap.put(cptyId, cptyAttributes);
    }

    return TradeUtil.getInstance().isInternal(trade, poAttributes,
            cptyAttributes);
  }

  private Collection<LegalEntityAttribute> getLegalEntityAttributes(int leId) {
    final Collection<LegalEntityAttribute> legalEntityAttributes = new LinkedList<LegalEntityAttribute>();

    final Vector<?> rawAttributes = BOCache.getLegalEntityAttributes(
            DSConnection.getDefault(), leId);

    if(rawAttributes != null) {
      for (final Object rawAttribute : rawAttributes) {
        if (rawAttribute instanceof LegalEntityAttribute) {
          legalEntityAttributes.add((LegalEntityAttribute) rawAttribute);
        }
      }
    }

    return legalEntityAttributes;
  }

  public List<Long> getTradeIds(List<Trade> trades) {
    final List<Long> tradeIds = new ArrayList<Long>();

    for (final Trade trade : trades) {
      tradeIds.add(trade.getLongId());
    }

    return tradeIds;
  }

  public Map<Long, Map<Integer, Trade>> getTradesWithVersions(
          Collection<Trade> trades,
          Map<Long, List<AuditValue>> auditByTrade) {
    final Map<Long, Map<Integer, Trade>> tradesWithVersions = new TreeMap<Long, Map<Integer, Trade>>();

    for (final Trade trade : trades) {
      final Map<Integer, Trade> allTradeVersions = getAllTradeVersions(trade,
              auditByTrade.get(trade.getLongId()));
      tradesWithVersions.put(trade.getLongId(), allTradeVersions);
    }

    return tradesWithVersions;
  }

  public Map<Integer, Trade> getAllTradeVersions(Trade currentTrade,
                                                 List<AuditValue> tradeAudit) {
    final Map<Integer, Trade> allTradeVersions = new TreeMap<Integer, Trade>();

    final Map<Integer, List<AuditValue>> tradeAuditPerVersion = new TreeMap<Integer, List<AuditValue>>();
    for (final AuditValue auditValue : tradeAudit) {
      final int versionNum = auditValue.getVersion();
      List<AuditValue> partialTradeAudit = tradeAuditPerVersion
              .get(versionNum);
      if (partialTradeAudit == null) {
        partialTradeAudit = new ArrayList<AuditValue>();
        tradeAuditPerVersion.put(versionNum, partialTradeAudit);
      }
      partialTradeAudit.add(auditValue);
    }

    allTradeVersions.put(currentTrade.getVersion(), currentTrade);
    Trade lastTrade = currentTrade;
    for (int versionNum = currentTrade.getVersion() - 1; versionNum >= 0; versionNum--) {
      final List<AuditValue> partialTradeAudit = tradeAuditPerVersion.get(versionNum + 1);
      if (partialTradeAudit != null && partialTradeAudit.size() > 0) {
        final Trade tradeWithVersion = lastTrade.clone();
        for (final AuditValue auditValue : partialTradeAudit) {
          tradeWithVersion.undo(DSConnection.getDefault(), auditValue);
        }
        // Set updated date time
        final List<AuditValue> currentVersionAudit = tradeAuditPerVersion.get(versionNum);
        if (currentVersionAudit != null && currentVersionAudit.size() > 0) {
          final JDatetime modifDate = currentVersionAudit.get(0).getModifDate();
          tradeWithVersion.setUpdatedTime(modifDate);
        }

        allTradeVersions.put(versionNum, tradeWithVersion);
        lastTrade = tradeWithVersion;
      }
    }

    return allTradeVersions;
  }

  public Map<Integer, Set<Integer>> getVersionsOnDate(JDate date,
                                                      List<AuditValue> fullAudit) {
    final Map<Integer, Set<Integer>> versionsOnDate = new TreeMap<Integer, Set<Integer>>();

    final Date lowerDate = getLowerDate(date);
    final Date upperDate = getUpperDate(date);

    for (final AuditValue auditValue : fullAudit) {
      final Date modifDate = auditValue.getModifDate();
      if (!modifDate.before(lowerDate) && modifDate.before(upperDate)) {
        final int tradeId = auditValue.getEntityId();
        Set<Integer> tradeVersions = versionsOnDate.get(tradeId);
        if (tradeVersions == null) {
          tradeVersions = new TreeSet<Integer>();
          versionsOnDate.put(tradeId, tradeVersions);
        }
        tradeVersions.add(auditValue.getVersion());
      }
    }

    return versionsOnDate;
  }

  public Map<Long, List<AuditValue>> filterAuditOnDate(JDate date, Map<Long, List<AuditValue>> auditByTrade) {
    final Map<Long, List<AuditValue>> auditByTradeOnDate = new TreeMap<Long, List<AuditValue>>(
            auditByTrade);
    final Date lowerDate = getLowerDate(date);
    final Date upperDate = getUpperDate(date);

    for (final Entry<Long, List<AuditValue>> entry : auditByTradeOnDate
            .entrySet()) {
      final List<AuditValue> tradeAudit = entry.getValue();
      final List<AuditValue> filteredTradeAudit = new ArrayList<AuditValue>();
      for (final AuditValue auditValue : tradeAudit) {
        final Date modifDate = auditValue.getModifDate();
        if (!modifDate.before(lowerDate) && modifDate.before(upperDate)) {
          filteredTradeAudit.add(auditValue);
        }
      }
      entry.setValue(filteredTradeAudit);
    }

    return auditByTradeOnDate;
  }

  public void concatenateItems(SantEmirSnapshotReportItem item1,
                               SantEmirSnapshotReportItem item2) {
    final Set<String> item1ColumnNames = item1.getColumnNames();

    for (final String columnName : item2.getColumnNames()) {
      final Object value = item2.getColumnValue(columnName);
      if (value != null
              && (!item1ColumnNames.contains(columnName) || (item1ColumnNames
              .contains(columnName) && EmirSnapshotReduxConstants.CADENA_NULL
              .equals(item1.getReportTypeValue(columnName))))) {
        final String reportType = item2.getReportTypeValue(columnName);

        item1.setColumnValue(columnName, value);
        item1.setReportTypeValue(columnName, reportType);
      }
    }
  }

  public List<EmirReport> createEmirReportMessage(Trade trade,
                                                  SantEmirSnapshotReportItem item, JDatetime valDatetime) {
    final List<EmirReport> emirMessage = new LinkedList<EmirReport>();

    final String action = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION);
    final String partenonId = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_PARTENON_ID);
    final String murexTradeId = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRADE_ID);
    final String poName = trade.getBook().getLegalEntity().getAuthName();
    final JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
    final long tradeId = trade.getLongId();
    final int tradeVersion = trade.getVersion();
    final String transtype = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE);

    for (final String columnName : item.getColumnNames()) {
      final String value = String
              .valueOf(item.getColumnValue(columnName));

      if (!Util.isEmpty(value)) {
        final EmirReport row = new EmirReport();

        row.setAction(action);
        // row.setFarLeg(String);
        row.setPartenonId(partenonId);
        row.setMurexTradeId(murexTradeId);
        row.setPo(poName);
        row.setReportDate(valDate);
        row.setReportType(item.getReportTypeValue(columnName));
        row.setTag(getColumnName(columnName));
        row.setTradeId(tradeId);
        row.setTradeVersion(tradeVersion);
        row.setTranstype(transtype);
        row.setValue(value);

        emirMessage.add(row);
      }

    }

    Collections.sort(emirMessage, new EmirReportComparator());

    return emirMessage;
  }

  private String getColumnName(final String columnName) {

    if (EmirSnapshotReduxConstants.SUBMITTEDVALUE_VAL.equals(columnName)) {
      return EmirSnapshotColumn.SUBMITTEDVALUE.toString();
    }
    return columnName;
  }

  private class EmirReportComparator implements Comparator<EmirReport> {

    @Override
    public int compare(EmirReport row1, EmirReport row2) {
      return row1.getTag().compareTo(row2.getTag());
    }

  }

  public Map<String, Long> getTradeIdsByExternalReference(Map<Long, Map<Integer, Trade>> tradesWithVersion) {
    final Map<String, Long> tradeIdsByExternalReference = new HashMap<String, Long>();

    for (final Entry<Long, Map<Integer, Trade>> entry : tradesWithVersion
            .entrySet()) {
      final long tradeId = entry.getKey();
      final Map<Integer, Trade> allTradeVersionsMap = entry.getValue();
      final Collection<Trade> allTradeVersions = allTradeVersionsMap.values();
      // It shouldn't matter which version of the trade we use, all should
      // have the same external reference.
      String externalReference = null;
      final Iterator<Trade> iTrade = allTradeVersions.iterator();
      while (externalReference == null && iTrade.hasNext()) {
        final Trade trade = iTrade.next();
        externalReference = trade.getExternalReference();
      }

      tradeIdsByExternalReference.put(externalReference, tradeId);
    }

    return tradeIdsByExternalReference;
  }

  public Map<String, Long> getTradeIdsByMurexTradeID(Map<Long, Map<Integer, Trade>> tradesWithVersion) {
    final Map<String, Long> tradeIdsByExternalReference = new HashMap<String, Long>();

    for (final Entry<Long, Map<Integer, Trade>> entry : tradesWithVersion
            .entrySet()) {
      final long tradeId = entry.getKey();
      final Map<Integer, Trade> allTradeVersionsMap = entry.getValue();
      final Collection<Trade> allTradeVersions = allTradeVersionsMap.values();
      // It shouldn't matter which version of the trade we use, all should
      // have the same external reference.
      String externalReference = null;
      final Iterator<Trade> iTrade = allTradeVersions.iterator();
      while (externalReference == null && iTrade.hasNext()) {
        final Trade trade = iTrade.next();
        externalReference = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRADE_ID);
      }
      tradeIdsByExternalReference.put(externalReference, tradeId);
    }

    return tradeIdsByExternalReference;
  }


  public boolean isChangeInCounterparty(Trade previousTrade, Trade newTrade) {
    boolean changeInCounterparty = false;

    if (previousTrade != null && newTrade != null) {
      final LegalEntity previousCounterparty = previousTrade
              .getCounterParty();
      final LegalEntity newCounterparty = newTrade.getCounterParty();

      final String previousCode = previousCounterparty.getCode();
      final String newCode = newCounterparty.getCode();

      changeInCounterparty = !newCode.equals(previousCode);
    }

    return changeInCounterparty;
  }

  public boolean isLeiChange(Trade previousTrade, Trade newTrade) {
    LegalEntityAttribute currentLEI = LegalEntityAttributesCache.getInstance().getAttribute(newTrade, EmirSnapshotReduxConstants.LEI, true);
    LegalEntityAttribute previousLEI = LegalEntityAttributesCache.getInstance().getAttribute(previousTrade, EmirSnapshotReduxConstants.LEI, true);

    boolean isSameLEI =  (currentLEI != null
            && previousLEI != null
            && !currentLEI.getAttributeValue().equals(previousLEI.getAttributeValue())) ;

    return isSameLEI;
  }


  public Trade cloneTrade(Trade trade) {
    final Trade clonedTrade = trade.clone();
    return clonedTrade;
  }

  public Map<String, Boolean> getColumnsMap(
          EmirSnapshotColumn[] columns) {
    final Map<String, Boolean> columnsMap = new HashMap<String, Boolean>();
    for (int iColumn = 0; iColumn < columns.length; iColumn++) {
      columnsMap.put(columns[iColumn].name(), Boolean.TRUE);
    }

    return columnsMap;
  }

  public void filterColumns(List<EmirReport> emirMessage,
                            Map<String, Boolean> columns) {
    final List<EmirReport> filteredMessage = new ArrayList<EmirReport>();

    for (final EmirReport row : emirMessage) {
      if (!Util.isEmpty(row.getReportType())
              && !EmirSnapshotReduxConstants.CADENA_NULL.equals(row
              .getReportType())
              && columns.containsKey(row.getTag())) {
        filteredMessage.add(row);
      }
    }

    emirMessage.clear();
    emirMessage.addAll(filteredMessage);
  }

  public LegType getLegType(Trade trade) {
    LegType legType = LegType.NOT_SWAP;

    final String legTypeAtribute = trade.getKeywordValue(FXSwap.LEG_TYPE);
    if (FXSwap.NEAR_LEG.equals(legTypeAtribute)) {
      legType = LegType.NEAR;
    } else if (FXSwap.FAR_LEG.equals(legTypeAtribute)) {
      legType = LegType.FAR;
    }

    return legType;
  }

  public boolean isFullDelegation(final Trade trade) {
    final String value = EmirFieldBuilderUtil.getInstance()
            .getLegalEntityAttribute(trade,
                    EmirSnapshotReduxConstants.LE_ATTRIBUTE_EMIR_FULL_DELEG
                    , true);

    return Boolean.TRUE.toString().equalsIgnoreCase(value);
  }

  // ------------------------ Task Util

  /**
   * Checks if a trade should be processed by the AsyncWfEngine.
   *
   * @param trade
   *            Trade to check
   * @return True if this trade should be processed, or false otherwise.
   */
  public boolean acceptTask(Trade trade, final String[] productTypes,
                            final String[] acceptedTradeStatuses) {
    return acceptTask(trade, true, productTypes, acceptedTradeStatuses);
  }

  /**
   * Checks if a trade should be processed by the AsyncWfEngine.
   *
   * @param trade
   *            Trade to check
   * @param checkReissue
   *            If true, we'll check if this trade is a reissue or a novation
   *            of a reportable trade. If false, only the standard check will
   *            be performed.
   * @param productTypes
   *            Product Types accepted
   * @param acceptedTradeStatuses
   *            Status accepted
   * @return True if this trade should be processed, or false otherwise.
   */
  private boolean acceptTask(final Trade trade, final boolean checkReissue,
                             final String[] productTypes, final String[] acceptedTradeStatuses) {
    boolean taskAccepted = true;

    // Product type
    final String productType = trade.getProductType();
    if (taskAccepted
            && !Arrays.asList(productTypes).contains(productType)) {
      taskAccepted = false;
    }

    // Status as String
    if (taskAccepted && !Arrays.asList(acceptedTradeStatuses)
            .contains(trade.getStatus().getStatus())) {
      taskAccepted = false;
    }

    if (taskAccepted) {
      // Trade is not internal
      final LegalEntity cp = trade.getCounterParty();
      final Collection<LegalEntityAttribute> cpAtts = getLegalEntityAttributes(
              cp.getId());

      final LegalEntity po = trade.getBook().getLegalEntity();
      final Collection<LegalEntityAttribute> poAtts = getLegalEntityAttributes(
              po.getId());

      final boolean isInternalDeal = TradeUtil.getInstance()
              .isInternal(trade, poAtts, cpAtts);
      if (isInternalDeal) {
        taskAccepted = false;
      }
    }

    if (!taskAccepted && checkReissue) {
      taskAccepted = isReissueOfReportableTrade(trade, false);
    }

    return taskAccepted;
  }

  /**
   * Checks if a trade should be processed by the AsyncWfEngine.
   *
   * @param trade
   *            Trade to check
   * @param checkReissue
   *            If true, we'll check if this trade is a reissue or a novation
   *            of a reportable trade. If false, only the standard check will
   *            be performed.
   * @return True if this trade should be processed, or false otherwise.
   */
  private boolean acceptTask(Trade trade, boolean checkReissue) {
    boolean taskAccepted = true;

    // Product type is FXForward, FXNDF or FXSwap
    final String productType = trade.getProductType();
    if (taskAccepted
            && !Arrays.asList(REPORTABLE_PRODUCTS).contains(productType)) {
      taskAccepted = false;
    }

    // Status is PENDING, CANCELED or TERMINATED
    if (taskAccepted && !Arrays.asList(ACCEPTED_TRADE_STATUSES)
            .contains(trade.getStatus())) {
      taskAccepted = false;
    }

    if (taskAccepted) {
      // Trade is not internal
      final LegalEntity cp = trade.getCounterParty();
      final Collection<LegalEntityAttribute> cpAtts = getLegalEntityAttributes(
              cp.getId());

      final LegalEntity po = trade.getBook().getLegalEntity();
      final Collection<LegalEntityAttribute> poAtts = getLegalEntityAttributes(
              po.getId());

      final boolean isInternalDeal = TradeUtil.getInstance()
              .isInternal(trade, poAtts, cpAtts);
      if (isInternalDeal) {
        taskAccepted = false;
      }
    }

    if (!taskAccepted && checkReissue) {
      taskAccepted = isReissueOfReportableTrade(trade, false);
    }

    return taskAccepted;
  }

  /**
   * Checks if this trade is a non reportable trade that is a reissue or
   * novation of a reportable trade.
   *
   * @param trade
   *            Trade to check
   *
   * @return True if an EXIT message should be generated for the previous
   *         trade.
   */
  private boolean isReissueOfReportableTrade(Trade trade,
                                             boolean checkIfReportable) {
    boolean reissueOfReportable = false;

    boolean reportable = false;
    if (checkIfReportable) {
      reportable = acceptTask(trade, false);
    }
    if (!reportable) {
      final String reportableKeyword = trade
              .getKeywordValue(TRADE_KEYWORD_REISSUE_REPORTABLE);
      if (!Util.isEmpty(reportableKeyword)) {
        reissueOfReportable = Boolean.valueOf(reportableKeyword);
      } else {
        String previousTradeRef = trade.getKeywordValue(
                KeywordConstantsUtil.KEYWORD_CANCEL_REISSUE_FROM);
        if (Util.isEmpty(previousTradeRef)) {
          previousTradeRef = trade.getKeywordValue(
                  KeywordConstantsUtil.KEYWORD_NOVATION_FROM);
        }

        if (!Util.isEmpty(previousTradeRef)) {
          try {
            final TradeArray previousTradeArray = DSConnection
                    .getDefault().getRemoteTrade()
                    .getTradesByExternalRef(previousTradeRef);
            if (previousTradeArray != null
                    && previousTradeArray.size() > 0) {
              final Trade previousTrade = previousTradeArray.get(0);
              // checkReissue: false
              // Do not check if the previous trade is also a
              // reissue
              // of a reportable trade.
              reissueOfReportable = acceptTask(previousTrade,
                      false);
            }
          } catch (final RemoteException e) {
            final String errorMessage = String.format(
                    "Could not retrieve trades with external reference \"%s\"",
                    previousTradeRef);
            Log.error(this, errorMessage, e);
          }
        }

        trade.addKeyword(TRADE_KEYWORD_REISSUE_REPORTABLE,
                Boolean.toString(reissueOfReportable));
      }
    }

    return reissueOfReportable;
  }

  public Trade getTradeByExternalReference(final String externalReference) {
    return TradeUtil.getInstance().getTradeFromMurexReference(externalReference);
  }

  /**
   * @param keywordName
   * @param keywordValue
   * @return
   */
  public TradeArray getTradeByKeyword(String keywordName, String keywordValue, boolean includeCancel) {
    TradeArray existingTrades = null;
    try {

      StringBuilder where = new StringBuilder();
      where.append(" trade.trade_id=kwd.trade_id ");
        if (!includeCancel)  {
          where.append(" and trade.trade_status<>'CANCELED' ");
        }
        where.append(" and kwd.keyword_name='" + keywordName + "' and kwd.keyword_value='" + keywordValue + "' ");

      existingTrades = DSConnection
              .getDefault()
              .getRemoteTrade()
              .getTrades(
                      "trade, trade_keyword kwd",
                      where.toString(),
                      null, null);
    } catch (RemoteException e) {
      Log.error(this, e);
      existingTrades = null;
    }
    return existingTrades;
  }

  public Trade getTradeByMurexTradeId(String keywordValue, boolean includeCanceled) {
    if (!Util.isEmpty(keywordValue)) {
      TradeArray existingTrades = getTradeByKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRADE_ID , keywordValue, includeCanceled);
      if (!existingTrades.isEmpty()
              && existingTrades.size()>0) {
        return existingTrades.get(0);

      }
    }
    return null;
  }

  /**
   * Check if the attribute LEI has changed.
   */
  public boolean isChangeLeiAttribute(final Trade previousTrade, final Trade newTrade) {

    if (previousTrade != null && newTrade != null) {
      final String leiAttrPrevTrade = getNullable(LegalEntityAttributesCache.getInstance().getAttributeValue(
              previousTrade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true));
      final String leiAttrNewTrade = getNullable(LegalEntityAttributesCache.getInstance().getAttributeValue(newTrade,
              KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true));

      return !leiAttrNewTrade.equals(leiAttrPrevTrade);
    }

    return false;
  }

  /**
   * getNullable
   *
   * @param s String
   * @return String
   */
  private String getNullable(String s) {
    String result = EmirSnapshotReduxConstants.EMPTY_SPACE;
    if (null != s) {
      result = s;
    }
    return result;
  }

  // DDR v25

  /**
   * Checks if trade was EMIR reportable on ValDate, but after a change in counterparty's attribute
   * LEI, the trade became non EMIR reportable.
   *
   * @param trade
   * @return
   */
  private boolean isTradeNonReportableByLeiChange(final Trade trade, final JDate valDate) {

    final String prevLei = trade.getKeywordValue(KeywordConstantsUtil.TRADE_KEYWORD_PREVIOUSLEIVALUE);

    // Checks if trade is NOT reportable because of is INTERNAL and the trade has suffered a LEI
    // change and the trade is not entered today (only D+1).

    final boolean isEnteredBeforeToday = trade.getEnteredDate().getJDate(TimeZone.getDefault()).before(valDate);

    if (checkAttrsTradeReportable(trade) && isInternal(trade) && !Util.isEmpty(prevLei) && isEnteredBeforeToday) {

      Log.debug(this, "Checking the trade " + trade.getLongId()
              + ", that is NON EMIR Reportable, with a previous LEI Value " + prevLei);

      // The cpty's trade has changed, so trade may have been reportable on ValDate

      // Get audit on ValDate
      final List<AuditValue> tradeAllAudit = getTradeAudit(trade.getLongId());
      final Map<Long, List<AuditValue>> auditByTradeOnValDate = filterAuditOnDate(valDate, getMapByTradeId(tradeAllAudit));
      final List<AuditValue> tradeAuditOnValDate = auditByTradeOnValDate.get(trade.getLongId());

      // Checks if the change LEI has happened on ValDate
      boolean leiChangeOnValDate = false;
      for (final AuditValue auditValue : tradeAuditOnValDate) {
        final String fieldName = auditValue.getFieldName();
        if (!Util.isEmpty(fieldName) && fieldName.endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_PREVIOUSLEIVALUE)) {
          leiChangeOnValDate = true;
          break;
        }
      }

      if (leiChangeOnValDate) {
        // The change LEI has happened on ValDate. Check if the trade was reportable with the
        // previous LEI value.
        return !wasInternal(trade, prevLei);

      } else {
        // The change LEI has not happened on ValDate.
        Log.debug(this,
                "The change LEI in trade " + trade.getLongId() + ", has not happened on ValDate " + valDate.toString());
        return false;
      }
    }

    // There is not a change LEI or the trade is not internal or the trade is not reportable due to
    // Status/Po/Product or the trade is entered today
    return false;
  }

  public boolean wasInternal(Trade trade, final String prevCptyLei) {

    Log.debug(this, "Checking if the PO attribute LEI value is equals to previous CPTY attribute LEI value.");

    final int poId = trade.getBook().getLegalEntity().getId();

    Collection<LegalEntityAttribute> poAttributes = leAttributesMap.get(poId);
    if (poAttributes == null) {
      poAttributes = getLegalEntityAttributes(poId);
      leAttributesMap.put(poId, poAttributes);
    }

    if (poAttributes == null) {
      return false;
    }

    final Iterator<LegalEntityAttribute> poIter = poAttributes.iterator();
    LegalEntityAttribute currentAtt = null;
    String poLei = "";

    while (poIter.hasNext()) {
      currentAtt = poIter.next();
      if (KeywordConstantsUtil.LE_ATTRIBUTE_LEI.contains(currentAtt.getAttributeType())) {
        poLei = currentAtt.getAttributeValue();
      }
    }

    if ((trade.getMirrorTradeId() != 0) || ((poLei != null) && (prevCptyLei != null) && poLei.equalsIgnoreCase(prevCptyLei))) {
      return true;
    }

    return false;
  }

  // DDR v25 - End


  // DDR MedioDePago

  public List<Long> getAcceptedTradeIds(final JDate date) {
    // Get TradeIds with changes on valDate
    final List<Long> tradeidsWithChanges = getTradeIdsWithChanges(date);

    // Get TradeIds without keyword MedioDePago
    // FX - final List<Long> acceptiedTradeIds = getTradeIdsNotMedioDePago(tradeidsWithChanges);
    //TODO do we need this in collaterales?
    return tradeidsWithChanges;
  }

  /**
   * Get TradeId list of trades with some change on JDate. In addition, filter trades has the
   * keyword "MedioDePago" not null/empty.
   *
   * @param tradeIdsWithChangesOnValDate
   * @return
   */
  public List<Long> getTradeIdsNotMedioDePago(final List<Long> tradeIdsWithChangesOnValDate) {

    Log.info(this, "START getTradeIdsNotMedioDePago - Get trade ids without keyword 'MedioDePago'.");

    final List<Long> tradeIdsNotMedioDePago = new ArrayList<Long>();

    /*
    if (Util.isEmpty(tradeIdsWithChangesOnValDate)) {
      return tradeIdsNotMedioDePago;
    }

    final StringBuilder where = new StringBuilder();

    if (tradeIdsWithChangesOnValDate.size() > EmirSnapshotReduxConstants.MAX_SIZE) {
      final List<String> aStringList = iterateMaxSize(tradeIdsWithChangesOnValDate, "trade_id IN ");
      final String aWhere = StringUtils.join(aStringList, " OR ");
      where.append(aWhere);
    } else {
      final String tradeIdsWithChangesSql = Util.collectionToSQLString(tradeIdsWithChangesOnValDate);
      where.append(" trade_id IN ");
      where.append(tradeIdsWithChangesSql);
    }

    // Get the trades without keyword MEDIODEPAGO on ValDate
    final StringBuilder query = new StringBuilder();
    query.append("SELECT trade_id ");
    query.append("FROM trade ");
    query.append("WHERE ");
    query.append(where);
    query.append(" AND trade_id NOT IN ");
    query.append("(SELECT trade_keyword.trade_id ");
    query.append("FROM trade_keyword ");
    query.append("WHERE trade_keyword.KEYWORD_NAME = '");
    query.append(KeywordConstantsUtil.TRADE_KEYWORD_MEDIODEPAGO);
    query.append("' ");
    query.append("AND trade_keyword.KEYWORD_VALUE = 'S') ");
    query.append("ORDER BY trade_id ASC");

    try {
      final Vector<?> rawResultSet = DSConnection.getDefault()
              .getRemoteAccess().executeSelectSQL(query.toString(), null);


      if (rawResultSet.size() > 2) {
        final Vector<Vector<Long>> result = SantanderUtil.getInstance().getDataFixedResultSetWithType(rawResultSet,
                Long.class);
        for (final Vector<Long> v : result) {
          final Long tradeId = v.get(0);
          tradeIdsNotMedioDePago.add(tradeId);
        }
      }
    } catch (final RemoteException e) {
      Log.error(this, "Could not execute query.", e);
    }

    Log.info(this, "END getTradeIdsNotMedioDePago - Get trade ids without keyword 'MedioDePago' - Total TradeIds: ["
            + tradeIdsNotMedioDePago.size() + "]");
     */

    return tradeIdsNotMedioDePago;
  }

  private static <T> List<String> iterateMaxSize(final List<T> aList, final String aString) {
    final List<String> aStringList = new ArrayList<String>();
    boolean control = true;
    final int sizeMax = aList.size();
    int n = 0;
    int m = EmirSnapshotReduxConstants.MAX_SIZE;

    while (control) {
      final List<T> copy = new ArrayList<T>();
      copy.addAll(aList);
      final List<T> partList = safeSubList(copy, n, m);
      if (partList.isEmpty() || m == sizeMax) {
        control = false;
      } else {
        n = n + EmirSnapshotReduxConstants.MAX_SIZE;
        m = m + EmirSnapshotReduxConstants.MAX_SIZE;
        if (m >= sizeMax) {
          m = sizeMax;
        }
      }
      aStringList.add(aString + Util.collectionToSQLString(partList));
      partList.clear();
    }

    return aStringList;
  }

  public static <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
    final int size = list.size();
    if (fromIndex >= size || toIndex <= 0 || fromIndex >= toIndex) {
      return Collections.emptyList();
    }

    fromIndex = Math.max(0, fromIndex);
    toIndex = Math.min(size, toIndex);

    return list.subList(fromIndex, toIndex);
  }

  // DDR MedioDePago - End

}
