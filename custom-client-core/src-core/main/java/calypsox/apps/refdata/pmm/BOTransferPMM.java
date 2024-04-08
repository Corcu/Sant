package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.ExternalArray;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.TransferArray;

import calypsox.apps.refdata.PhilsUploader;

public class BOTransferPMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		TransferArray elements = new TransferArray();
		String action = PMMCommon.DEFAULT_ACTION;
		
		if (!Util.isEmpty(PMMCommon.DEFAULT_OBJECT_ACTION)) {
			action = PMMCommon.DEFAULT_OBJECT_ACTION;
		}
		
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			BOTransfer transfer = (BOTransfer)entry.getValue();
			transfer.setAction(Action.valueOf(action));
			elements.add(transfer);
		}
		
		try {
			PMMCommon.DS.getRemoteBackOffice().saveTransfers(0L, (String) null, elements, 0L, new ExternalArray());
		} catch (CalypsoServiceException e) {
			String msg = "Error saving Tranfers : " + e.toString();
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

			String where = "bo_transfer.transfer_id IN " + Util.collectionToSQLString(wantedElementsList);
			
			try {
				PMMCommon.DS.getRemoteBackOffice().deleteTransfers(where, false, null);
			} catch (CalypsoServiceException e) {
				String msg = "Error deleting Transfers : " + e.toString();
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
			PhilsUploader.addTextToTextArea("BO Transfers can only be identified via their ID.", PMMCommon.TYPE_ERROR, 2);
			return null;
		}

		List<Long> longList = PMMCommon.convertList(list, s -> Long.parseLong(s));
		where = " transfer_id in (" + Util.collectionToString(longList) + ")";

		TransferArray elements = PMMCommon.DS.getRemoteBackOffice().getTransfers(null, where, null);

		for (BOTransfer element : elements) {
			String identifier = null;
			identifier = String.valueOf(element.getLongId());
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
		return com.calypso.tk.bo.BOTransfer.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((BOTransfer)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
