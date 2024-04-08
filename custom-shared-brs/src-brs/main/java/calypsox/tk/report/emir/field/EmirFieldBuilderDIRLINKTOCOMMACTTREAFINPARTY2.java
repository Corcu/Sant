
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.tk.util.emir.*;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderDIRLINKTOCOMMACTTREAFINPARTY2
implements EmirFieldBuilder {

  @Override
  public String getValue(Trade trade) {

    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    String financial = EmirSnapshotReduxConstants.EMPTY_SPACE;

    // Get the LegalEntity attribute Emir Cpty Class
    final String attrEmirCptyClass = EmirFieldBuilderUtil.getInstance().getLegalEntityAttribute(trade,
        KeywordConstantsUtil.LE_ATTRIBUTE_EMIR_CPTY_CLASS, true);

    // If EMIR_CPTY_CLASS is empty or null, check the FINANCIAL attribute.
    if (Util.isEmpty(attrEmirCptyClass)) {
      // Checks FINANCIAL
      financial = getFinancial(trade);
    }

    if (isEmirNonFinancial(attrEmirCptyClass, financial)) { // No financial
      final String level = getLogicLevel(trade);
      final String tradeParty2NatRepCpty = getLogicTradeParty2NatReportingCpty(trade);

      if (tradeParty2NatRepCpty.equalsIgnoreCase(EmirSnapshotReduxConstants.LITERAL_N)
          && level.equalsIgnoreCase(EmirSnapshotReduxConstants.LITERAL_T)) {
        final String attrTradingHedging = LegalEntityAttributesCache.getInstance().getAttributeValue(trade,
            KeywordConstantsUtil.LE_ATTRIBUTE_TRADING_HEDGING, true);

        if (EmirSnapshotReduxConstants.TRADING.equalsIgnoreCase(attrTradingHedging)) {
          rst = EmirSnapshotReduxConstants.MAY_FALSE;
        } else if (Util.isEmpty(attrTradingHedging)
            || EmirSnapshotReduxConstants.HEDGING.equalsIgnoreCase(attrTradingHedging)) {
          rst = EmirSnapshotReduxConstants.MAY_TRUE;
        }
      }
    }


    return rst;

  }

  /**
   * Get Emir field value checking the Counterparty FINANCIAL.
   *
   * @param trade
   * @return
   */
  private String getFinancial(final Trade trade) {
    // EMIR_CPTY_CLASS is null/empty, so checks FINANCIAL
    final LegalEntity cpty = trade.getCounterParty();

    if (cpty != null) {
      final boolean isFinancial = cpty.getClassification();
      if (!isFinancial) {
        // No financial
        return EmirSnapshotReduxConstants.MAY_FALSE;
      } else {
        // Financial
        return EmirSnapshotReduxConstants.MAY_TRUE;
      }
    } else {
      Log.info(this, "Trade " + trade.getLongId() + " has not a valid Counterparty.");
      return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
  }

  /**
   * Checks if the EmirCptyClass is Non-Financial (NFC or NFC+). In case of EmirCptyClass is null or
   * empty, check the FINANCIAL.
   *
   * @param attrEmirCptyClass
   * @param financial
   * @return
   */
  private boolean isEmirNonFinancial(final String attrEmirCptyClass, final String financial) {
    return (EmirSnapshotReduxConstants.NON_FINANCIAL_COUNTERPARTY_PLUS.equals(attrEmirCptyClass)
        || EmirSnapshotReduxConstants.NON_FINANCIAL_COUNTERPARTY.equals(attrEmirCptyClass) || EmirSnapshotReduxConstants.MAY_FALSE
        .equals(financial));
  }

  /**
   * Returns the Level.
   *
   * @return Level.
   */
  private String getLogicLevel(Trade trade) {
    String result = EmirSnapshotReduxConstants.EMPTY_SPACE;
    final String actionType = EmirFieldBuilderUtil.getInstance()
        .getLogicActionType(trade);
    if (ActionTypeValue.N.name().equalsIgnoreCase(actionType)
        || ActionTypeValue.R.name().equalsIgnoreCase(actionType)
        || ActionTypeValue.M.name().equalsIgnoreCase(actionType)) {
      result = EmirSnapshotReduxConstants.LITERAL_T;
    }
    return result;
  }

  private String getLogicTradeParty2NatReportingCpty(Trade trade) {
    String result = EmirSnapshotReduxConstants.EMPTY_SPACE;
    final EmirFieldBuilder fieldBuilder = EmirFieldBuilderFactory.getInstance().getFieldBuilder(
        EmirSnapshotColumn.TRADEPARTY2NATREPORTINGCPTY, EmirSnapshotReportType.BOTH);
    if (fieldBuilder != null) {
      result = fieldBuilder.getValue(trade);
    }

    return result;
  }
}
