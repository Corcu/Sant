package calypsox.tk.export;

import java.util.Optional;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.refdata.RateIndexDefaults;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoRateIndex;
import com.calypso.tk.upload.jaxb.CalypsoUploadDocument;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.DataUploaderUtil;

public class RateIndexDefaultsUploaderXMLDataExporter implements AbstractUploaderXMLDataExporter {
	@Override
	public String export(Object sourceObject, UploaderXMLDataExporter exporter) {
		RateIndexDefaults rateIndexDefaults = (RateIndexDefaults) sourceObject;
		
        CalypsoObject object = exporter.exportObject(rateIndexDefaults, CalypsoRateIndex.class, "RateIndex");
        if (object != null) {
        	exporter.setCalypsoObject(object);
            forceRateIndexNewAction(object,rateIndexDefaults);

            CalypsoUploadDocument document = new CalypsoUploadDocument();// 524
            GatewayUtil.addCalypsoObject(document, object);// 525
            return DataUploaderUtil.marshallCalypsoObject(document);// 526
        }
		
		return null;
	}
	
	@Override
	public void linkBOMessage(Object sourceObject, BOMessage boMessage) {
		CalypsoRateIndex rateIndex = (CalypsoRateIndex) sourceObject;
        boMessage.setAttribute("ObjectId", rateIndex.getCurrency() + " " + rateIndex.getIndex());
	}
	
	@Override
	public void fillInfo(Object sourceObject, UploaderXMLDataExporter exporter, BOMessage boMessage) {
		
	}
	
    /**
     * If ForceNewExport attribute is set to true, the object will be always exported with NEW action instead of AMEND
     * @param calypsoObject
     * @param rateIndexDefaults
     */
    private void forceRateIndexNewAction(CalypsoObject calypsoObject,RateIndexDefaults rateIndexDefaults){
        if (calypsoObject instanceof CalypsoRateIndex) {
            CalypsoRateIndex rateIndex = (CalypsoRateIndex) calypsoObject;
            boolean forceNewExport=Optional.ofNullable(rateIndexDefaults).map(rid->rid.getAttribute("ForceNewExport"))
                    .map(Boolean::valueOf).orElse(false);
            if (forceNewExport&&Action.S_AMEND.equals(rateIndex.getAction())){
                rateIndex.setAction(Action.S_NEW);
            }
        }
    }
    
    @Override
	public String getIdentifier(Object sourceObject) {
    	if (sourceObject instanceof RateIndexDefaults) {
    		RateIndexDefaults rateIndexDefaults = (RateIndexDefaults) sourceObject;
            return rateIndexDefaults.getAuthName();
    	}
		return "";
	}
}
