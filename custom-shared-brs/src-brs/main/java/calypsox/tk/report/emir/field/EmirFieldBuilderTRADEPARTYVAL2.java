
package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;


public class EmirFieldBuilderTRADEPARTYVAL2 implements EmirFieldBuilder {
  @Override
  public String getValue(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    String ref2Value = EmirFieldBuilderUtil.getInstance().logicTRADEPARTYPREF2(trade);
    if (EmirSnapshotReduxConstants.LEI.equalsIgnoreCase(ref2Value)) {

      final String actualLeiValue = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);
      final String leiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);
      rst =  (!Util.isEmpty(leiValue)) ? leiValue : actualLeiValue;

    } else if (EmirSnapshotReduxConstants.INTERNAL.equalsIgnoreCase(ref2Value)) {
          rst = trade.getCounterParty().getCode();
    }
    return rst;
  }
}
