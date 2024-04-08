package calypsox.apps.refdata.pmm;

import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;

public class EquityPMM extends SecurityPMMBase implements PMMHandlerInterface {
	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.product.Equity.class;
	}
	
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((Equity)objectToClone).clone();
	}
	
	public boolean additionalProcessing() {
		return updateQuotes();
	}

	public Vector<?> loadElements(final List<String> list) throws CalypsoServiceException {
		return super.loadElements(list, "Equity");
	}
	
	public void saveElement(Object product) throws CalypsoServiceException {
		PMMCommon.DS.getRemoteProduct().saveProduct((Equity)product, true);
	}
	
	@Override
	public void deleteElement(Object product) throws CalypsoServiceException {
		PMMCommon.DS.getRemoteProduct().removeProduct(((Equity)product).getId(), true);
	}
	
	@Override
	public String getSecCode(Object product, String identifierName) {
		return ((Equity)product).getSecCode(identifierName);
	}
	
	@Override
	public String getQuoteName(Object product) {
		return ((Equity)product).getQuoteName();
	}
	
	@Override
	public Long getLongId(Object product) {
		return ((Equity)product).getLongId();
	}
	
	@Override
	public void removeOldQuoteNames(Object product) throws CalypsoServiceException {
		PMMCommon.DS.getRemoteProduct().removeQuoteName(getQuoteName(product));
	}
}
