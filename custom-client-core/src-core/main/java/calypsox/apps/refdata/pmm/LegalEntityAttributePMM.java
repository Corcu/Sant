package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;

import calypsox.apps.refdata.PhilsUploader;

public class LegalEntityAttributePMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			try {
				PMMCommon.DS.getRemoteReferenceData().save((LegalEntityAttribute)entry.getValue());
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

		Vector<LegalEntityAttribute> leas = new Vector<LegalEntityAttribute>();
		try {
			List<List<String>> splitCollection = PMMCommon.splitCollection(PMMCommon.ELEMENTS_IDENTIFIERS, 999);
			for (List<String> wantedElementsList : splitCollection) {
				if (Util.isEmpty(wantedElementsList)) {
					continue;
				}
				
				String where = "LE_ATTRIBUTE.LE_ATTRIBUTE_ID IN " + Util.collectionToSQLString(wantedElementsList);
				Vector<LegalEntityAttribute> contactsSplit = (Vector<LegalEntityAttribute>)PMMCommon.DS.getRemoteReferenceData().getLegalEntityAttributes(null, where, null);
				leas.addAll(contactsSplit);
			}
		} catch (CalypsoServiceException e) {
			String msg = "Error retrieving elements to delete : " + e.toString();
			PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
			return true;
		}
		
		for (LegalEntityAttribute entry : leas) {
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
		Vector<LegalEntityAttribute> lea = null; 
		String where = "";
		switch (PMMCommon.IDENTIFIER_NAME) {
		case "ID":
			List<Long> longList = PMMCommon.convertList(list, s -> Long.parseLong(s));
			where = " le_attribute_id in (" + Util.collectionToString(longList) + ")";
			lea = PMMCommon.DS.getRemoteReferenceData().getLegalEntityAttributes(null, where, null);
			
			break;
		default:
			lea = new Vector<LegalEntityAttribute>();
			break;
		}
		
		for (LegalEntityAttribute leAtt : lea) {
			String identifier = String.valueOf(leAtt.getLongId());
			PMMCommon.MODIFIABLE_ELEMENTS.put(identifier, leAtt);
		}
		
		return lea;
	}

	@Override
	public void checkLoadedElements(List<String> wantedElements, Vector<?> foundElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.refdata.LegalEntityAttribute.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((LegalEntityAttribute)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
