package calypsox.tk.refdata.sdfilter.criterionimpl;

import java.util.TimeZone;

import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;

public class XferRuleCompareTDSDSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {
	  private static final String CRIT_NAME = "XferRuleCompareTDSD";

	  public XferRuleCompareTDSDSDFilterCriterion() {
	    setName(CRIT_NAME);
	    setCategory(SDFilterCategory.TRADE_XFER_RULE);
	    setTradeNeeded(true);
	  }
	  
	  public SDFilterCategory getCategory() {
		  return SDFilterCategory.TRADE_XFER_RULE;
	  }

	  @Override
	  public Class<Boolean> getValueType() {
	    return Boolean.class;
	  }

	  @Override
	  public Boolean getValue(SDFilterInput sdFilterInput) {
		  final Trade trade = sdFilterInput.getTrade();
		  TradeTransferRule rule = sdFilterInput.getRule();
		  
		  return rule.getSettleDate().after(trade.getTradeDate().getJDate(TimeZone.getDefault()));
	  }
}
