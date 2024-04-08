package calypsox.tk.export.ack;

import com.calypso.tk.core.VersionedObject;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoRateIndex;
import com.calypso.tk.publish.jaxb.Error;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class RateIndexACKProcessor extends CalypsoAcknowledgementProcessor<CalypsoRateIndex> {


    public RateIndexACKProcessor(){
        this.messageType="RATEINDEX_EXPORT";
        this.engineName="RateIndexExportServiceEngine";
    }

   /* protected
    public void processAckEvent(CalypsoAcknowledgement calypsoAcknowledgement){
        List<CalypsoRateIndex> indexList=calypsoAcknowledgement.getCalypsoRateIndexs().getCalypsoRateIndex();
        if(!Util.isEmpty(indexList)&& indexList.get(0)!=null){
            CalypsoRateIndex ackIndex=indexList.get(0);
            String indexCode=ackIndex.getIndexCurrency()+" "+indexList.get(0).getIndexName();
            String rateIndexVersion= Optional.ofNullable(LocalCache.getRateIndexDefaults(DSConnection.getDefault(),ackIndex.getIndexCurrency(),ackIndex.getIndexName()))
                    .map(RateIndexDefaults::getVersion).map(String::valueOf).orElse("");
            BOMessage mess= getSourceMsg(indexCode,rateIndexVersion);
            if(mess!=null) {
                setMsgAction(mess, ackIndex);
                saveMessage(mess);
            }
        }
    }*/

    @Override
    protected List<Error> getErrorsFromObject(CalypsoRateIndex ackIndex){
        return Optional.ofNullable(ackIndex.getError()).orElse(new ArrayList<>());
    }

    @Override
    protected VersionedObject getSourceObject(String objectId) {
        if(objectId!=null) {
            String[] splittedIds = objectId.split(" ");
            if(splittedIds.length == 2) {
                return LocalCache.getRateIndexDefaults(DSConnection.getDefault(), splittedIds[0],splittedIds[1]);
            }
        }
        return null;
    }

    @Override
    protected String getSourceObjectId(CalypsoRateIndex ackObject) {
        return ackObject.getIndexCurrency()+" "+ackObject.getIndexName();
    }

    @Override
    protected List<CalypsoRateIndex> getAckObjectList(CalypsoAcknowledgement calypsoAcknowledgement) {
        return calypsoAcknowledgement.getCalypsoRateIndexs().getCalypsoRateIndex();
    }

    @Override
    protected String getAckObjectStatus(CalypsoRateIndex ackObject) {
        return ackObject.getStatus();
    }


}
