package calypsox.tk.util.emir;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.report.emir.field.EmirFieldBuilderUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.FXSwap;
import com.calypso.tk.product.SwapLeg;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class EmirSnapshotReduxProcessor {

  private final EmirSnapshotReduxUtil util = EmirSnapshotReduxUtil.getInstance();

  private static EmirSnapshotReduxProcessor instance = null;

  private EmirSnapshotReduxProcessor() {
  }

  public static EmirSnapshotReduxProcessor getInstance() {
    if (instance == null) {
      instance = new EmirSnapshotReduxProcessor();
    }
    return instance;
  }

  public List<Trade> process(
          Map<Long, List<AuditValue>> auditByTradeOnDate, Map<Long, List<AuditValue>> auditByTrade,
          Map<Long, Map<Integer, Trade>> trades, JDate valDate,
          EmirSnapshotReportType reportType) {
    final List<Trade> emirMessages = new ArrayList<Trade>();

    for (final Entry<Long, List<AuditValue>> entry : auditByTradeOnDate
        .entrySet()) {
      final long tradeId = entry.getKey();
      final List<AuditValue> tradeAudit = entry.getValue();

      if(tradeAudit == null || tradeAudit.isEmpty()) { // No related trades D-1
        continue;
      }

      // Check the last status of the trade at valDate
      final int lastVersionOnDate = tradeAudit.get(tradeAudit.size() - 1).getVersion();
      final Trade tradeLastVersionOnDate = trades.get(tradeId).get(lastVersionOnDate);
      if(!isAcceptedStatus(tradeLastVersionOnDate)) {
        continue;
      }

      final SnapshotStatus snapshotStatus = new SnapshotStatus();
      // Stores in which versions we have generated a matching message.
      final Map<String, Boolean> matchingVersions = new HashMap<String, Boolean>();

      boolean hasDelegateReportabilityChange = hasDelegateReportabilityChange(valDate, reportType, tradeLastVersionOnDate, tradeAudit, lastVersionOnDate, trades);
      snapshotStatus.setDelegateReportabilityChanged(hasDelegateReportabilityChange);

      for (final AuditValue auditValue : tradeAudit) {
        final int version = auditValue.getVersion();
        final Trade trade = trades.get(tradeId).get(version);

        // Check if this trade should be reported in the Delegate report
        if ( /*isAcceptedStatus(trade) &&*/ (reportType == EmirSnapshotReportType.BOTH
            || reportType == EmirSnapshotReportType.INDEPENDENT
            || (reportType == EmirSnapshotReportType.DELEGATE
                  && isFullDelegation(trade)
                  && ( isBooking(auditValue) || isCancelation(auditValue)))
            || snapshotStatus.isDelegateReportabilityChanged())) {

           if (!isMatured(trade, valDate)) {
              final LegType legType = EmirSnapshotReduxUtil
                  .getInstance().getLegType(trade);

              if (isBooking(auditValue)) {
                updateBookingStatus(snapshotStatus, legType,
                    version, trade, auditByTrade,
                    valDate);
              } else if (isChangeStatus(auditValue)) {


                if (isCancelation(auditValue)) {
                  updateCancelationStatus(snapshotStatus,
                      legType, version);

                } else if (isTermination(auditValue)) {
                  updateTerminationStatus(snapshotStatus,
                          legType, version);

                } else if (isUndo(auditValue)) {
                  updateUndoAction(snapshotStatus,version);
                } else if (isUndoTerm(auditValue)) {
                  updateUndoTerm(snapshotStatus,version);
                }

              } else if (isUtiChange(auditValue, legType)) {
                processUtiChange(emirMessages, trade,
                    snapshotStatus, trades, legType,
                    tradeId, version, auditByTrade, valDate);
              } else if (isUtiTemporalChange(auditValue, legType)) {
                updateUtiTemporalChange(snapshotStatus,
                    legType, version);
              } else if (isMatching(auditValue, trade, trades,
                  valDate)) {
                final String matchingVersionsKey = legType.name()
                    + "-" + version;
                if (!matchingVersions
                    .containsKey(matchingVersionsKey)) {
                  matchingVersions.put(matchingVersionsKey,
                      Boolean.TRUE);
                  processMatching(emirMessages, trade,
                      snapshotStatus, legType, version,
                      auditByTrade, valDate);
                }
              } else if (isTradingVenueUpdate(auditValue)) {

                processTradingVenueUpdate(snapshotStatus,
                        trade);
              } else if (isEmirAction(auditValue)) {

                snapshotStatus.setBookedVersion(auditValue.getVersion());
                snapshotStatus.setBookedFarVersion(auditValue.getVersion());
                // DDR v25

              } else if (isLeiChange(auditValue)) {
                updateLeiChange(trade, legType, auditValue, version, snapshotStatus);

              } else if (isRateAmendment(auditValue, legType)) {

                snapshotStatus.setRateAmendment(true);
                snapshotStatus.setRateAmendmentVersion(version);

              } else if (isPortfolioModification(auditValue)) {
                snapshotStatus.setPortfolioModificationVersion(auditValue.getVersion());
                snapshotStatus.setPortfolioModification(true);

              } else if (isPortfolioAssignment(auditValue)) {
                // Status also change to TERMINATED BUT we do not process as Termination
                snapshotStatus.setPortfolioAssignmentVersion(auditValue.getVersion());
                snapshotStatus.setPortfolioAssignment(true);

              } else if (isAdditionalFlowAmendment(auditValue)) {
                 snapshotStatus.setAdditionalFlowAmendment(true);
                 snapshotStatus.setAdditionalFlowAmendmentVersion(auditValue.getVersion());

              } else if (isCancelReissue(auditValue)) {
                 updateCancelReissue(snapshotStatus,
                        legType, version);
              } else if (isRestructured(auditValue)) {
                updateRestructured(snapshotStatus,
                        legType, version);
              } else if (isSharesModification(auditValue)) {
                updateSharesModification(snapshotStatus,
                        legType, version);
              } else if (isCounterpartyAmendment(auditValue)) {
                updateCounterpartyAmendment(snapshotStatus,
                        legType, version);
              } else if (isMaturityExtension(auditValue)) {
                updateMaturityExtension(snapshotStatus,
                        legType, version);
              } else if (isModifyUserField(auditValue)) {
                updateModifyUserField(snapshotStatus,
                        legType, version);
              } else if (isUndoTerm((auditValue))) {
                updateUndoTerm(snapshotStatus,version);
              } else if (isAmortizationChange(trade, auditValue)) {
                updateAmortizationChange(snapshotStatus,version);
              }

              // DDR v25 - End
            }
          }
        //}
      }

      final Map<String, Long> tradeIdsByMurexTradeID = EmirSnapshotReduxUtil
              .getInstance().getTradeIdsByMurexTradeID(trades);

      // SnapshotStatus control
      processSnapshotStatus(snapshotStatus);

      if (snapshotStatus.isBooked()) {
        final Trade trade = trades.get(tradeId).get(
            snapshotStatus.getBookedVersion());
        final Trade tradeLeg = getTradeLeg(trade, LegType.NEAR);

        setConfirmationDateTime(tradeLeg, snapshotStatus, LegType.NEAR,
            auditByTrade.get(tradeLeg.getLongId()), valDate);
        EmirSnapshotReduxBookingUtil.getInstance().processBooking(
            emirMessages, tradeLeg, tradeIdsByMurexTradeID,
            trades, snapshotStatus, valDate);
      }
      if (snapshotStatus.isCanceled()
              && !snapshotStatus.isCancelReissue()
              && !snapshotStatus.isRestructured()) {

        final Trade trade = trades.get(tradeId).get(
            snapshotStatus.getCanceledVersion());
        final Trade tradeLeg = getTradeLeg(trade, LegType.NEAR);

        setConfirmationDateTime(tradeLeg, snapshotStatus, LegType.NEAR,
            auditByTrade.get(tradeLeg.getLongId()), valDate);
        processCancelation(emirMessages, tradeLeg,
                tradeIdsByMurexTradeID, trades, snapshotStatus);
      }

      if (snapshotStatus.isTerminated()) {

        /* special check para cpty Ament */

        final Trade trade = trades.get(tradeId).get(
            snapshotStatus.getTerminatedVersion());
        final Trade tradeLeg = getTradeLeg(trade, LegType.NEAR);

        if (!tradeLeg.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MX_LAST_EVENT).equalsIgnoreCase(EmirSnapshotReduxConstants.MX_EVENT_ICOUNTERPART_AMENDMENT))  {
          setConfirmationDateTime(tradeLeg, snapshotStatus, LegType.NEAR,
                  auditByTrade.get(tradeLeg.getLongId()), valDate);
          processTermination(emirMessages, tradeLeg,
                  tradeIdsByMurexTradeID, trades, snapshotStatus);
        }
      }

      // DDR v25
      if (snapshotStatus.isLeiChanged()) {
        final int version = snapshotStatus.getLeiChangedVersion();
        final Trade trade = trades.get(tradeId).get(version);
        final Trade tradeLeg = getTradeLeg(trade, LegType.NEAR);

        setConfirmationDateTime(tradeLeg, snapshotStatus, LegType.NEAR, auditByTrade.get(tradeLeg.getLongId()), valDate);
        processLeiChanged(emirMessages, tradeLeg, snapshotStatus);
      }

      if (snapshotStatus.isRateAmendment()) {
        final int version = snapshotStatus.getRateAmendmentVersion();
        final Trade trade = trades.get(tradeId).get(version);
        processRateAmendment(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isPortfolioAssignment()) {
        final int version = snapshotStatus.getPortfolioAssignmentVersion();
        final Trade trade = trades.get(tradeId).get(version);
        processPortfolioAssignment(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isPortfolioModification()) {
        final int version = snapshotStatus.getPortfolioModificationVersion();
        final Trade trade = trades.get(tradeId).get(version);
        processPortfolioModification(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isCancelReissue()) {
        final int version = snapshotStatus.getCancelReissueVersion();
        final Trade trade = trades.get(tradeId).get(version);
        processCancelReissue(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isRestructured()) {
        final int version = snapshotStatus.getRestructuredVersion();
        final Trade trade = trades.get(tradeId).get(version);
        processRestructured(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }


      if (snapshotStatus.isSharesModification()) {
        final int version = snapshotStatus.getSharesModificationVersion();
        final Trade trade = trades.get(tradeId).get(version);

        processSharesModification(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isCounterpartyAmendment()) {
        final int version = snapshotStatus.getCptyAmendVersion();
        final Trade trade = trades.get(tradeId).get(version);

        processCptyAmendment(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isUndoTerm()) {
        final int version = snapshotStatus.getUndoTermVersion();
        final Trade trade = trades.get(tradeId).get(version);

        processUndoTerm(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      if (snapshotStatus.isAmortizationChange()) {
        final int version = snapshotStatus.getAmortizationChangeVersion();
        final Trade trade = trades.get(tradeId).get(version);

        processAmortizationChange(emirMessages, trade, snapshotStatus, trades, tradeId, version, auditByTrade, valDate);
      }

      // DDR v25 - End
    }

    return emirMessages;
  }


  private boolean hasDelegateReportabilityChange(JDate valDate, EmirSnapshotReportType reportType, Trade trade, List<AuditValue> tradeAudit, int lastVersionOnDate, Map<Long, Map<Integer, Trade>> trades) {
    if (EmirSnapshotReportType.DELEGATE.equals(reportType)) {

      Boolean currentIsFullDeleg = isFullDelegation(trade);

      // Trade is not reportable now
        if (lastVersionOnDate > 0 ) {
          int cptyChangeVersion = -1;
          for (AuditValue av : tradeAudit) {
            if (av.getField().getName().equals("_counterPartyId")) {
              cptyChangeVersion = av.getVersion();
            }
          }
          if (cptyChangeVersion > 0) {
            final Trade previousTradeVersion = trades.get(trade.getLongId()).get(cptyChangeVersion-1);
            if (previousTradeVersion != null) {
                Boolean previousIsFullDeleg =  isFullDelegation(previousTradeVersion);
                return !(previousIsFullDeleg.equals(currentIsFullDeleg));
            }
          }
        }
    }
    return false;
  }

  private boolean isPortfolioModification(AuditValue auditValue) {

      final String fieldName = auditValue.getField().getName();
      final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IPORTFOLIO_MODIFICATION));
  }

  private boolean isPortfolioAssignment(AuditValue auditValue) {

    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IPORTFOLIO_ASSIGNMENT));
  }

  private boolean isAdditionalFlowAmendment(AuditValue auditValue) {

    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IADDITIONAL_FLOW_AMENDMENT));
  }

  private boolean isBooking(AuditValue auditValue) {
    return auditValue.getVersion() == 0;
  }

  private boolean isUtiChange(AuditValue auditValue, LegType legType) {
    boolean utiChange = false;

    final String fieldName = auditValue.getFieldName();
    if (!Util.isEmpty(fieldName)) {
      if (legType == LegType.FAR) {
        utiChange = fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_UTI_FAR);
      } else {
        utiChange = fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_UTI)
            || fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_UTI_NEAR)
            || fieldName
                .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_UTI_REFERENCE);
      }
    }

    return utiChange;
  }

  private boolean isUtiTemporalChange(AuditValue auditValue, LegType legType) {
    boolean utiTempChange = false;

    final String fieldName = auditValue.getFieldName();
    if (!Util.isEmpty(fieldName)) {
      if (legType == LegType.FAR) {
        utiTempChange = fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_TEMP_UTI_FAR);
      } else {
        utiTempChange = fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_TEMP_UTI)
            || fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_TEMP_UTI_NEAR);
      }
    }

    return utiTempChange;
  }

  private boolean isMatching(AuditValue auditValue, Trade trade,
                             //TODO - Check MAtching Criteria
                             Map<Long, Map<Integer, Trade>> trades, JDate valDate) {
      boolean matching = false;

      final String fieldName = auditValue.getField().getName();
      final String newValue = auditValue.getField().getNewValue();

      final LegType legType = util.getLegType(trade);
      if (fieldName
          .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MATCHING_STATUS)
          && KeywordConstantsUtil.TRADE_KEYWORD_STATUS_MATCHED
          .equals(newValue)) {
        matching = true;
      }

    return matching;
  }

  private boolean isChangeStatus(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();
    final String oldValue = auditValue.getField().getOldValue();
    return EmirSnapshotReduxConstants.AUDIT_STATUS.equals(fieldName)
        && ((!Util.isEmpty(oldValue) && !oldValue.equals(newValue)) || (Util
            .isEmpty(oldValue) && !Util.isEmpty(newValue)));
  }

  private boolean isCancelation(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return EmirSnapshotReduxConstants.AUDIT_STATUS.equals(fieldName)
        && Status.CANCELED.equals(newValue);
  }

   boolean isCancelReissue(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_ICANCEL_REISSUE));

  }

  private boolean isRestructured(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IRESTRUCTURE));

  }

  private boolean isSharesModification(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_ISHARES_MODIFICATION));

  }

  private boolean isTermination(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return EmirSnapshotReduxConstants.AUDIT_STATUS.equals(fieldName)
        && Status.TERMINATED.equals(newValue);
  }

  private boolean isUndo(AuditValue auditValue) {
    final String action = auditValue.getAction().toString();
    return action.equalsIgnoreCase("UNDO");
  }

  private boolean isUndoTerm(AuditValue auditValue) {
    final String action = auditValue.getAction().toString();
    return action.equalsIgnoreCase("UNDO_TERMINATE");
  }

  private boolean isCounterpartyAmendment(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    &&
                    newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_ICOUNTERPART_AMENDMENT));

  }

  private boolean isAmortizationChange(Trade trade, AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String action = auditValue.getAction().toString();

    if (Action.S_AMEND.equalsIgnoreCase(action)
            && EmirSnapshotReduxConstants.AUDIT_FIELDS_AMORTIZATION_SCHEDULE.contains(fieldName)) {

        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
         if (pLeg != null) {
            return  ("Schedule".equalsIgnoreCase(pLeg.getPrincipalStructure()));
         }
    }
    return false;
  }

  private boolean isMaturityExtension(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
            &&
            newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IMATURIRY_EXTENSION));

  }

  private boolean isTradingVenueUpdate(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();

    return fieldName
        .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_EMIR_IDENTIFICATION_ISIN)
        || fieldName
        .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_EMIR_MIC_CODE)
        || fieldName
        .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_EMIR_PRODUCT_TOTV);
  }

  private boolean isEmirAction(AuditValue auditValue) {
    final String action = auditValue.getAction().toString();
    if("EMIR_NEW".equalsIgnoreCase(action) || "EMIR_AMENDEMENT".equalsIgnoreCase(action) || "EMIR_NOVATION".equalsIgnoreCase(action)) {
      return true;
    }
    return false;
  }

  private boolean isRateAmendment(AuditValue auditValue, LegType legType) {

    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    && newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IRATE_AMENDMENT));

  }


  private boolean isModifyUserField(AuditValue auditValue) {
    final String fieldName = auditValue.getField().getName();
    final String newValue = auditValue.getField().getNewValue();

    return  fieldName
            .endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_MX_LAST_EVENT)
            &&
            (!Util.isEmpty(newValue)
                    && newValue.endsWith(EmirSnapshotReduxConstants.MX_EVENT_IMODIFY_UDF));
  }

  private boolean isMatured(Trade trade, JDate valDate) {
    return trade.getMaturityDate().before(valDate);
  }

  public void addEmirMessage(List<Trade> emirMessages, Trade trade,
                             ActionTypeValue actionTypeValue,
                             LifeCycleEventValue lifeCycleEventValue,
                             TransactionTypeValue transactionTypeValue,
                             ActionValue actionValue,
                             SnapshotStatus snapshotStatus) {

    final Trade clonedTrade = util.cloneTrade(
        trade);
    addTradeKeywords(clonedTrade, actionTypeValue, lifeCycleEventValue,
        transactionTypeValue, actionValue);
    for (final Entry<String, String> keywordEntry : snapshotStatus.getKeywordsTradingVenue()
        .entrySet()) {
      final String keywordName = keywordEntry.getKey();
      final String keywordValue = keywordEntry.getValue();
      clonedTrade.addKeyword(keywordName, keywordValue);
    }
    emirMessages.add(clonedTrade);
  }

  public void addEmirActionsMessage(List<Trade> emirMessages, Trade trade,
                                    String actionType, String lifeCycleEvent, TransactionTypeValue transactionType,
                                    ActionValue actionValue, SnapshotStatus snapshotStatus) {

    final Trade clonedTrade = EmirSnapshotReduxUtil.getInstance().cloneTrade(trade);

    clonedTrade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION, actionValue.name());
    clonedTrade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION_TYPE, actionType);
    clonedTrade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT, lifeCycleEvent);
    clonedTrade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE, transactionType.name());

    for (final Entry<String, String> keywordEntry : snapshotStatus.getKeywordsTradingVenue()
        .entrySet()) {
      final String keywordName = keywordEntry.getKey();
      final String keywordValue = keywordEntry.getValue();
      clonedTrade.addKeyword(keywordName, keywordValue);
    }
    emirMessages.add(clonedTrade);
  }

  private void addTradeKeywords(Trade trade, ActionTypeValue actionTypeValue,
                                LifeCycleEventValue lifecycleEventValue,
                                TransactionTypeValue transactionTypeValue, ActionValue actionValue) {
    final String action = actionValue.name();
    final String actionType = actionTypeValue.name();
    final String lifecycleEvent = lifecycleEventValue.name();
    final String transactionType = transactionTypeValue.name();

    trade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION,
        action);
    trade.addKeyword(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION_TYPE,
        actionType);
    trade.addKeyword(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT,
        lifecycleEvent);
    trade.addKeyword(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE,
        transactionType);

    final String logMessage = String
        .format("Trade Id: %d, Version: %d. Action = \"%s\", ActionType = \"%s\", LifeCycleEvent = \"%s\", Transtype = \"%s\"",
            trade.getLongId(), trade.getVersion(), action, actionType,
            lifecycleEvent, transactionType);
    Log.debug(this, logMessage);
  }

  private void updateBookingStatus(SnapshotStatus snapshotStatus,
                                   LegType legType, int version, Trade tradeLeg,
                                   Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    // Check if it's matched and set the matching version
    if (isTradeMatched(tradeLeg, legType)) {
      setConfirmationDateTime(tradeLeg, snapshotStatus, legType,
          auditByTrade.get(tradeLeg.getLongId()), valDate);
    }

    snapshotStatus.setBooked(true);
    snapshotStatus.setBookedVersion(version);

  }

  private void updateCancelationStatus(SnapshotStatus snapshotStatus,
                                       LegType legType, int version) {
    if (legType == LegType.FAR) {
      snapshotStatus.setCanceledFar(true);
      snapshotStatus.setCanceledFarVersion(version);
    } else {
      snapshotStatus.setCanceled(true);
      snapshotStatus.setCanceledVersion(version);
    }
  }

  private void updateTerminationStatus(SnapshotStatus snapshotStatus,
                                       LegType legType, int version) {
    if (legType == LegType.FAR) {
      snapshotStatus.setTerminatedFar(true);
      snapshotStatus.setTerminatedFarVersion(version);
    } else {
      snapshotStatus.setTerminated(true);
      snapshotStatus.setTerminatedVersion(version);
    }
  }

  private void updateUndoAction(SnapshotStatus snapshotStatus, int version) {

    if (snapshotStatus.isCanceled()) {
      snapshotStatus.setUndoVersion(0);
      snapshotStatus.setUndo(false);
      snapshotStatus.setCanceledVersion(0);
      snapshotStatus.setCanceled(false);
    }
  }

  private void updateUndoTerm(SnapshotStatus snapshotStatus, int version) {
      snapshotStatus.setUndoTermVersion(version);
      snapshotStatus.setUndoTerm(true);
  }

  private void  updateAmortizationChange(SnapshotStatus snapshotStatus, int version) {
    snapshotStatus.setAmortizationChange(true);
    snapshotStatus.setAmortizationChangeVersion(version);
  }

  private void updateCancelReissue(SnapshotStatus snapshotStatus,
                                   LegType legType, int version) {
      snapshotStatus.setCancelReissue(true);
      snapshotStatus.setCancelReissueVersion(version);
  }

  private void updateRestructured(SnapshotStatus snapshotStatus,
                                        LegType legType, int version) {
    snapshotStatus.setRestructured(true);
    snapshotStatus.setRestructuredVersion(version);
  }

  private void updateSharesModification(SnapshotStatus snapshotStatus,
                                  LegType legType, int version) {
    snapshotStatus.setSharesModification(true);
    snapshotStatus.setSharesModificationVersion(version);
  }

  private void updateCounterpartyAmendment(SnapshotStatus snapshotStatus,
                                        LegType legType, int version) {
    snapshotStatus.setCptyAmendVersion(version);
    snapshotStatus.setCptyAmend(true);
  }

  private void updateMaturityExtension(SnapshotStatus snapshotStatus,
                                       LegType legType, int version) {
    snapshotStatus.setMaturityExtensionVersion(version);
    snapshotStatus.setMaturityExtension(true);
  }

  private void updateModifyUserField(SnapshotStatus snapshotStatus,
                                       LegType legType, int version) {
    if (snapshotStatus.isBooked()
            && !snapshotStatus.isCanceled()
            && !snapshotStatus.isTerminated())
      snapshotStatus.setBookedVersion(version);
  }

  private void processCancelation(List<Trade> emirMessages, Trade trade,
                                  Map<String, Long> tradeIdsByExternalReference,
                                  Map<Long, Map<Integer, Trade>> tradesWithVersion,
                                  SnapshotStatus snapshotStatus) {

    if(!isBookedToday(snapshotStatus)) {
      // E/Error/Exit
      addEmirMessage(emirMessages, trade, ActionTypeValue.E,
          LifeCycleEventValue.Error, TransactionTypeValue.EXT,
          ActionValue.CAN, snapshotStatus);
    }
  }

  private void processTermination(List<Trade> emirMessages, Trade trade,
                                  Map<String, Long> tradeIdsByExternalReference,
                                  Map<Long, Map<Integer, Trade>> trades,
                                  SnapshotStatus snapshotStatus) {

      final String trasnferTo = trade
              .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_TO);

      if (Util.isEmpty(trasnferTo)) {
          // C/Termination/Exit - Early Termination
          addEmirMessage(emirMessages, trade, ActionTypeValue.C,
                  LifeCycleEventValue.Termination, TransactionTypeValue.EXT,
                  ActionValue.NEW, snapshotStatus);
      } else {
          // C/Termination/Exit - Early Termination
          addEmirMessage(emirMessages, trade, ActionTypeValue.C,
              LifeCycleEventValue.Termination, TransactionTypeValue.EXT,
              ActionValue.NEW, snapshotStatus);
      }

  }

  private void processUtiChange(List<Trade> emirMessages, Trade trade,
                                SnapshotStatus snapshotStatus,
                                Map<Long, Map<Integer, Trade>> trades, LegType legType, long tradeId, int version,
                                Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    if (legType == LegType.FAR && snapshotStatus.isBookedFar()) {
      snapshotStatus.setBookedFarVersion(version);
    } else if (legType != LegType.FAR && snapshotStatus.isBooked()) {
      snapshotStatus.setBookedVersion(version);
    } else {

      // DDR v25
      if (legType == LegType.FAR) {
        snapshotStatus.setUtiChangedFar(true);
        snapshotStatus.setUtiChangedFarVersion(version);
      } else {
        snapshotStatus.setUtiChanged(true);
        snapshotStatus.setUtiChangedVersion(version);
      }
      // DDR v25 - End

      if (hasPreviousUti(trades, legType, tradeId, version) || hasPreviousUtiTemporal(trades, tradeId, version)) {

        // E/Error/Exit - N/Trade/Trade
        final Trade previousTrade = trades.get(tradeId).get(version - 1);
        final Trade previousLeg = getTradeLeg(previousTrade, legType);

        // As Of Date Time has to be the date of the event, not of the
        // previous change
        previousLeg.setUpdatedTime(trade.getUpdatedTime());
        setConfirmationDateTime(previousLeg, snapshotStatus, legType, auditByTrade.get(trade.getLongId()), valDate);

        addEmirMessage(emirMessages, previousLeg, ActionTypeValue.E, LifeCycleEventValue.Error,
            TransactionTypeValue.EXT, ActionValue.CAN, snapshotStatus);

        setConfirmationDateTime(trade, snapshotStatus, legType, auditByTrade.get(trade.getLongId()), valDate);
        addEmirMessage(emirMessages, trade, ActionTypeValue.N, LifeCycleEventValue.Trade, TransactionTypeValue.TRD,
            ActionValue.NEW, snapshotStatus);
      } else {
        // N/Trade/Trade
        setConfirmationDateTime(trade, snapshotStatus, legType, auditByTrade.get(trade.getLongId()), valDate);
        addEmirMessage(emirMessages, trade, ActionTypeValue.N, LifeCycleEventValue.Trade, TransactionTypeValue.TRD,
            ActionValue.NEW, snapshotStatus);
      }
    }
  }

  private void updateUtiTemporalChange(SnapshotStatus snapshotStatus,
                                       LegType legType, int version) {

    if (legType == LegType.FAR && snapshotStatus.isBookedFar()) {
      snapshotStatus.setBookedFarVersion(version);
    } else if (legType != LegType.FAR && snapshotStatus.isBooked()) {
      snapshotStatus.setBookedVersion(version);
    }
  }

  private void processMatching(List<Trade> emirMessages, Trade trade,
                               SnapshotStatus snapshotStatus, LegType legType, int version,
                               Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    if (legType == LegType.FAR) {
      snapshotStatus.setConfirmationDateTimeFar(trade.getUpdatedTime());
    } else {
      snapshotStatus.setConfirmationDateTime(trade.getUpdatedTime());
    }

    if (legType == LegType.FAR && snapshotStatus.isBookedFar()) {
      snapshotStatus.setBookedFarVersion(version);
    } else if (legType != LegType.FAR && snapshotStatus.isBooked()) {
      snapshotStatus.setBookedVersion(version);
    } else {
      setConfirmationDateTime(trade, snapshotStatus, legType,
          auditByTrade.get(trade.getLongId()), valDate);
      // M/Amendment/Trade
      addEmirMessage(emirMessages, trade, ActionTypeValue.M,
          LifeCycleEventValue.Amendment, TransactionTypeValue.TRD,
          ActionValue.NEW, snapshotStatus);
    }
  }

  private void processTradingVenueUpdate(SnapshotStatus snapshotStatus,
                                         Trade trade) {
    final String emirIdentificationIsin = trade
        .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_IDENTIFICATION_ISIN);
    final String emirMicCode = trade
        .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE);
    final String emirProductTotv = trade
        .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_PRODUCT_TOTV);

    snapshotStatus
    .addKeywordsTradingVenue(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_IDENTIFICATION_ISIN,
        emirIdentificationIsin);
    snapshotStatus.addKeywordsTradingVenue(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE,
        emirMicCode);
    snapshotStatus.addKeywordsTradingVenue(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_PRODUCT_TOTV,
        emirProductTotv);
  }

  private void processRateAmendment(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {

    if (isBookedToday(snapshotStatus)) {
      addEmirMessage(emirMessages, trade,
              ActionTypeValue.N, LifeCycleEventValue.Trade,
              TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
    } else  {
        addEmirMessage(emirMessages, trade, ActionTypeValue.M,
            LifeCycleEventValue.Amendment, TransactionTypeValue.TRD,
            ActionValue.NEW, snapshotStatus);
    }
  }

  private void processPortfolioModification(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    if (isAcceptedStatus(trade)
           && !isBookedToday(snapshotStatus)) {
      setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);
      addEmirMessage(emirMessages, trade, ActionTypeValue.R, LifeCycleEventValue.Amendment,
              TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
    }
  }

  private void processPortfolioAssignment(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
      if (trade.getStatus().equals(Status.S_TERMINATED)
                && !isBookedToday(snapshotStatus)) {

        String transferToID = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_TO);
        if (Util.isEmpty(transferToID)) {
            setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);
            addEmirMessage(emirMessages, trade, ActionTypeValue.C,LifeCycleEventValue.Termination, TransactionTypeValue.EXT,
              ActionValue.NEW, snapshotStatus);
        }

    }
  }




  private void  processCancelReissue(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    if (!isBookedToday(snapshotStatus)
            && trade.getStatus().equals(Status.S_CANCELED)) {

      String transferToID = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_MUREX_TRANSFER_TO);
      Trade tradeTransferTo = util.getTradeByMurexTradeId(transferToID, true);
      if(null != tradeTransferTo){
        boolean isLeichange = util.isLeiChange(tradeTransferTo, trade);

        if (isLeichange)   {
          // Baja de la operacion Anterior
          setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);
          addEmirMessage(emirMessages, trade, ActionTypeValue.E, LifeCycleEventValue.Error, TransactionTypeValue.EXT,
                  ActionValue.NEW, snapshotStatus);
        }
      }else{
        Log.warn(this.getClass().getSimpleName(), "Can´t find a trade with ID: " + transferToID);
      }
    }
  }

  private void processRestructured(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    boolean addCancelMsg = true;
    boolean addNewMsg = true;
    if (isBookedToday(snapshotStatus)) {
        if (snapshotStatus.isDelegateReportabilityChanged()) {
          addNewMsg = isFullDelegation(trade);
        }
        if (addNewMsg)   {
            addEmirMessage(emirMessages, trade,
                  ActionTypeValue.N, LifeCycleEventValue.Trade,
                  TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
        }

      } else {

          final Trade previousTrade = trades.get(tradeId).get(version - 1);

           if (snapshotStatus.isDelegateReportabilityChanged()) {
               addCancelMsg = isFullDelegation(previousTrade);
               addNewMsg = isFullDelegation(trade);
           }


            // Checks if the trade is EMIR Reportable
            final boolean tradeIsReportable = snapshotStatus.isReportable();


            boolean isLeichange = util.isLeiChange(previousTrade, trade);
            boolean isCounterpartyChange = util.isChangeInCounterparty(previousTrade, trade);

            if (!isCounterpartyChange) {
              addEmirMessage(emirMessages, trade,
                      ActionTypeValue.M, LifeCycleEventValue.Amendment,
                      TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
            }
            else if (!isLeichange)   {
              addEmirMessage(emirMessages, trade,
                      ActionTypeValue.M, LifeCycleEventValue.Amendment,
                      TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
            }
            else {

              // change in LEI
              setConfirmationDateTime(previousTrade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);

              if (addCancelMsg) {
                addEmirMessage(emirMessages, previousTrade, ActionTypeValue.E, LifeCycleEventValue.Error, TransactionTypeValue.EXT,
                        ActionValue.NEW, snapshotStatus);
              }

              if (addNewMsg) {
                addEmirMessage(emirMessages, trade,
                        ActionTypeValue.N, LifeCycleEventValue.Trade,
                        TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
              }
            }
          }
  }

  private void processCptyAmendment(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
      // E/Error/Exit - N/Trade/Trade

    /*
    final Trade previousTrade = trades.get(tradeId).get(version - 1);
      if (util.isLeiChange(previousTrade, trade)) {
            setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);
            addEmirMessage(emirMessages, trade, ActionTypeValue.C, LifeCycleEventValue.Novation, TransactionTypeValue.EXT,
                    ActionValue.NEW, snapshotStatus);
            return;
      }

  */

  }

  private void processUndoTerm(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);
    addEmirMessage(emirMessages, trade, ActionTypeValue.M, LifeCycleEventValue.Amendment, TransactionTypeValue.TRD,
            ActionValue.NEW, snapshotStatus);
  }

  private void processAmortizationChange(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {
    setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);

    boolean addMsg = true;
    if (snapshotStatus.isDelegateReportabilityChanged()) {
      addMsg = isFullDelegation(trade);
    }

    if (addMsg) {
      addEmirMessage(emirMessages, trade, ActionTypeValue.M, LifeCycleEventValue.Amendment, TransactionTypeValue.TRD,
              ActionValue.NEW, snapshotStatus);
    }
  }

  private void processSharesModification(List<Trade> emirMessages, Trade trade, SnapshotStatus snapshotStatus, Map<Long, Map<Integer, Trade>> trades, long tradeId, int version, Map<Long, List<AuditValue>> auditByTrade, JDate valDate) {

    if (isBookedToday(snapshotStatus)) {
      addEmirMessage(emirMessages, trade,
              ActionTypeValue.N, LifeCycleEventValue.Trade,
              TransactionTypeValue.TRD, ActionValue.NEW, snapshotStatus);
    } else  {
      setConfirmationDateTime(trade, snapshotStatus, LegType.NEAR, auditByTrade.get(trade.getLongId()), valDate);
      addEmirMessage(emirMessages, trade, ActionTypeValue.M, LifeCycleEventValue.Amendment, TransactionTypeValue.TRD,
              ActionValue.NEW, snapshotStatus);

    }

  }

  private boolean hasPreviousUti(Map<Long, Map<Integer, Trade>> trades, LegType legType, long tradeId, int version) {
    String keywordName = EmirSnapshotReduxConstants.TRADE_KEYWORD_UTI_REFERENCE;

    final Trade previousTrade = trades.get(tradeId).get(version - 1);
    final String previousUti = previousTrade.getKeywordValue(keywordName);

    return !Util.isEmpty(previousUti);
  }


  private Trade getTradeLeg(Trade trade, LegType legType) {
    Trade tradeLeg = trade;

    if (legType != LegType.NOT_SWAP) {
      final Product product = trade.getProduct();
      if (product instanceof FXSwap) {
        final FXSwap fxSwap = (FXSwap) product;
        final Vector<Trade> tradeLegs = fxSwap.explodeTrade(trade);
        for (final Trade explodedTradeLeg : tradeLegs) {
          if (util.getLegType(
              explodedTradeLeg) == legType) {
            tradeLeg = explodedTradeLeg;
          }
        }
      }
    }

    return tradeLeg;
  }

  private JDatetime getPreviousConfirmationDatetime(
          List<AuditValue> tradeAudit, JDate date, LegType legType) {
    JDatetime confirmationDatetime = null;
    final JDatetime maxDate = new JDatetime(
        date,
        0,
        0,
        0,
        TimeZone.getTimeZone(EmirSnapshotReduxConstants.MADRID_TIMEZONE));

    for (final AuditValue auditValue : tradeAudit) {
      final JDatetime currentModifDate = auditValue.getModifDate();
      if (currentModifDate.before(maxDate)) {
        boolean updateTime = false;

        final String fieldName = auditValue.getField().getName();
        if (legType == LegType.FAR) {
          if (fieldName.endsWith("#ConfirmationDateTimeFar")) {
            updateTime = true;
          }
        } else {
          if (fieldName.endsWith("#ConfirmationDateTime")) {
            updateTime = true;
          }
        }

        if (updateTime) {
          if (confirmationDatetime == null
              || currentModifDate.after(confirmationDatetime)) {
            confirmationDatetime = currentModifDate;
          }
        }
      }
    }

    return confirmationDatetime;
  }

  private void setConfirmationDateTime(Trade trade,
                                       SnapshotStatus snapshotStatus, LegType legType,
                                       List<AuditValue> tradeAudit, JDate valDate) {
    JDatetime confirmationDateTime = null;
    if (legType == LegType.FAR) {
      confirmationDateTime = snapshotStatus.getConfirmationDateTimeFar();
    } else {
      confirmationDateTime = snapshotStatus.getConfirmationDateTime();
    }

    if (confirmationDateTime == null) {
      confirmationDateTime = getPreviousConfirmationDatetime(tradeAudit,
          valDate, legType);
    }

    if (confirmationDateTime != null) {
      final SimpleDateFormat dateFormat = new SimpleDateFormat(
          EmirSnapshotReduxConstants.UTC_DATE_FORMAT,
          Locale.getDefault());
      dateFormat.setTimeZone(TimeZone
          .getTimeZone(EmirSnapshotReduxConstants.TIMEZONE_UTC));
      final String dateStr = dateFormat.format(confirmationDateTime);
      trade.addKeyword(
          EmirSnapshotReduxConstants.TRADE_KEYWORD_CONFIRMATION_DATE_TIME,
          dateStr);
    }
  }

  private boolean isFullDelegation(final Trade trade) {
    final String value = LegalEntityAttributesCache
        .getInstance()
        .getAttributeValue(
            trade,
            EmirSnapshotReduxConstants.LE_ATTRIBUTE_EMIR_FULL_DELEG,
            true);

    return Boolean.TRUE.toString().equalsIgnoreCase(value);
  }

  private boolean isAcceptedStatus(Trade trade) {
    final Status status = trade.getStatus();
    return Arrays.asList(EmirSnapshotReduxUtil.ACCEPTED_TRADE_STATUSES).contains(status);
  }

  private boolean isTradeMatched(Trade tradeLeg, LegType legType) {
    /*
    String keywordName = KeywordConstantsUtil.TRADE_KEYWORD_MATCHING_STATUS;
    if (legType == LegType.FAR) {
      keywordName = KeywordConstantsUtil.TRADE_KEYWORD_MATCHING_STATUS_FAR;
    }
    */

    final String keywordValue = tradeLeg.getKeywordValue(KeywordConstantsUtil.TRADE_KEYWORD_MATCHING_STATUS);

    return KeywordConstantsUtil.TRADE_KEYWORD_STATUS_MATCHED
        .equals(keywordValue);
  }

  // Check and add UTI temporal
  private boolean hasPreviousUtiTemporal(Map<Long, Map<Integer, Trade>> trades, long tradeId, int version) {

    final Trade previousTrade = trades.get(tradeId).get(version - 1);
    return EmirFieldBuilderUtil.getInstance().hasTemporaryUTI(previousTrade);

  }
  // Check and add UTI temporal - End

  private boolean isBookedToday(final SnapshotStatus snapshotStatus) {
    return snapshotStatus.isBooked() || snapshotStatus.isBookedFar();
  }

  private void processSnapshotStatus(SnapshotStatus snapshotStatus) {
    // If the trade is cancelated in the same date in which was inserted in
    // Calypso through a cancel or cancel & Reissue event, Calypso will not
    // generate any register in the report for this trade.
    if ( (snapshotStatus.isBooked() || snapshotStatus.isBookedFar())
        && (snapshotStatus.isCanceled() || snapshotStatus.isCanceledFar()) ) {
      // Nothing to do
      snapshotStatus.clear();
    }

    //Cpty Amend no se trata como termination.
    if (snapshotStatus.isCounterpartyAmendment())  {
      snapshotStatus.setTerminated(false);
      snapshotStatus.setTerminatedVersion(0);
    }

    if (snapshotStatus.isPortfolioAssignment())  {
          snapshotStatus.setTerminated(false);
          snapshotStatus.setTerminatedVersion(0);
    }

    if (snapshotStatus.isBooked()
            && snapshotStatus.isRestructured())  {
        //snapshotStatus.setTerminated(false);
      snapshotStatus.setBookedVersion(snapshotStatus.getRestructuredVersion());
    }


    // DDR v25
    // If the trade is non EMIR reportable but it was EMIR reportable on ValDate,
    // it should be process ONLY the case of LEI change.
    if (!snapshotStatus.isReportable()) {
      if (snapshotStatus.isLeiChanged()) {
        final int version = snapshotStatus.getLeiChangedVersion();
        snapshotStatus.clear();
        snapshotStatus.setLeiChanged(true);
        snapshotStatus.setLeiChangedVersion(version);
        snapshotStatus.setReportable(false);
      } else if (snapshotStatus.isLeiChangedFar()) {
        final int version = snapshotStatus.getLeiChangedFarVersion();
        snapshotStatus.clear();
        snapshotStatus.setLeiChangedFar(true);
        snapshotStatus.setLeiChangedFarVersion(version);
        snapshotStatus.setReportable(false);
      }
    }
    // DDR v25 - End

  }

  private boolean isTradeMatchedPrev(final int version, final Map<Integer, Trade> tradeWithVersions, final String keywordMatch) {
    final Trade tr = tradeWithVersions.get(version-1);
    if(tr != null) {
      final String kw = tr.getKeywordValue(keywordMatch);
      return KeywordConstantsUtil.TRADE_KEYWORD_STATUS_MATCHED.equals(kw);
    }

    return false;
  }

  public void removeEmirMessageCanceled(List<Trade> emirMessages, final Trade tradeToRemove) {
    int indexToRemove = -1;
    if (tradeToRemove != null) {
      for (int i = 0; i < emirMessages.size() ; i++) {
        final Trade tradeEmir = emirMessages.get(i);
        if (tradeEmir.getLongId() == tradeToRemove.getLongId()) {
          final String action = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION);
          final String actionType = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION_TYPE);
          final String lifecycle = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT);
          final String transtype = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE);
          // E/Error/Exit
          if (tradeEmir.isTradeCanceled() && (ActionTypeValue.E.name().equals(actionType) && LifeCycleEventValue.Error.name().equals(lifecycle)
              && TransactionTypeValue.EXT.name().equals(transtype) && ActionValue.CAN.name().equals(action))) {
            indexToRemove = i;
          }
        }
      }
      if (indexToRemove >= 0 && indexToRemove < emirMessages.size()) {
        emirMessages.remove(indexToRemove);
      }
    }
  }

  public void removeEmirMessageTerminated(List<Trade> emirMessages, final Trade tradeToRemove) {
    int indexToRemove = -1;
    if (tradeToRemove != null) {
      for (int i = 0; i < emirMessages.size() ; i++) {
        final Trade tradeEmir = emirMessages.get(i);
        if (tradeEmir.getLongId() == tradeToRemove.getLongId()) {
          final String action = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION);
          final String actionType = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION_TYPE);
          final String lifecycle = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT);
          final String transtype = tradeEmir.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_TRANSTYPE);
          // C/Novation/Exit
          if (tradeEmir.isTerminated() && (ActionTypeValue.C.name().equals(actionType) && LifeCycleEventValue.Novation.name().equals(lifecycle)
              && TransactionTypeValue.EXT.name().equals(transtype) && ActionValue.NEW.name().equals(action))) {
            indexToRemove = i;
          }
        }
      }

      if (indexToRemove >= 0 && indexToRemove < emirMessages.size()) {
        emirMessages.remove(indexToRemove);
      }
    }
  }

  // DDR v25

  /**
   * Checks if the auditValue is a LEI change.
   *
   * @param auditValue
   * @return
   */
  private boolean isLeiChange(final AuditValue auditValue) {
    boolean leiChange = false;

    final String fieldName = auditValue.getFieldName();
    if (!Util.isEmpty(fieldName)) {
      leiChange = fieldName.endsWith(EmirSnapshotReduxConstants.ENDS_KEYWORD_PREVIOUSLEIVALUE);
    }

    return leiChange;
  }


  /**
   * Update the SnapshotStatus object when a LEI change has happened.
   *
   * @param trade
   * @param legType
   * @param auditValue
   * @param version
   * @param snapshotStatus
   */
  private void updateLeiChange(final Trade trade, final LegType legType, final AuditValue auditValue,
                               final int version, final SnapshotStatus snapshotStatus) {

    // In case of the new LEI is NULL/Empty, we will use the GLCS on
    // TRADEPARTYVAL2/EXCHANGEDCURRPAYVALUE2
    final String newLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade,
        KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
    if (Util.isEmpty(newLeiValue)) {
      final String leCode = trade.getCounterParty().getCode();
      trade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE, leCode);
    }

    // Check if the trade is reportable
    snapshotStatus.setReportable(util.isTradeReportable(trade));

    // On Date D, only show one NEW message generated by LEI Change.
    if (legType == LegType.FAR) {
      snapshotStatus.setLeiChangedFar(true);
      snapshotStatus.setLeiChangedFarVersion(version);
      if (snapshotStatus.isBookedFar()) {
        snapshotStatus.setBookedFarVersion(version);
      }
    } else if (legType != LegType.FAR) {
      snapshotStatus.setLeiChanged(true);
      snapshotStatus.setLeiChangedVersion(version);
      if (snapshotStatus.isBooked()) {
        snapshotStatus.setBookedVersion(version);
      }
    }
  }

  /**
   * Process the emir messages when a LEI change has happened. Adds a CANCEL and/or a NEW message
   * depending on the trade.
   *
   * @param emirMessages
   * @param trade
   * @param snapshotStatus
   */
  private void processLeiChanged(final List<Trade> emirMessages, final Trade trade, final SnapshotStatus snapshotStatus) {

    // On Date D, only show one NEW message with the new LEI Info. Via processBooking
    // On Date D+1, show a CANCEL message and/or a NEW Message.
    if (!isBookedToday(snapshotStatus)) {
      // Previous LEI Value
      final String prevLei = trade.getKeywordValue(KeywordConstantsUtil.TRADE_KEYWORD_PREVIOUSLEIVALUE);

      boolean addCancelMsg = false;
      boolean addNewMsg = false;

      // Checks if the trade is EMIR Reportable
      final boolean tradeIsReportable = snapshotStatus.isReportable();

      if (tradeIsReportable) {
        addNewMsg = true; // NEW Message with info new LEI

        // Check if the LEI change causes a change on a trade EMIR "reportability" from Trade
        // non-Reportable to Trade Reportable.
        if (!util.wasInternal(trade, prevLei)) {
          addCancelMsg = true; // CANCEL Message with the previous LEI or the GLCS name
        }
      } else {
        addCancelMsg = true; // CANCEL Message with the previous LEI or the GLCS name
      }

      // CANCEL Message with the previous LEI or the GLCS name
      if (addCancelMsg) {
        final Trade cancelTrade = util.cloneTrade(trade);
        cancelTrade.addKeyword(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE, prevLei);

        // E/Error/Exit
        addEmirMessage(emirMessages, cancelTrade, ActionTypeValue.E, LifeCycleEventValue.Error,
            TransactionTypeValue.EXT, ActionValue.CAN, snapshotStatus);
      }

      // NEW Message with info new LEI
      if (addNewMsg) {

        // Check if there is a UTI Change in the following versions.
        final boolean addNewMessage = isAddNewMessage(snapshotStatus);
        if (addNewMessage) {
          // N/Trade/Trade
          addEmirMessage(emirMessages, trade, ActionTypeValue.N, LifeCycleEventValue.Trade, TransactionTypeValue.TRD,
              ActionValue.NEW, snapshotStatus);
        }

      }
    }
  }

  /**
   * Compare LeiChanged version with UtiChanged version
   *
   * @param snapshotStatus
   * @return
   */
  private boolean isAddNewMessage(final SnapshotStatus snapshotStatus) {
    return (snapshotStatus.getLeiChangedFarVersion() > snapshotStatus.getUtiChangedFarVersion() || snapshotStatus
        .getLeiChangedVersion() > snapshotStatus.getUtiChangedVersion());
  }
  // DDR v25 - End

}
