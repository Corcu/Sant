package calypsox.tk.util.emir;

import calypsox.tk.core.KeywordConstantsUtil;
import com.calypso.tk.core.*;

import java.util.*;

public class EmirSnapshotReduxBookingUtil {
  private static EmirSnapshotReduxBookingUtil instance = null;

  private EmirSnapshotReduxBookingUtil() {
    // do nothing
  }

  public static EmirSnapshotReduxBookingUtil getInstance() {
    if (instance == null) {
      instance = new EmirSnapshotReduxBookingUtil();
    }

    return instance;
  }

  public void processBooking(List<Trade> emirMessages, Trade trade,
                             Map<String, Long> tradeIdByMurexTradeID,
                             Map<Long, Map<Integer, Trade>> tradesWithVersions,
                             SnapshotStatus snapshotStatus, JDate valDate) {
    final EmirSnapshotReduxProcessor processor = EmirSnapshotReduxProcessor
            .getInstance();
    final EmirSnapshotReduxUtil util = EmirSnapshotReduxUtil.getInstance();


    // Check the EMIR actions
    final String kwActionType = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION_TYPE);
    final String kwLifecycleevent = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT);
    final String kwTransType = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE);
    if(!Util.isEmpty(kwActionType) && !Util.isEmpty(kwLifecycleevent) && !Util.isEmpty(kwTransType)) {
      processor.addEmirActionsMessage(emirMessages, trade, kwActionType, kwLifecycleevent, TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
    }
    else {

      if (isCancelReissue(trade))  {
        processCancelReissueOnBookedTrade(trade, processor, util, emirMessages, tradeIdByMurexTradeID,
                tradesWithVersions, snapshotStatus, valDate);
      } else if (isPortfolioAssignment(trade))  {
        processPortfolioAssignment(trade, processor, util, emirMessages, tradeIdByMurexTradeID,
                tradesWithVersions, snapshotStatus, valDate);
      } else if (isTermination(trade))  {
        processTermination(trade, processor, util, emirMessages, tradeIdByMurexTradeID,
                tradesWithVersions, snapshotStatus, valDate);
      } else if (isCounterpartyAmendment(trade))  {
        processCounterPartyAmendment(trade, processor, util, emirMessages, tradeIdByMurexTradeID,
                tradesWithVersions, snapshotStatus, valDate);
      } else {
        if (!snapshotStatus.isRestructured()
              && !snapshotStatus.isRateAmendment()
                && !snapshotStatus.isSharesModification()
                && !snapshotStatus.isCounterpartyAmendment()
                && !snapshotStatus.isPortfolioAssignment()) {

         processor.addEmirMessage(emirMessages, trade, ActionTypeValue.N,
                LifeCycleEventValue.Trade, TransactionTypeValue.TRD,
                ActionValue.NEW, snapshotStatus);

        }
      }
    }
  }

  private boolean isCancelReissue(Trade trade) {
    return EmirSnapshotReduxConstants.MX_EVENT_ICANCEL_REISSUE
            .equals(trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MX_LAST_EVENT));
  }

  private boolean isPortfolioAssignment(Trade trade) {
    return EmirSnapshotReduxConstants.MX_EVENT_IPORTFOLIO_ASSIGNMENT
            .equals(trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MX_LAST_EVENT));
  }

  private boolean isTermination(Trade trade) {
    return EmirSnapshotReduxConstants.MX_EVENT_EARLY_TERMINATION_TOTAL_RETURN
            .equals(trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MX_LAST_EVENT));
  }


  private boolean isCounterpartyAmendment(Trade trade) {
    return EmirSnapshotReduxConstants.MX_EVENT_ICOUNTERPART_AMENDMENT
            .equals(trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MX_LAST_EVENT));
  }

  private boolean isRestructure(Trade trade) {
    final String mxLastEvent = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MX_LAST_EVENT);

    return (!Util.isEmpty(mxLastEvent) && mxLastEvent.equals(EmirSnapshotReduxConstants.MX_EVENT_IRESTRUCTURE));

  }

  private boolean isReissue(Trade trade) {
    return hasKeyword(trade,
            EmirSnapshotReduxConstants.MX_EVENT_ICANCEL_REISSUE);
  }

  private boolean isNovation(Trade trade) {
    return hasKeyword(trade, KeywordConstantsUtil.KEYWORD_NOVATION_FROM);
  }

  private boolean hasKeyword(Trade trade, String keywordName) {
    return !Util.isEmpty(trade.getKeywordValue(keywordName));
  }

  public Trade getPreviousTradeReportable(String externalReference,
                                          Map<String, Long> tradeIdByExternalReference,
                                          Map<Long, Map<Integer, Trade>> allTrades) {
    Trade trade = null;

    final Long tradeId = tradeIdByExternalReference.get(externalReference);
    if (tradeId != null) {
      final Map<Integer, Trade> allTradeVersions = allTrades.get(tradeId);
      if (allTradeVersions != null) {
        final int maxVersion = getMax(allTradeVersions.keySet());
        trade = allTradeVersions.get(maxVersion);
      }
    }

    // If returned null it means that the previous trade is not reportable
    if (trade != null) { // D
      return trade;
    } else { // D - 1
      trade = EmirSnapshotReduxUtil.getInstance().getTradeByExternalReference(externalReference);

      if (trade == null) {
        return null;
      }

      // Check previousTrade is Reportable
      return (EmirSnapshotReduxUtil.getInstance()
              .isTradeReportable(trade)) ? trade : null;
    }
  }

  private Integer getMax(Collection<Integer> collection) {
    final Iterator<Integer> iInteger = collection.iterator();
    Integer maxValue = iInteger.next();
    while (iInteger.hasNext()) {
      final Integer nextInteger = iInteger.next();
      if (nextInteger > maxValue) {
        maxValue = nextInteger;
      }
    }

    return maxValue;
  }

  private boolean isMurex3(Trade trade) {
    /*
    return trade.getExternalReference().startsWith(
        KeywordConstantsUtil.PREFIX_MUREX_3_1);

     */
    return false;
  }


  private boolean isBookedOnValDate(final long tradeId, final JDate valDate) {
    boolean isBookedOnValDate = false;

    final List<Long> id = new ArrayList<Long>();
    id.add(tradeId);
    final List<AuditValue> tradeAudit = EmirSnapshotReduxUtil.getInstance()
            .getTradeAudits(id);

    final Map<Long, List<AuditValue>> auditByTrade = new HashMap<Long, List<AuditValue>>();
    auditByTrade.put(tradeId, tradeAudit);

    final Map<Long, List<AuditValue>> auditOnDate = EmirSnapshotReduxUtil.getInstance()
            .filterAuditOnDate(valDate, auditByTrade);

    final Iterator<AuditValue> iAudit = auditOnDate.get(tradeId).iterator();
    while (!isBookedOnValDate && iAudit.hasNext()) {
      final AuditValue auditValue = iAudit.next();
      if (Trade.CREATE.equals(auditValue.getFieldName())) {
        isBookedOnValDate = true;
      }
    }

    return isBookedOnValDate;
  }

  private void processCounterPartyAmendment(Trade trade,
                                  EmirSnapshotReduxProcessor processor, EmirSnapshotReduxUtil util,
                                  List<Trade> emirMessages, Map<String, Long> tradeIdByMurexTradeID,
                                  Map<Long, Map<Integer, Trade>> tradesWithVersions,
                                  SnapshotStatus snapshotStatus, JDate valDate) {

    // Get Previous Trade (Canceled)
    Trade previousTrade = null;
    final String cancelReissueFrom = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_FROM);

    boolean isPreviousTradeReportable = false;

    previousTrade = getPreviousTradeReportable(cancelReissueFrom, tradeIdByMurexTradeID, tradesWithVersions);
    if (previousTrade != null) {
      isPreviousTradeReportable = true;
    } else {
      isPreviousTradeReportable = false;
      previousTrade = EmirSnapshotReduxUtil.getInstance().getTradeByMurexTradeId(cancelReissueFrom, true);
    }

    boolean previousTradeBookToday = false;

    if (previousTrade != null) {
      previousTradeBookToday = isBookedOnValDate(previousTrade.getLongId(), valDate);
    }

      boolean isleiChange = util.isLeiChange(previousTrade, trade);

      // Si Counterparty Amendment a contrapartida nueva sin cambio de LEI en d:
      if (isPreviousTradeReportable) {
            if (previousTradeBookToday) {
              processor.addEmirMessage(emirMessages, previousTrade,
                      ActionTypeValue.N, LifeCycleEventValue.Trade,
                      TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
            }
            //BAJA Antihua en D
            processor.addEmirMessage(emirMessages, previousTrade, ActionTypeValue.C, LifeCycleEventValue.Novation, TransactionTypeValue.EXT,
                    ActionValue.NEW, snapshotStatus);
            // ALTA a Nueva en D
            processor.addEmirMessage(emirMessages, trade,
                    ActionTypeValue.N, LifeCycleEventValue.NovationTrade,
                    TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
      } else  if (isleiChange) {
        // Operacion antigua no reportable - Alta a la nueva
        processor.addEmirMessage(emirMessages, trade,
                ActionTypeValue.N, LifeCycleEventValue.Trade,
                TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
      }
  }

  private void processCancelReissueOnBookedTrade(Trade trade,
                                                 EmirSnapshotReduxProcessor processor, EmirSnapshotReduxUtil util,
                                                 List<Trade> emirMessages,
                                                 Map<String, Long> tradeIdByMurexTradeID,
                                                 Map<Long, Map<Integer, Trade>> tradesWithVersions,
                                                 SnapshotStatus snapshotStatus, JDate valDate) {

    // Get Previous Trade (Canceled)
    Trade previousTrade = null;
    final String cancelReissueFrom = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_FROM);

    boolean isPreviousTradeReportable = false;

    previousTrade = getPreviousTradeReportable(cancelReissueFrom, tradeIdByMurexTradeID, tradesWithVersions);
    if (previousTrade != null) {
      isPreviousTradeReportable = true;
    } else {
      isPreviousTradeReportable = false;
      previousTrade = EmirSnapshotReduxUtil.getInstance().getTradeByMurexTradeId(cancelReissueFrom, true);
    }

    boolean previousTradeBookToday = false;

    if (previousTrade != null) {
      previousTradeBookToday = isBookedOnValDate(previousTrade.getLongId(), valDate);
    }

    if (previousTradeBookToday) {
      // N/Trade/Trade
      processor.addEmirMessage(emirMessages, trade,
              ActionTypeValue.N, LifeCycleEventValue.Trade,
              TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
    } else {
      if (util.isChangeInCounterparty(previousTrade, trade)) {
        // DDR v25
        if (util.isChangeLeiAttribute(previousTrade, trade)) {
          // N/Trade/Trade
          processor.addEmirMessage(emirMessages, trade,
                  ActionTypeValue.N, LifeCycleEventValue.Trade,
                  TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);

        } else {
          // R/Amendment/Trade
          processor.addEmirMessage(emirMessages, trade, ActionTypeValue.R, LifeCycleEventValue.Amendment,
                  TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);

          processor.removeEmirMessageCanceled(emirMessages, previousTrade);
        }
        // DDR v25 - End

      } else {
        if (isPreviousTradeReportable) {
          // R/Amendment/Trade
          processor.addEmirMessage(emirMessages, trade,
                  ActionTypeValue.R, LifeCycleEventValue.Amendment,
                  TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);

          processor.removeEmirMessageCanceled(emirMessages, previousTrade);

        } else {
          // N/Trade/Trade
          processor.addEmirMessage(emirMessages, trade,
                  ActionTypeValue.N, LifeCycleEventValue.Trade,
                  TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
        }
      }
    }
  }

  private void processPortfolioAssignment(Trade trade,
                                          EmirSnapshotReduxProcessor processor, EmirSnapshotReduxUtil util,
                                          List<Trade> emirMessages,
                                          Map<String, Long> tradeIdByMurexTradeID,
                                          Map<Long, Map<Integer, Trade>> tradesWithVersions,
                                          SnapshotStatus snapshotStatus, JDate valDate) {

    // Get Previous Trade (Canceled)
    Trade previousTrade = null;
    final String cancelReissueFrom = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_FROM);

    boolean isPreviousTradeReportable = false;

    previousTrade = getPreviousTradeReportable(cancelReissueFrom, tradeIdByMurexTradeID, tradesWithVersions);
    if (previousTrade != null) {
      isPreviousTradeReportable = true;
    } else {
      isPreviousTradeReportable = false;
      previousTrade = EmirSnapshotReduxUtil.getInstance().getTradeByMurexTradeId(cancelReissueFrom, true);
    }

    boolean previousTradeBookToday = false;

    if (previousTrade != null) {
      previousTradeBookToday = isBookedOnValDate(previousTrade.getLongId(), valDate);
    }

    if (previousTradeBookToday) {
            //    && !previousTrade.getStatus().equals(Status.S_TERMINATED)) {
      // N/Trade/Trade
      processor.addEmirMessage(emirMessages, trade,
              ActionTypeValue.N, LifeCycleEventValue.Trade,
              TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
    } else {
      if (isPreviousTradeReportable) {
        // R/Amendment/Trade
        processor.addEmirMessage(emirMessages, trade,
                ActionTypeValue.R, LifeCycleEventValue.Amendment,
                TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);

        processor.removeEmirMessageCanceled(emirMessages, previousTrade);
        processor.removeEmirMessageTerminated(emirMessages, previousTrade);


      } else {
        // N/Trade/Trade
        processor.addEmirMessage(emirMessages, trade,
                ActionTypeValue.N, LifeCycleEventValue.Trade,
                TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
      }
    }
  }


  private void processTermination (Trade trade,
                               EmirSnapshotReduxProcessor processor, EmirSnapshotReduxUtil util,
                               List<Trade> emirMessages,
                               Map<String, Long> tradeIdByMurexTradeID,
                               Map<Long, Map<Integer, Trade>> tradesWithVersions,
                               SnapshotStatus snapshotStatus, JDate valDate) {

    // Get Previous Trade (Canceled)
    Trade previousTrade = null;
    final String cancelReissueFrom = trade
            .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_FROM);


    previousTrade = getPreviousTradeReportable(cancelReissueFrom, tradeIdByMurexTradeID, tradesWithVersions);
    if (previousTrade == null) {
      previousTrade = EmirSnapshotReduxUtil.getInstance().getTradeByMurexTradeId(cancelReissueFrom, true);
    }

    boolean previousTradeBookToday = false;

    if (previousTrade != null) {
      previousTradeBookToday = isBookedOnValDate(previousTrade.getLongId(), valDate);
    }

    if (previousTradeBookToday) {
      // N/Trade/Trade

      processor.addEmirMessage(emirMessages, trade,
              ActionTypeValue.N, LifeCycleEventValue.Trade,
              TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);

      // C/Termination/Exit - Early Termination
      processor.addEmirMessage(emirMessages, trade, ActionTypeValue.C,
              LifeCycleEventValue.Termination, TransactionTypeValue.EXT,
              ActionValue.NEW, snapshotStatus);


    } else {
      /*
      // C/Termination/Exit - Early Termination
      processor.addEmirMessage(emirMessages, trade, ActionTypeValue.C,
              LifeCycleEventValue.Termination, TransactionTypeValue.EXT,
              ActionValue.NEW, snapshotStatus);
      */
    }
  }
}
