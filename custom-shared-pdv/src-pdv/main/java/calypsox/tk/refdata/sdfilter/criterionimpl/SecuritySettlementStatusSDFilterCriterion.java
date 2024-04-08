package calypsox.tk.refdata.sdfilter.criterionimpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.secfinance.SecurityActualSettlementStatusImpl.SecurityActualSettlementStatusEnum;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;

public class SecuritySettlementStatusSDFilterCriterion extends AbstractSDFilterCriterion<String> {
	  private static final String SEC_SETTLE_STATUS = "SecuritySettlementStatus";

	  public SecuritySettlementStatusSDFilterCriterion() {
	    setName(SEC_SETTLE_STATUS);
	    setCategory(SDFilterCategory.TRADE);
	    setTradeNeeded(true);
	  }

	  @Override
	  public Class<String> getValueType() {
	    return String.class;
	  }

	  /**
	   * A list of possible choices when applicable. Item selected is saved as String value.
	   */
	  @Override
	  public List<String> getDomainValues() {
	    return Stream.of(SecurityActualSettlementStatusEnum.values())
	                               .map(SecurityActualSettlementStatusEnum::getDisplayStatus)
	                               .collect(Collectors.toList());
	  }

	  @Override
	  public List<SDFilterOperatorType> getOperatorTypes() {
	    return Arrays.asList(SDFilterOperatorType.IN, SDFilterOperatorType.NOT_IN);
	  }

	  @Override
	  public boolean hasDomainValues() {
	    return true;
	  }

	  @Override
	  public String getValue(SDFilterInput sdFilterInput) {
		  final Trade trade = sdFilterInput.getTrade();
		  if (trade != null && trade.getProduct() instanceof SecFinance) {
			  JDate valDate = JDate.getNow();
			  String secSettlementSatus = ((SecFinance)trade.getProduct()).getActualSettlementDetails().getSecuritySettlementStatus(trade, valDate);
			  return secSettlementSatus;
		  } else {
			  return null;
		  }
	  }
}
