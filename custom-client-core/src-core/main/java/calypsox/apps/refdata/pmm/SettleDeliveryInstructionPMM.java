package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.SettleDeliveryInstruction;

import calypsox.apps.refdata.PhilsUploader;

public class SettleDeliveryInstructionPMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			try {
				PMMCommon.DS.getRemoteReferenceData().save((SettleDeliveryInstruction)entry.getValue());
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

		Vector<SettleDeliveryInstruction> sdis = new Vector<SettleDeliveryInstruction>();
		try {
			List<List<String>> splitCollection = PMMCommon.splitCollection(PMMCommon.ELEMENTS_IDENTIFIERS, 999);
			for (List<String> wantedElementsList : splitCollection) {
				if (Util.isEmpty(wantedElementsList)) {
					continue;
				}
				
				String where = "LE_SETTLE_DELIVERY.SDI_ID IN " + Util.collectionToSQLString(wantedElementsList);
				Vector<SettleDeliveryInstruction> contactsSplit = (Vector<SettleDeliveryInstruction>)PMMCommon.DS.getRemoteReferenceData().getSettleDeliveryInstructions(where, null);
				sdis.addAll(contactsSplit);
			}
		} catch (CalypsoServiceException e) {
			String msg = "Error retrieving elements to delete : " + e.toString();
			PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
			return true;
		}
		
		for (SettleDeliveryInstruction entry : sdis) {
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
		Vector<SettleDeliveryInstruction> vectorReturn = null; 
		String where = "";
		switch (PMMCommon.IDENTIFIER_NAME) {
		case "ID":
			List<Long> longList = PMMCommon.convertList(list, s -> Long.parseLong(s));
			where = " SDI_ID in (" + Util.collectionToString(longList) + ")";
			vectorReturn = PMMCommon.DS.getRemoteReferenceData().getSettleDeliveryInstructions(where, null);
			break;
			
		case "REFERENCE":
			where = " SDI_ID in " + Util.collectionToSQLString(list);
			vectorReturn = PMMCommon.DS.getRemoteReferenceData().getSettleDeliveryInstructions(where, null);
			break;
			
		default:
			vectorReturn = new Vector<SettleDeliveryInstruction>();
			break;
		}
		
		for (SettleDeliveryInstruction currentObject : vectorReturn) {
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
		return com.calypso.tk.refdata.SettleDeliveryInstruction.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((SettleDeliveryInstruction)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
