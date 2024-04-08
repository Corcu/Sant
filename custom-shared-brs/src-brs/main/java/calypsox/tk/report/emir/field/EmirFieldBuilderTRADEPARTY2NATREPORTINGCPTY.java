
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTY2NATREPORTINGCPTY
implements EmirFieldBuilder {

  @Override
  public String getValue(Trade trade) {

    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    // Get the LegalEntity attribute Emir Cpty Class
    final String attrEmirCptyClass = EmirFieldBuilderUtil.getInstance().getLegalEntityAttribute(trade,
        KeywordConstantsUtil.LE_ATTRIBUTE_EMIR_CPTY_CLASS, true);

    if (Util.isEmpty(attrEmirCptyClass)) {
      // Checks FINANCIAL
      rst = getEmirFieldValueFromFinancial(trade);
    } else {
      rst = getEmirFieldValueFromEmirCptyClassAttr(attrEmirCptyClass);
    }

    return rst;
  }

  /**
   * Get Emir field value checking the Counterparty EMIR_CPTY_CLASS attribute.
   *
   * @param attrEmirCptyClass
   * @return
   */
  private String getEmirFieldValueFromEmirCptyClassAttr(final String attrEmirCptyClass) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    switch (attrEmirCptyClass) {
    case EmirSnapshotReduxConstants.FINANCIAL_COUNTERPARTY:
      rst = EmirSnapshotReduxConstants.LITERAL_F;
      break;
    case EmirSnapshotReduxConstants.SMALL_FINANCIAL_COUNTERPARTY:
      rst = EmirSnapshotReduxConstants.LITERAL_F;
      break;
    case EmirSnapshotReduxConstants.NON_FINANCIAL_COUNTERPARTY_PLUS:
      rst = EmirSnapshotReduxConstants.LITERAL_N;
      break;
    case EmirSnapshotReduxConstants.NON_FINANCIAL_COUNTERPARTY:
      rst = EmirSnapshotReduxConstants.LITERAL_N;
      break;
    default:
      break;
    }

    return rst;
  }

  /**
   * Get Emir field value checking the Counterparty FINANCIAL.
   *
   * @param trade
   * @return
   */
  private String getEmirFieldValueFromFinancial(final Trade trade) {
    // EMIR_CPTY_CLASS is null/empty, so checks FINANCIAL
    final LegalEntity cpty = trade.getCounterParty();

    if (cpty != null) {
      final boolean isFinancial = cpty.getClassification();
      if (!isFinancial) {
        // No financial
        return EmirSnapshotReduxConstants.LITERAL_N;
      } else {
        // Financial
        return EmirSnapshotReduxConstants.LITERAL_F;
      }
    } else {
      Log.info(this, "Trade " + trade.getLongId() + " has not a valid Counterparty.");
      return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
  }

}
