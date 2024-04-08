package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;

import calypsox.apps.refdata.PhilsUploader;

public class LegalEntityPMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			try {
				PMMCommon.DS.getRemoteReferenceData().save((LegalEntity)entry.getValue());
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

		Vector<LegalEntity> les = new Vector<LegalEntity>();
		try {
			List<List<String>> splitCollection = PMMCommon.splitCollection(PMMCommon.ELEMENTS_IDENTIFIERS, 999);
			for (List<String> wantedElementsList : splitCollection) {
				if (Util.isEmpty(wantedElementsList)) {
					continue;
				}
				
				String where = "LEGAL_ENTITY.LEGAL_ENTITY_ID IN " + Util.collectionToSQLString(wantedElementsList);
				Vector<LegalEntity> contactsSplit = (Vector<LegalEntity>)PMMCommon.DS.getRemoteReferenceData().getAllLE(where, null);
				les.addAll(contactsSplit);
			}
		} catch (CalypsoServiceException e) {
			String msg = "Error retrieving elements to delete : " + e.toString();
			PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
			return true;
		}
		
		for (LegalEntity entry : les) {
			try {
				PMMCommon.DS.getRemoteReferenceData().remove(entry);
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
		List<LegalEntity> les = null; 
		switch (PMMCommon.IDENTIFIER_NAME) {
		case "ID":
			List<Integer> intList = PMMCommon.convertList(list, s -> Integer.parseInt(s));
			les = BOCache.getLegalEntitiesFromLegalEntityIds(PMMCommon.DS, intList, false);
			break;
		case "NAME":
		case "SHORTNAME":
			les = PMMCommon.DS.getRemoteReferenceData().getLegalEntitiesFromLegalEntityNames(new Vector<>(list));
			break;
		default:
			les = new ArrayList<LegalEntity>();
			break;
		}
		
		for (LegalEntity le : les) {
			String identifier = null;
			if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
				identifier = le.getCode();
			}
			else {
				identifier = String.valueOf(le.getLongId());
			}
			PMMCommon.MODIFIABLE_ELEMENTS.put(identifier, le);
		}
		
		return new Vector<>(les);
	}

	@Override
	public void checkLoadedElements(List<String> wantedElements, Vector<?> foundElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.core.LegalEntity.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((LegalEntity)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
