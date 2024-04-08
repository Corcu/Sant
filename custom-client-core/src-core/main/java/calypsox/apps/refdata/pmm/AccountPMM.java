package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.refdata.Account;

import calypsox.apps.refdata.PhilsUploader;

public class AccountPMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			try {
				PMMCommon.DS.getRemoteAccounting().save((Account)entry.getValue());
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
	
	@Override
	public boolean deleteElements() {
		int count = 0;
		
		int[] ids = new int[PMMCommon.ELEMENTS_IDENTIFIERS.size()];
		final Iterator<String> elementsIterator = PMMCommon.ELEMENTS_IDENTIFIERS.iterator();
		int countIds = 0;
		while (elementsIterator.hasNext()) {
			final String currentElementIdentifier = elementsIterator.next();
			ids[countIds] = Integer.parseInt(currentElementIdentifier);
			countIds++;
		}

		Vector<Account> accounts = null;
		try {
			accounts = (Vector<Account>)PMMCommon.DS.getRemoteAccounting().getAccounts(ids);
		} catch (CalypsoServiceException e) {
			String msg = "Error retrieving elements to delete : " + e.toString();
			PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
			return true;
		}
		
		for (Account entry : accounts) {
			try {
				PMMCommon.DS.getRemoteAccounting().remove(entry);
			} catch (CalypsoServiceException e) {
				String msg = "Error deleting element : " + e.toString();
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

	@Override
	public Vector<?> loadElements(List<String> list) throws CalypsoServiceException {
		Vector<Account> vectorReturn = null; 
		switch (PMMCommon.IDENTIFIER_NAME) {
		case "ID":
			List<Integer> intList = PMMCommon.convertList(list, s -> Integer.parseInt(s));
			vectorReturn = new Vector<Account>();
			for (Integer id : intList) {
				vectorReturn.add(BOCache.getAccount(PMMCommon.DS, id));
			}
			break;
			
		default:
			vectorReturn = new Vector<Account>();
			break;
		}
		
		for (Account currentObject : vectorReturn) {
			String identifier = String.valueOf(currentObject.getLongId());
			PMMCommon.MODIFIABLE_ELEMENTS.put(identifier, currentObject);
		}
		
		return vectorReturn;
	}

	@Override
	public void checkLoadedElements(List<String> wantedElements, Vector<?> foundElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.refdata.Account.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((Account)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
