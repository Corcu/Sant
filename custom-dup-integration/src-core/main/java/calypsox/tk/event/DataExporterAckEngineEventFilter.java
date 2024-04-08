package calypsox.tk.event;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.DataExportServiceEngineEventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class DataExporterAckEngineEventFilter extends DataExportServiceEngineEventFilter {

    private final String allStr="ALL";

    public boolean accept(PSEvent event) {
        boolean isAccepted=false;
        if (event instanceof PSEventDataUploaderAck) {
            isAccepted = acceptAckEvent((PSEventDataUploaderAck) event);
        } else if(event instanceof PSEventMessage){
            if(super.accept(event)){
                isAccepted=acceptMsgProductType((PSEventMessage) event);
            }
        }
        return isAccepted;
    }

    protected boolean acceptMsgProductType(PSEventMessage event){
        String productType=getProductTypeFromClassName();
        String msgProductType= Optional.ofNullable((event).getBoMessage())
                .map(BOMessage::getProductType).orElse("NONE");
        return productType.equals(allStr) || productType.equalsIgnoreCase(msgProductType);
    }

    protected boolean acceptAckEvent(PSEventDataUploaderAck event){
        return true;
    }

    protected String getProductTypeFromClassName(){
        String productType=allStr;
        String[] classNameCrop=this.getClass().getSimpleName().split(DataExporterAckEngineEventFilter.class.getSimpleName());
        if(classNameCrop[0] != null){
            productType=classNameCrop[0];
        }
        return productType;
    }


}
