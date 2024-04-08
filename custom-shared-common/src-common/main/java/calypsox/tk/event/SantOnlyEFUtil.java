package calypsox.tk.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

public class SantOnlyEFUtil {
	boolean negate = false;
	Trade trade = null;
	BOTransfer transfer = null;
	BOMessage message = null; 
	String engineName = null;
	PSEvent event = null;
	
	SantOnlyEFUtil(PSEvent paramEvent, boolean paramNegate) {
		negate = paramNegate;
		event = paramEvent;
		engineName = paramEvent.getEngineName();
	}
	
	private StaticDataFilter loadSDF(String filterName){
		return BOCache.getStaticDataFilter(DSConnection.getDefault(), filterName);
	}
	
	private List<String> getEventFiltersSDF() {
        List<String> finalEngineEventFilterSDFs = new ArrayList<String>();
        try {
        	List<String> engineEventFilterSDFs = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterGroupMemberNames(Util.string2Vector("EngineEventFilter"));
            
            if (Util.isEmpty(engineEventFilterSDFs)) {
            	logInfo("No filter found in group EngineEventFilter", "", false);
            	return null;
            }
            logInfo("Found " + engineEventFilterSDFs.size() + " filters", "", true);
            
            String eventFilterPrefix = engineName;
            if (negate) {
            	eventFilterPrefix += "BlockEvents";
            }
            else {
            	eventFilterPrefix += "PassEvents";
            }
            
            logInfo("Searching for filters with prefix " + eventFilterPrefix, "", true);
            for (String currentSDF : engineEventFilterSDFs) {
            	if (currentSDF.startsWith(eventFilterPrefix)) {
            		logInfo("Found filter with correct prefix: " + currentSDF, "", true);
            		finalEngineEventFilterSDFs.add(currentSDF);
            	}
            }
            
            if (Util.isEmpty(engineEventFilterSDFs)) {
            	logInfo("No filter found with prefix " + eventFilterPrefix, "", false);
            	return null;
            }
            logInfo("Found " + finalEngineEventFilterSDFs.size() + " filters with correct prefix.", "", true);
        } catch (CalypsoServiceException e) {
            Log.error("SantOnly", engineName + " - Error loading Filters. " + e);
        }
        
        return finalEngineEventFilterSDFs;
    }
	
	public boolean accept() {
		boolean res = getObjects();
		if (!res) {
			return res;
		}

		List<String> engineEventFilterSDFs = getEventFiltersSDF();
		if (Util.isEmpty(engineEventFilterSDFs)) {
        	return true;
        }
		
		Optional<StaticDataFilter> sdFilterNullable;
		
		for (String currentSDF : engineEventFilterSDFs) {
			sdFilterNullable = Optional.ofNullable(loadSDF(currentSDF));
			
			if (sdFilterNullable.isPresent()) {
				boolean filterAcceptResult = sdFilterNullable.get().accept(trade, transfer, message);
				logInfo("Result of accept() method for SDFilter ", currentSDF, filterAcceptResult);
				
				if (negate) {
					res &= !filterAcceptResult;
				}
				else {
					res &= filterAcceptResult;
				}
				logInfo("Current result of accept() method for EventFilter", "", res);
			}
		}
		
		logInfo("Final result of accept() method for EventFilter", "", res);

		return res;
	}
	
	private boolean getObjects() {
		if (Util.isEmpty(engineName)) {
			return false;
		}
		
		long tradeID = 0;
		long transferID = 0;
		if (event instanceof PSEventTrade) {
			trade = ((PSEventTrade) event).getTrade();
		}
		else if (event instanceof PSEventTransfer) {
			PSEventTransfer eventXFer = (PSEventTransfer) event;

			transfer = eventXFer.getBoTransfer();
			tradeID = transfer.getTradeLongId();
		}
		else if (event instanceof PSEventMessage) {
			PSEventMessage eventMsg = (PSEventMessage) event;

			message = eventMsg.getBoMessage();
			transferID = message.getTransferLongId();
			tradeID = message.getTradeLongId();
		}
		else {
			return true;
		}

		try {
			if (trade == null && tradeID > 0) {
				trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeID);
			}
			if (transfer == null && transferID > 0) {
				transfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(transferID);
			}
		}
		catch (CalypsoServiceException e) {
			Log.error("SantOnly", "Error retrieving Object : " + e.toString());
		}
		
		if (trade == null && transfer == null && message == null) {
			return false;
		}
		
		return true;
	}

	private void logInfo(String msgPrefix, String sdFilter, boolean resToShow) {
		StringBuilder strBld = new StringBuilder(engineName);
		strBld.append(" - ");
		if (negate) {
			strBld.append("Block");
		}
		else {
			strBld.append("Pass");
		}
		strBld.append(" - ");
		strBld.append(msgPrefix);
		strBld.append(sdFilter);
		strBld.append(": ");
		strBld.append(resToShow);
		strBld.append(" - Trade Id: ");
		if (trade != null) {
			strBld.append(trade.getLongId());
		}
		else {
			strBld.append("NULL");
		}
		strBld.append(", Transfer Id: ");
		if (transfer != null) {
			strBld.append(transfer.getLongId());
		}
		else {
			strBld.append("NULL");
		}
		strBld.append(", Message Id: ");
		if (message != null) {
			strBld.append(message.getLongId());
		}
		else {
			strBld.append("NULL");
		}
		Log.info("SantOnly", strBld.toString());
	}
}
