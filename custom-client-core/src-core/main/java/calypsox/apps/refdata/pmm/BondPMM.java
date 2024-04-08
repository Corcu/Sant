package calypsox.apps.refdata.pmm;

import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.product.Bond;

public class BondPMM extends SecurityPMMBase implements PMMHandlerInterface {
	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.product.Bond.class;
	}
	
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((Bond)objectToClone).clone();
	}
	
	public boolean additionalProcessing() {
		return updateQuotes();
	}

	public Vector<?> loadElements(final List<String> list) throws CalypsoServiceException {
		return super.loadElements(list, "Bond");
	}
	
	@Override
	public void saveElement(Object product) throws CalypsoServiceException {
		PMMCommon.DS.getRemoteProduct().saveProduct((Bond)product, true);
	}
	
	@Override
	public void deleteElement(Object product) throws CalypsoServiceException {
		PMMCommon.DS.getRemoteProduct().removeProduct(((Bond)product).getId(), true);
	}
	
	@Override
	public String getSecCode(Object product, String identifierName) {
		return ((Bond)product).getSecCode(identifierName);
	}
	
	@Override
	public String getQuoteName(Object product) {
		return ((Bond)product).getQuoteName();
	}
	
	@Override
	public Long getLongId(Object product) {
		return ((Bond)product).getLongId();
	}
	
	@Override
	public void removeOldQuoteNames(Object product) throws CalypsoServiceException {
		PMMCommon.DS.getRemoteProduct().removeBondQuoteNames((Bond)product);
	}
}
