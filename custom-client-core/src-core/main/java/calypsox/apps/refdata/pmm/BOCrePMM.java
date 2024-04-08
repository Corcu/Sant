package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.CreArray;

import calypsox.apps.refdata.PhilsUploader;

public class BOCrePMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		CreArray elements = new CreArray();
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			elements.add((BOCre)entry.getValue());
		}
		
		try {
			PMMCommon.DS.getRemoteBackOffice().saveCres(elements);
		} catch (CalypsoServiceException e) {
			String msg = "Error saving Cres : " + e.toString();
			PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
		}
		
		return false;
	}
	
	@Override
	public boolean deleteElements() {
		List<List<String>> splitCollection = PMMCommon.splitCollection(PMMCommon.ELEMENTS_IDENTIFIERS, 999);
		int roundCount = 1;
		for (List<String> wantedElementsList : splitCollection) {
			if (Util.isEmpty(wantedElementsList)) {
				continue;
			}

			String where = "bo_cre.bo_cre_id IN " + Util.collectionToSQLString(wantedElementsList);
			
			try {
				PMMCommon.DS.getRemoteBackOffice().deleteCres(where, false, null);
			} catch (CalypsoServiceException e) {
				String msg = "Error deleting Cres : " + e.toString();
				PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("Round ");
			sb.append(roundCount);
			sb.append("/");
			sb.append(splitCollection.size()); 
			PhilsUploader.addTextToTextArea(sb.toString(), PMMCommon.TYPE_INFO, 2);
			
			roundCount++;
		}
		
		return false;
	}

	@Override
	public Vector<?> loadElements(List<String> list) throws CalypsoServiceException {
		String where;
		if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
			PhilsUploader.addTextToTextArea("BO Cres can only be identified via their ID.", PMMCommon.TYPE_ERROR, 2);
			return null;
		}

		List<Long> longList = PMMCommon.convertList(list, s -> Long.parseLong(s));
		where = " bo_cre_id in (" + Util.collectionToString(longList) + ")";

		CreArray elements = PMMCommon.DS.getRemoteBackOffice().getBOCres(null, where, null);

		for (BOCre element : elements) {
			String identifier = null;
			identifier = String.valueOf(element.getId());
			PMMCommon.MODIFIABLE_ELEMENTS.put(identifier, element);
		}

		return elements.toVector();
	}

	@Override
	public void checkLoadedElements(List<String> wantedElements, Vector<?> foundElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.bo.BOCre.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((BOCre)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
