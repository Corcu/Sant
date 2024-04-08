
package calypsox.tk.report.emir.field;


import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTYTRANSACTIONID1
implements EmirFieldBuilder {
  @Override
  public String getValue(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

     // tradeId not saved until trade stop in any state
     // use allocated_seed to get the future tradeId
      String tradeIdStr = String.valueOf(trade.getLongId());
      if (trade.getLongId() == 0) {
        tradeIdStr = String.valueOf(trade.getAllocatedLongSeed());
      }
      rst = tradeIdStr;


    return rst;
  }
}
