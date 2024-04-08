package calypsox.tk.upload.validator;

import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.upload.jaxb.Product;
import com.calypso.tk.upload.jaxb.TransferAgent;


public class ValidateCalypsoTradeTransferAgent
		extends com.calypso.tk.upload.validator.ValidateCalypsoTradeTransferAgent {
	
	public void validateProduct(Product product, Vector<BOException> errors, Object connection, long tradeId) {
		TransferAgent transferAgent = product.getTransferAgent();
		if(Util.isEmpty(transferAgent.getFromAgentSDIName()))
				transferAgent.setFromAgentSDIName("FromAgentSDIDummy");
		if(Util.isEmpty(transferAgent.getToAgentSDIName()))
			transferAgent.setToAgentSDIName("ToAgentSDIDummy");
		super.validateProduct(product, errors, connection, tradeId);
		if(transferAgent.getFromAgentSDIName().equals("FromAgentSDIDummy"))
			transferAgent.setFromAgentSDIName(null);
		if(transferAgent.getToAgentSDIName().equals("ToAgentSDIDummy"))
			transferAgent.setToAgentSDIName(null);
	}

}
