package calypsox.tk.upload.validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Termination;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.util.UploaderTradeUtil;

public class ValidateCalypsoTradeSecLending extends com.calypso.tk.upload.validator.ValidateCalypsoTradeSecLending {
	
	/**
	 * validate : unique external reference per product.
	 */
	public void validate(CalypsoObject object, Vector<BOException> errors) {
		this.calypsoTrade = (CalypsoTrade) object;

		_trade = ValidatorUtil.getExistingTrade(this.calypsoTrade,isUndoTerminate(), errors);
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
		
		Vector<String> excludeTaskTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "SLRemoveTaskTypes");
		if (!Util.isEmpty(excludeTaskTypes) && errors.size() > 1) {
			for (int i = errors.size() - 1; i >= 0; i--) {
				if (errors.size() == 1) {
					Log.info(this, "Only one error left, leaving it as-is.");
					break;
				}
				BOException currentException = errors.get(i);
				for (String excludeTaskType : excludeTaskTypes) {
					if (currentException.getMessage().contains(excludeTaskType)) {
						Log.info(this, "Removing Task Station Task error because it matches " + excludeTaskType);
						errors.remove(i);
						break;
					}
				}
			}
		}
	}
	
	@Override
    protected boolean isTerminationAmountCorrect(double terminationAmount, double tradeNotionalValue) {
        return true;
     }
	
	public void validateTerminateAction(CalypsoObject object, Trade trade, Vector<BOException> errors, Connection dbConnection, long tradeId) {
		CalypsoTrade calypsoTrade = (CalypsoTrade)object;
		Termination terminationDetails = calypsoTrade.getTermination();
		
		if(terminationDetails!=null && (terminationDetails.getTerminationAmount()==null)) {
			terminationDetails.setTerminationPercent(BigDecimal.valueOf(100));
		}
		super.validateTerminateAction(object, trade, errors, dbConnection, tradeId);
	}
	
	public boolean isUndoTerminate() {
		return calypsoTrade.getAction().equals("UNDO_TERMINATE");
	}


}
