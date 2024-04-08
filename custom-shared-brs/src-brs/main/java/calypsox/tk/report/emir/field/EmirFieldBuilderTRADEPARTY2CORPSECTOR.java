
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class EmirFieldBuilderTRADEPARTY2CORPSECTOR implements EmirFieldBuilder {

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

    // Checks
    if (isEmirFinancial(attrEmirCptyClass, financial)) {
      final String emirCorpSector = LegalEntityAttributesCache.getInstance().getAttributeValue(trade,
          KeywordConstantsUtil.LE_ATTRIBUTE_EMIR_CORPORATE_SECTOR, true);
      if (!Util.isEmpty(emirCorpSector)) {
        rst = emirCorpSector;
      }
    } else if (isEmirNonFinancial(attrEmirCptyClass, financial)) {
      String emirCorpSectorNf = LegalEntityAttributesCache.getInstance().getAttributeValue(trade,
          EmirSnapshotReduxConstants.LE_ATTRIBUTE_EMIR_CORPORATE_SECTOR_NF, true);
      if (Util.isEmpty(emirCorpSectorNf)) {
        emirCorpSectorNf = LocalCache.getDomainValueComment(DSConnection.getDefault(),
            EmirSnapshotReduxConstants.DV_LE_ATTRIBUTE_TYPE,
            EmirSnapshotReduxConstants.LE_ATTRIBUTE_EMIR_CORPORATE_SECTOR_NF);
      }

      rst = emirCorpSectorNf;
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
   * Checks if the EmirCptyClass is Financial (FC or SFC). In case of EmirCptyClass is null or
   * empty, check the FINANCIAL.
   *
   * @param attrEmirCptyClass
   * @param financialNature
   * @return
   */
  private boolean isEmirFinancial(final String attrEmirCptyClass, final String financialNature) {
    return (EmirSnapshotReduxConstants.FINANCIAL_COUNTERPARTY.equals(attrEmirCptyClass)
        || EmirSnapshotReduxConstants.SMALL_FINANCIAL_COUNTERPARTY.equals(attrEmirCptyClass) || EmirSnapshotReduxConstants.MAY_TRUE
        .equals(financialNature));
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

}
