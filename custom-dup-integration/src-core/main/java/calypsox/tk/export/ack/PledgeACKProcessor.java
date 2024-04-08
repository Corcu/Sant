package calypsox.tk.export.ack;

import calypsox.tk.export.ack.model.MxAckToCalypsoAckMapper;
import calypsox.tk.export.ack.model.MxDefaultAckBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.VersionedObject;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoTrade;
import com.calypso.tk.publish.jaxb.Error;
import com.calypso.tk.service.DSConnection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class PledgeACKProcessor extends CalypsoAcknowledgementProcessor<CalypsoTrade>{


    public PledgeACKProcessor(){
        this.engineName="PledgeDataExporterEngine";
    }


    @Override
    public final CalypsoAcknowledgement unmarshallCalypsoAcknowledgement(ExternalMessage message){
        StringReader ackText= Optional.ofNullable(message).map(ExternalMessage::getText)
                .map(StringReader::new).orElse(new StringReader(""));
        CalypsoAcknowledgement ackObj=new CalypsoAcknowledgement();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MxDefaultAckBean.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            MxDefaultAckBean mxAck = (MxDefaultAckBean) jaxbUnmarshaller.unmarshal(ackText);
            ackObj= new MxAckToCalypsoAckMapper().map(mxAck);

        } catch (JAXBException exc) {
            Log.error(this,exc);
        }
        Log.info("UPLOADER", ackText.toString());
        return ackObj;
    }


    @Override
    protected VersionedObject getSourceObject(String objectId) {
       return null;
    }

    /**
     *
     * @param ackObject
     * @return Linked BOMessage Id
     */
    @Override
    protected String getSourceObjectId(CalypsoTrade ackObject) {
        return ackObject.getExternalRef();
    }

    @Override
    protected List<CalypsoTrade> getAckObjectList(CalypsoAcknowledgement calypsoAcknowledgement) {
        return calypsoAcknowledgement.getCalypsoTrades().getCalypsoTrade();
    }

    @Override
    protected String getAckObjectStatus(CalypsoTrade ackObject) {
        return ackObject.getStatus();
    }

    @Override
    protected List<Error> getErrorsFromObject(CalypsoTrade ackObject) {
        return ackObject.getError();
    }


    @Override
    protected BOMessage getSourceMsg(String objectId, String objectVersion){
        BOMessage boMessage=null;
        try{
            long msgId=Long.parseLong(objectId);
            boMessage= DSConnection.getDefault().getRemoteBO().getMessage(msgId);
        }catch(CalypsoServiceException | NumberFormatException exc){
            Log.warn(this,exc.getCause());
        }
        return boMessage;
    }
}
