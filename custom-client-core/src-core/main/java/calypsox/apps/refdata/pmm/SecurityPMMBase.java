package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;

import calypsox.apps.refdata.PhilsUploader;

public class SecurityPMMBase {
	public Class<?> getObjectClass() {
		return null;
	}
	
	public String getSecCode(Object product, String identifierName) {
		return null;
	}
	
	public String getQuoteName(Object product) {
		return null;
	}
	
	public Long getLongId(Object product) {
		return null;
	}
	
	public void removeOldQuoteNames(Object product) throws CalypsoServiceException {
		throw new CalypsoServiceException("This method should be overridden");
	}
	
	public void saveElement(Object product) throws CalypsoServiceException {
		throw new CalypsoServiceException("This method should be overridden");
	}
	
	public void deleteElement(Object product) throws CalypsoServiceException {
		throw new CalypsoServiceException("This method should be overridden");
	}
	
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
	
	public void checkLoadedElements(final List<String> wantedElements, final Vector<?> foundElements) {
		Map<String, Integer> foundElementsFreq = new HashMap<String, Integer>();
		for (Object foundElement : foundElements) {
			String identifier = null;
			if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
				identifier = getSecCode(foundElement, PMMCommon.IDENTIFIER_NAME);
			}
			else {
				identifier = String.valueOf(getLongId(foundElement));
			}
			
			wantedElements.remove(identifier);
			Integer previousFreq = foundElementsFreq.get(identifier);
			if (previousFreq == null) {
				previousFreq = 0;
			}
			foundElementsFreq.put(identifier, previousFreq + 1);
		}
		
		List<String> duplicatedElements = new ArrayList<String>();
		for (Map.Entry<String, Integer> entry : foundElementsFreq.entrySet()) {
			if (entry.getValue() > 1) {
				duplicatedElements.add(entry.getKey());
			}
		}
		
		boolean problemFound = false;
		StringBuilder sb = new StringBuilder();
		if (wantedElements.size() > 0) {
			sb.append("  The following elements have not been found: ");
			sb.append(wantedElements.toString());
			problemFound = true;
		}
		if (duplicatedElements.size() > 0) {
			if (problemFound) {
				sb.append("\n");
			}
			sb.append("  The following elements are duplicated: ");
			sb.append(duplicatedElements.toString());
			problemFound = true;
		}
		
		PhilsUploader.addTextToTextArea("Checking loaded elements.", PMMCommon.TYPE_INFO, 2);
		if (problemFound) {
			PhilsUploader.addTextToTextArea(sb.toString() , PMMCommon.TYPE_WARNING, 2);
		}
		else {
			PhilsUploader.addTextToTextArea("Everything is fine.", PMMCommon.TYPE_INFO, 2);
		}	
	}

	public Vector<?> loadElements(final List<String> list, String sqlType) throws CalypsoServiceException {
		String where;
		if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
			where = " product_desc.product_family = '" + sqlType + "' AND product_desc.product_id in (select product_id from product_sec_code where sec_code = '" + PMMCommon.IDENTIFIER_NAME + "' AND CODE_VALUE in "
					+ Util.collectionToSQLString(list) + ")";
		}
		else {
			List<Long> longList = PMMCommon.convertList(list, s -> Long.parseLong(s));
			where = " product_desc.product_id in (" + Util.collectionToString(longList) + ")";
		}
		Vector<Product> products = PMMCommon.DS.getRemoteProduct().getAllProducts(null, where, null);
 
		for (Product product : products) {
			String identifier = null;
			if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
				identifier = product.getSecCode(PMMCommon.IDENTIFIER_NAME);
			}
			else {
				identifier = String.valueOf(product.getLongId());
			}
			PMMCommon.MODIFIABLE_ELEMENTS.put(identifier, product);
		}
		
		return products;
	}
	
	public boolean saveElements() {
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			try {
				saveElement(entry.getValue());
			} catch (CalypsoServiceException e) {
				String msg = "Error saving element : " + e.toString();
				PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
				continue;
			}
			if (count % 100 == 0 && count > 0) {
				PhilsUploader.addTextToTextArea("Saved " + count + "/" + PMMCommon.MODIFIED_ELEMENTS.size() + " elements.", PMMCommon.TYPE_INFO, 2);
			}
			count++;
		}
		
		return false;
	}
	
	public boolean deleteElements() {
		int count = 0;
		for (String wantedElement : PMMCommon.ELEMENTS_IDENTIFIERS) {
			if (Util.isEmpty(wantedElement)) {
				continue;
			}
			
			int id = Integer.valueOf(wantedElement);
			try {
				PMMCommon.DS.getRemoteProduct().removeProduct(id, true);
			} catch (CalypsoServiceException e) {
				String msg = "Error deleting element " + wantedElement + " : " + e.toString();
				PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
				continue;
			}
			
			if (count % 100 == 0 && count > 0) {
				PhilsUploader.addTextToTextArea("Deleted " + count + "/" + PMMCommon.ELEMENTS_IDENTIFIERS.size() + " elements.", PMMCommon.TYPE_INFO, 2);
			}
			count++;
		}
		
		return false;
	}
	
	protected boolean updateQuotes() {
		boolean ret = false;
		PhilsUploader.addTextToTextArea("", "");
		PhilsUploader.addTextToTextArea("*******************************************************", PMMCommon.TYPE_INFO);
		PhilsUploader.addTextToTextArea("Step ++ : Updating Quotes @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		
		boolean quoteModified = false;
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			String identifier = entry.getKey();
			Object newObject = entry.getValue();
			
			Object oldObject = PMMCommon.MODIFIABLE_ELEMENTS.get(identifier);
			if (oldObject == null) {
				PhilsUploader.addTextToTextArea("Could not retrieve original object with identifier " + identifier, PMMCommon.TYPE_ERROR, 2);
				continue;
			}
			
			String oldQuoteName = getQuoteName(oldObject);
			String newQuoteName = getQuoteName(newObject);
			
			if (!newQuoteName.equals(oldQuoteName)) {
				PhilsUploader.addTextToTextArea(identifier + ": Update Quotes, Quote Name has changed.", PMMCommon.TYPE_INFO, 2);
				
				try {
					PMMCommon.DS.getRemoteMarketData().saveQuotesFromName(oldQuoteName, newQuoteName);
				} catch (CalypsoServiceException e) {
					PhilsUploader.addTextToTextArea("Error saving Quotes for " + identifier + ": " + e.toString(), PMMCommon.TYPE_ERROR, 2);
					ret = true;
				}
				try {
					removeOldQuoteNames(oldObject);
				} catch (CalypsoServiceException e) {
					PhilsUploader.addTextToTextArea("Error removing old Quote Names for " + identifier + ": " + e.toString(), PMMCommon.TYPE_ERROR, 2);
					ret = true;
				}
				quoteModified = true;
			}
			
			if (count % 100 == 0 && count > 0) {
				PhilsUploader.addTextToTextArea("Checked " + count + "/" + PMMCommon.MODIFIED_ELEMENTS.size() + " elements for Quotes changes.", PMMCommon.TYPE_INFO, 2);
			}
			count++;
		}
		
		if (!quoteModified) {
			PhilsUploader.addTextToTextArea("No Quote had to be modified.", PMMCommon.TYPE_INFO, 2);
		}
		
		PhilsUploader.addTextToTextArea("Finished updating Quotes @" + PMMCommon.getCurrentDateTime(), PMMCommon.TYPE_INFO);
		
		return ret;
	}
}
