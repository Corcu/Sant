package calypsox.tk.export.ack;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.VersionedObject;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoProduct;
import com.calypso.tk.publish.jaxb.Error;

import java.util.List;


public class BondDefICACKProcessor extends CalypsoAcknowledgementProcessor<CalypsoProduct>{


    public BondDefICACKProcessor(){
        this.messageType="BOND_IC_EXPORT";
        this.engineName="BondDefICExportServiceEngine";
    }
    public void processAckEvent(CalypsoAcknowledgement calypsoAcknowledgement){
        List<CalypsoProduct> prodList = getAckObjectList(calypsoAcknowledgement);
        if (!Util.isEmpty(prodList) && prodList.get(0) != null){
        	CalypsoProduct ackIndex = prodList.get(0);
            String secCode = ackIndex.getSecCodeValue();
            BOMessage mess = getSourceMsg(secCode, null);
            
            if (mess != null) {
                setMsgAction(mess,getAckObjectStatus(ackIndex), getErrorsFromObject(ackIndex));
                saveMessage(mess);
            }
        }
    }

    @Override
    protected VersionedObject getSourceObject(String objectId) {
        return null;
    }

    @Override
    protected String getSourceObjectId(CalypsoProduct ackObject) {
        return null;
    }

    @Override
    protected List<CalypsoProduct> getAckObjectList(CalypsoAcknowledgement calypsoAcknowledgement) {
    	if (calypsoAcknowledgement.getCalypsoProducts() != null) {
    		return calypsoAcknowledgement.getCalypsoProducts().getCalypsoProduct();
    	}

    	return null;
    }

    @Override
    protected String getAckObjectStatus(CalypsoProduct ackObject) {
        return ackObject.getStatus();
    }

    @Override
    protected List<Error> getErrorsFromObject(CalypsoProduct ackObject) {
        return ackObject.getError();
    }

}
