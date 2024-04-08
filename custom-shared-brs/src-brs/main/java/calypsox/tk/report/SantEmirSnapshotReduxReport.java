/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.core.EmirReport;
import calypsox.tk.core.GenericReg_EmirReport;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxProcessor;
import calypsox.tk.util.emir.EmirSnapshotReduxReportLogic;
import calypsox.tk.util.emir.EmirSnapshotReduxUtil;
import calypsox.tk.util.emir.EmirSnapshotReportType;
import calypsox.tk.util.emir.EmirUtil;
import calypsox.tk.util.emir.MasterLegalAgreementsCache;
import calypsox.tk.util.emir.OriginalTradesCache;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.report.TransferReportTemplate;
import com.calypso.tk.util.TradeArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

public class SantEmirSnapshotReduxReport extends TradeReport implements CheckRowsNumberReport {

  private static final long serialVersionUID = -1L;

  private final EmirSnapshotReduxUtil util = EmirSnapshotReduxUtil.getInstance();
  private final Set<Long> relatedTradeIds = new HashSet<Long>();

  /**
   * instance
   */
  private static final SantEmirSnapshotReduxReport INSTANCE = new SantEmirSnapshotReduxReport();
  private static SantEmirSnapshotReduxReport instance = INSTANCE;

  /**
   * Singleton access to GenericReg_SantEmirSnapshotReduxReport.
   *
   * @return singleton instance of GenericReg_SantEmirSnapshotReduxReport.
   */
  public static SantEmirSnapshotReduxReport getInstance() {
    return instance;
  }

  /**
   * Just for testing purposes. It should receive a mocked instance of
   * GenericReg_SantEmirSnapshotReduxReport.
   *
   * @param mockedUtil
   *            mocked instance of GenericReg_SantEmirSnapshotReduxReport.
   */
  public static void setInstance(
      final SantEmirSnapshotReduxReport mockedUtil) {
    if (mockedUtil == null) {
      instance = INSTANCE;
    } else {
      instance = mockedUtil;
    }
  }

  private void addToTradeIdsRelated(final long tradeId) {
    relatedTradeIds.add(tradeId);
  }

  private Set<Long> getTradeIdsRelated() {
    return new HashSet<Long>(relatedTradeIds);
  }

  private void clearAll() {
    relatedTradeIds.clear();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public ReportOutput load(final Vector errors) {

    Log.debug(this, "START GenericReg_SantEmirSnapshotReduxReport");

    // Init
    clearAll();

    final DefaultReportOutput rst = new StandardReportOutput(this);

    // Get valuation Date
    final JDate valDate = getValuationDatetime().getJDate(
        TimeZone.getDefault());

    // -----------------------------------------------------------
    // Get report Template filter values
    // -----------------------------------------------------------
    final ReportTemplate reportTemplate = getReportTemplate();

    // Get FO ProcessingOrg
    String POAuthNameFO = EmirSnapshotReduxConstants.EMPTY_SPACE;
    if (reportTemplate.get(com.calypso.tk.report.TransferReportTemplate.PO_NAME) != null) {
      POAuthNameFO = reportTemplate.get(TransferReportTemplate.PO_NAME).toString().trim();
    }

    final String fixedCode = EmirUtil.getSourceSystem(POAuthNameFO);

    // Get FO ReportType
    String reportTypeFO = EmirSnapshotReduxConstants.EMPTY_SPACE;
    if (reportTemplate.get(SantEmirSnapshotReduxReportTemplate.REPORT_TYPES) != null) {
      reportTypeFO = reportTemplate.get(SantEmirSnapshotReduxReportTemplate.REPORT_TYPES).toString().trim();
    }

    // Get FO TradeIds
    String tradeIdsFO = EmirSnapshotReduxConstants.EMPTY_SPACE;
    if (reportTemplate.get(SantEmirSnapshotReduxReportTemplate.TRADE_ID) != null) {
      tradeIdsFO = reportTemplate.get(SantEmirSnapshotReduxReportTemplate.TRADE_ID).toString()
          .replaceAll(EmirSnapshotReduxConstants.REGEX_WHITESPACES, EmirSnapshotReduxConstants.EMPTY_SPACE).trim();
    }

    Log.info(this, "Get data from ReportTemplate fields: PO: ["
        + POAuthNameFO + "]. ReportType: [" + reportTypeFO
        + "]. TradeIds: [" + tradeIdsFO + "]");

    // -----------------------------------------------------------

    // Get Trades to report
    final List<Trade> tradesEmirReport = getTradesEmirReport(valDate,
        POAuthNameFO, tradeIdsFO);

    Log.debug(this, "EMIR reportable Trades: " + tradesEmirReport.size());

    // Get AuditValues for each Trade
    final Map<Long, List<AuditValue>> auditByTrade = getMapByTradeId(tradesEmirReport);

    // Get AuditValues for each Trade on val Date
    final Map<Long, List<AuditValue>> auditByTradeOnDate = getAuditByTradeOnDate(
        valDate, auditByTrade);

    // Get versions for each Trade
    final Map<Long, Map<Integer, Trade>> tradesWithVersions = getMapTradeWithVersions(
        tradesEmirReport, auditByTrade);

    // InitCache
    initCache(tradesEmirReport);

    // Load EmirReport with the ReportType selected by FO
    final List<EmirReport> emirReport = getEmirReport(auditByTradeOnDate,
        auditByTrade, tradesWithVersions, valDate, reportTypeFO);

    Log.debug(this, "EMIR report by ReportType [" + reportTypeFO + "]: "
        + emirReport.size());

    // Get rows
    final ArrayList<ReportRow> rows = getReportRows(emirReport, fixedCode);

    rst.setRows(rows.toArray(new ReportRow[rows.size()]));

    //Generate a task exception if the number of rows is out of an umbral defined
    HashMap<String , String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
    checkAndGenerateTaskReport(rst, value);

    // Clear
    clearAll();

    return rst;
  }

  /**
   * Call new EMIR to get the TradeIds from a List of Trades.
   *
   * @param trades
   * @return
   */
  private List<Long> getTradeIds(final List<Trade> trades) {
    return util.getTradeIds(trades);
  }

  /**
   * Call new EMIR to get the Complete Trade Audits from a List of Trades.
   *
   * @param tradeIds
   * @return
   */
  private List<AuditValue> getCompleteTradeAudits(final List<Long> tradeIds) {
    List<AuditValue> auditValues = new ArrayList<AuditValue>();

    // Control tradeId Size
    int control = 0;
    if (!Util.isEmpty(tradeIds) && tradeIds.size() >= EmirSnapshotReduxConstants.MAX_SIZE) {
      final List<Long> tradeIds_part = new ArrayList<Long>();
      for (final Long id : tradeIds) {
        tradeIds_part.add(id);
        control++;

        if (control == EmirSnapshotReduxConstants.MAX_SIZE - 1) {
          control = 0;
          // Filter by PO
          final List<AuditValue> trades_part = util.getTradeAudits(tradeIds_part);
          auditValues.addAll(trades_part);
          tradeIds_part.clear();
        }
      }

      if (control > 0) {
        // Last batch
        final List<AuditValue> trades_part = util.getTradeAudits(tradeIds_part);
        auditValues.addAll(trades_part);
      }

    } else {
      // Filter by PO
      auditValues = util.getTradeAudits(tradeIds);
    }

    return auditValues;
  }

  /**
   * Call new EMIR to get a Map with the List of Audit by TradeId.
    * @param tradesEmirReport
   * @return
   */
  private Map<Long, List<AuditValue>> getMapByTradeId(final List<Trade> tradesEmirReport) {

    // Get TradeIds
    final List<Long> tradeIdsEmirReport = getTradeIds(tradesEmirReport);

    final List<AuditValue> completeAudit = getCompleteTradeAudits(tradeIdsEmirReport);
    return util.getMapByTradeId(
        completeAudit);
  }

  /**
   * Call new EMIR to get a Map with the List of Audit by TradeId on
   * ValuationDate.
   *
   * @param valDate
   * @param auditByTrade
   * @return
   */
  private Map<Long, List<AuditValue>> getAuditByTradeOnDate(JDate valDate, Map<Long, List<AuditValue>> auditByTrade) {
    return util.filterAuditOnDate(valDate,
        auditByTrade);
  }

  /**
   * Call new EMIR to get a Map with the Trade Versions.
   *
   * @param tradesEmirReport
   * @param auditByTrade
   * @return
   */
  private Map<Long, Map<Integer, Trade>> getMapTradeWithVersions(
      List<Trade> tradesEmirReport,
      Map<Long, List<AuditValue>> auditByTrade) {
    return util.getTradesWithVersions(
        tradesEmirReport, auditByTrade);
  }

  /**
   * Get the Report Rows.
   *
   * @param emirReport
   * @param fixedCode
   * @return
   */
  private ArrayList<ReportRow> getReportRows(
          final List<EmirReport> emirReport, final String fixedCode) {
    final ArrayList<ReportRow> rows = new ArrayList<ReportRow>();

    for (int i = 0; i < emirReport.size(); ++i) {
      final EmirReport currentItem = emirReport.get(i);
      final GenericReg_EmirReport currentItemGenericReg = new GenericReg_EmirReport(
          currentItem);
      currentItemGenericReg.setFixedCode(fixedCode);

      // CAL_EMIR_002
      final ReportRow currentRow = new ReportRow(currentItem);
      currentRow.setProperty(
          SantEmirSnapshotReportItem.SANT_EMIR_SNAPSHOT_ITEM,
          currentItemGenericReg);
      rows.add(currentRow);
    }

    return rows;

  }

  /**
   * Init cache.
   *
   * @param tradesEmirReport
   */
  private void initCache(List<Trade> tradesEmirReport) {
    // Init Master Legal Agreements Cache
    MasterLegalAgreementsCache.getInstance().init(tradesEmirReport);

    // Init Original Trades Cache
    OriginalTradesCache.getInstance().init(tradesEmirReport);

  }

  /**
   * Get EMIR reporting trades.
   *
   * @param valDate
   * @param pOAuthNameFO
   * @param tradeIdsFO
   * @return
   */
  private List<Trade> getTradesEmirReport(final JDate valDate,
                                          final String pOAuthNameFO, final String tradeIdsFO) {

    List<Trade> trades = new ArrayList<Trade>();

    // Filter by selected Trade Ids
    final List<Long> tradeIds = filterBySelectedTradeIds(valDate,
        tradeIdsFO);

    // Control tradeId Size
    int control = 0;
    if (!Util.isEmpty(tradeIds) && tradeIds.size() >= EmirSnapshotReduxConstants.MAX_SIZE) {
      final List<Long> tradeIds_part = new ArrayList<Long>();
      for (final Long id : tradeIds) {
        tradeIds_part.add(id);
        control++;

        if (control == EmirSnapshotReduxConstants.MAX_SIZE - 1) {
          control = 0;
          // Filter by PO
          final List<Trade> trades_part = filterByProcessingOrg(tradeIds_part, pOAuthNameFO, valDate);
          trades.addAll(trades_part);
          tradeIds_part.clear();
        }
      }

      if (control > 0) {
        // Last batch
        final List<Trade> trades_part = filterByProcessingOrg(tradeIds_part, pOAuthNameFO, valDate);
        trades.addAll(trades_part);
      }

    } else {
      // Filter by PO
      trades = filterByProcessingOrg(tradeIds, pOAuthNameFO, valDate);
    }

    return trades;
  }

  /**
   * Get only the trades, with changes on Valuation Date,
   * selected from FO.
   *
   * @param valDate
   * @param tradeIdsFO
   * @return
   */
  private List<Long> filterBySelectedTradeIds(final JDate valDate,
      final String tradeIdsFO) {
    final List<Long> tradeIds = new ArrayList<Long>();

    // Get Accepted Trade Ids from DataBase
    final List<Long> tradeIdsWithChanges = util.getAcceptedTradeIds(valDate);

    if (!Util.isEmpty(tradeIdsFO)) {
      // Get selected Trade Ids
      final List<Long> tradeFoIds = getSelectedFOTradeIdList(tradeIdsFO);

      // Add Related TradeIds -> C&R for example
      addRelatedTradeIds(tradeFoIds, valDate);

      for (final Long foId : tradeFoIds) {
        if(tradeIdsWithChanges.contains(foId)) {
          tradeIds.add(foId);
        }
      }
    } else {
      tradeIds.addAll(tradeIdsWithChanges);
    }

    return tradeIds;
  }

  /**
   * Get only the trades whose PO is selected from FO.
   *
   * @param tradeIds
   * @param pOAuthNameFO
   * @return
   */
  private List<Trade> filterByProcessingOrg(final List<Long> tradeIds,
                                            final String pOAuthNameFO, final JDate valDate) {
    List<Trade> trades = new ArrayList<Trade>();

    if (Util.isEmpty(pOAuthNameFO)) {
      // Get EMIR reporting trades without selected PO
      trades = util.getTrades(tradeIds, valDate);
    } else {
      // Get EMIR reporting trades with selected PO
      trades = util.getTrades(tradeIds, pOAuthNameFO, valDate);

    }

    return trades;
  }

  /**
   * Parse into a list the entered trades in FO.
   *
   * @param tradeIdsFO
   * @return
   */
  private List<Long> getSelectedFOTradeIdList(final String tradeIdsFO) {
    final List<Long> tradeIds = new ArrayList<Long>();
    String[] arrayTradesIdFO = null;

    if (!Util.isEmpty(tradeIdsFO)) {
      arrayTradesIdFO = tradeIdsFO
          .split(EmirSnapshotReduxConstants.DELIMITER);
    }

    if (arrayTradesIdFO != null) {
      final Set<Long> setTradeIds = new HashSet<Long>();
      for (final String id : arrayTradesIdFO) {
        if (id.matches(EmirSnapshotReduxConstants.REGEX_ONLY_NUMBERS)) {
          try {
            setTradeIds.add(Long.parseLong(id));
          } catch (final Exception e) {
            Log.error(this, "Trade Id exceeds limits.", e);
          }
        }
      }
      tradeIds.addAll(setTradeIds);
    }

    return tradeIds;
  }

  private void addRelatedTradeIds(final List<Long> tradeIdsFO, JDate valDate) {
    final Set<Long> tradeIdsRelated = new HashSet<Long>();
    final List<Trade> tradesFO = util.getTrades(tradeIdsFO, valDate);
    for (final Trade trade : tradesFO) {
      final List<String> extRef = new ArrayList<String>();

      final String transferTo = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_TO);
      if(!Util.isEmpty(transferTo)) {
        final TradeArray transerToArray = util.getTradeByKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRADE_ID, transferTo, false);
        if (!transerToArray.isEmpty()
                && transerToArray.size() > 0) {
          final Trade trdTransferTo = transerToArray.get(0);
          if (trdTransferTo != null) {
            tradeIdsRelated.add(trdTransferTo.getLongId());
          }
        }
      }

      final String transferFrom = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_FROM);
      if(!Util.isEmpty(transferFrom)) {
        final TradeArray transerFromArray = util.getTradeByKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRADE_ID, transferFrom, true);
        if (!transerFromArray.isEmpty()
                && transerFromArray.size() > 0) {
          final Trade trdTransferFrom = transerFromArray.get(0);
          if (trdTransferFrom != null) {
            tradeIdsRelated.add(trdTransferFrom.getLongId());
          }
        }
      }

    }

    for (final long id : tradeIdsRelated) {
      if(!tradeIdsFO.contains(id)) {
        addToTradeIdsRelated(id);
        tradeIdsFO.add(id);
      }
    }
  }


  /**
   * Get EmirReport Messages depending on report type.
   *
   * @param auditByTradeOnDate
   * @param auditByTrade
   * @param tradesWithVersions
   * @param valDate
   * @param reportTypeFO
   * @return
   */
  private List<EmirReport> getEmirReport(
          final Map<Long, List<AuditValue>> auditByTradeOnDate,
          final Map<Long, List<AuditValue>> auditByTrade, final Map<Long, Map<Integer, Trade>> tradesWithVersions,
          final JDate valDate, final String reportTypeFO) {

    List<EmirReport> emirReport = new ArrayList<EmirReport>();

    if (EmirSnapshotReportType.INDEPENDENT.name().equals(reportTypeFO)) {
      emirReport = getEmirReportList(auditByTradeOnDate, auditByTrade,
          tradesWithVersions, valDate,
          EmirSnapshotReportType.INDEPENDENT);
    } else if (EmirSnapshotReportType.DELEGATE.name().equals(reportTypeFO)) {
      emirReport = getEmirReportList(auditByTradeOnDate, auditByTrade,
          tradesWithVersions, valDate,
          EmirSnapshotReportType.DELEGATE);
    } else if (EmirSnapshotReportType.BOTH.name().equals(reportTypeFO)) {

      final List<EmirReport> emirReportIndep = getEmirReportList(auditByTradeOnDate,
          auditByTrade, tradesWithVersions, valDate,
          EmirSnapshotReportType.INDEPENDENT);

      final List<EmirReport> emirReportDeleg = getEmirReportList(auditByTradeOnDate,
          auditByTrade, tradesWithVersions, valDate,
          EmirSnapshotReportType.DELEGATE);

      emirReport.addAll(emirReportDeleg);
      emirReport.addAll(emirReportIndep);

    }

    return emirReport;
  }

  /**
   * Get the EmirReport Messages using a specific report type.
   *
   * @param auditByTradeOnDate
   * @param auditByTrade
   * @param tradesWithVersions
   * @param valDate
   * @return
   */
  private List<EmirReport> getEmirReportList(
          final Map<Long, List<AuditValue>> auditByTradeOnDate,
          final Map<Long, List<AuditValue>> auditByTrade, final Map<Long, Map<Integer, Trade>> tradesWithVersions,
          final JDate valDate, final EmirSnapshotReportType emirReportType) {

    final List<Trade> tradesEmir = EmirSnapshotReduxProcessor.getInstance()
        .process(auditByTradeOnDate, auditByTrade, tradesWithVersions,
            valDate, emirReportType);

    final List<EmirReport> emirReport = new ArrayList<EmirReport>();
    for (final Trade trade : tradesEmir) {

      // Skip Related Trade Ids added
      //if (!skipRelatedTradeId(trade.getLongId())) {
        SantEmirSnapshotReportItem item = null;

        // New field builders
        final EmirSnapshotReduxReportLogic logic = new EmirSnapshotReduxReportLogic(emirReportType, trade, valDate);
        item = logic.fillItem();

        final List<EmirReport> emirMessage = util
            .createEmirReportMessage(trade, item,
                getValuationDatetime());

        emirReport.addAll(emirMessage);
      }
    //}

    return emirReport;

  }

  private boolean skipRelatedTradeId(long tradeEmirId) {
    final Set<Long> relatedTradeIds = getTradeIdsRelated();
    return (!relatedTradeIds.isEmpty() && relatedTradeIds.contains(tradeEmirId));
  }

}
