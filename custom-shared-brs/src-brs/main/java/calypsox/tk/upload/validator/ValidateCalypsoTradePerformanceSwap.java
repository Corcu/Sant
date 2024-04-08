package calypsox.tk.upload.validator;

import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.util.UploaderTradeUtil;

public class ValidateCalypsoTradePerformanceSwap extends com.calypso.tk.upload.validator.ValidateCalypsoTradePerformanceSwap {

	/**
	 * validate : unique external reference per product.
	 */
	public void validate(CalypsoObject object, Vector<BOException> errors) {
		this.calypsoTrade = (CalypsoTrade) object;
		_trade = ValidatorUtil.getExistingTrade(this.calypsoTrade, errors);
		if((calypsoTrade.getTradeId()!=null || _trade!=null) && "NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
			errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00011", UploaderTradeUtil.getID(this.calypsoTrade), _trade!=null?_trade.getLongId():calypsoTrade.getTradeId()));
			return;
		}
		if((calypsoTrade.getTradeId()==null && _trade==null) && !"NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
			String productType = Util.isEmpty(this.calypsoTrade.getExternalReference())?"TradeId":"External Reference";
			errors.add(ErrorExceptionUtils.createException("21002", productType, "00002", UploaderTradeUtil.getID(this.calypsoTrade), 0L));
			return;
		}
		if(_trade!=null)
			calypsoTrade.setTradeId(_trade.getLongId());
		String externalReference=calypsoTrade.getExternalReference();
		calypsoTrade.setExternalReference(null);
		super.validate(object, errors);
		calypsoTrade.setExternalReference(externalReference);
	}
}
