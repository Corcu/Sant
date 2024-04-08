package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.ExternalArray;
import com.calypso.tk.core.InvalidClassException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.TradeArray;

import calypsox.apps.refdata.PhilsUploader;

public class TradePMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		Vector<Trade> elements = new Vector<Trade>();
		String action = PMMCommon.DEFAULT_ACTION;
		
		if (!Util.isEmpty(PMMCommon.DEFAULT_OBJECT_ACTION)) {
			action = PMMCommon.DEFAULT_OBJECT_ACTION;
		}
		
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			Trade trade = (Trade)entry.getValue();
			trade.setAction(Action.valueOf(action));
			elements.add((Trade)entry.getValue());
		}
		
		try {
			PMMCommon.DS.getRemoteTrade().saveTrades(new ExternalArray(elements));
		} catch (CalypsoServiceException | InvalidClassException e) {
			String msg = "Error saving Trades : " + e.toString();
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

			String where = "trade.trade_id IN " + Util.collectionToSQLString(wantedElementsList);
			
			try {
				PMMCommon.DS.getRemoteTrade().deleteTrades("", where, PMMCommon.DS.getUser(), null);
			} catch (CalypsoServiceException e) {
				String msg = "Error deleting Trades : " + e.toString();
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
		if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
			PhilsUploader.addTextToTextArea("Trades can only be identified via their ID.", PMMCommon.TYPE_ERROR, 2);
			return null;
		}

		List<Long> longList = PMMCommon.convertList(list, s -> Long.parseLong(s));
		long[] myArray = new long[longList.size()];
		for (int i = 0; i < longList.size(); i++) {
			myArray[i] = longList.get(i);
		}
		TradeArray elements = PMMCommon.DS.getRemoteTrade().getTrades(myArray);

		for (int i = 0; i < elements.size(); i++) {
			Trade element = elements.get(i);
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
		return com.calypso.tk.core.Trade.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((Trade)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
