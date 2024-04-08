package calypsox.tk.camel.processors;

import calypsox.tk.bo.swift.SwiftUtilPublic;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.Document;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

/**
 * @author acd
 *
 * Example Message
 * //1.21;ACK;;004919994511234567546;
 */
public class CPartenonProcessor implements Processor {

    private static final String PARTENON_ID = "PartenonAccountingID";
    private static final String PARTENON_ALIAS = "PartenonAlias";

    @Override
    public void process(Exchange exchange) throws Exception {
        String returnMessage = exchange.getIn().getBody(String.class);
        Log.system("Mic Partenon Response Message: ", returnMessage );
        final Optional<PartenonRMessage> partenonRMessage = Optional.ofNullable(parseMessage(returnMessage));
        partenonRMessage.ifPresent(this::applyActions);
    }

    /**
     * Apply action for trade and message
     * @param message
     */
    private void applyActions(PartenonRMessage message){
        updateMessage(message);
        updateTrade(message);
    }

    /**
     * Apply action on BRS trade
     *
     * @param message
     */
    private void updateTrade(PartenonRMessage message){
        final Long id = message.getId();
        final String status = message.getStatus();
        if("ACK".equalsIgnoreCase(status)){
            try {

                final Optional<Trade> trade = Optional.ofNullable(DSConnection.getDefault().getRemoteTrade().getTrade(id));

                trade.ifPresent(tradeToSave -> {
                    tradeToSave.setAction(Action.valueOf("AMEND_PARTENON"));
                    tradeToSave.addKeyword(PARTENON_ID,message.getPartenonID());
                    tradeToSave.addKeyword(PARTENON_ALIAS,getPartenonAlias(tradeToSave));
                    if(CollateralUtilities.isTradeActionApplicable(tradeToSave,Action.valueOf("AMEND_PARTENON"))){
                        saveTrade(tradeToSave);
                    }
                });
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading trade - " +id+" : "+ e.getCause());
            }
        }
    }

    /**
     * Apply action on last PARTENONMSG on BRS
     * @param message
     */
    private void updateMessage(PartenonRMessage message){
        final Long id = message.getId();
        final String action = message.getStatus();
        if(null!=id && !Util.isEmpty(action)){
            try {
                final Action applyAction = Action.valueOf(action);
                String where = "trade_id = "+id+" AND message_type LIKE 'PARTENONMSG'";
                String orderBy = "message_id DESC";
                final MessageArray messages = DSConnection.getDefault().getRemoteBackOffice().getMessages(null, where, orderBy, null);
                final BOMessage lastBoMessage = messages.get(0);
                lastBoMessage.setAction(applyAction);
                if("NACK".equalsIgnoreCase(action)){
                    lastBoMessage.setAttribute("Mic Response: ",message.getError());
                }
                if(isBOMessageActionApplicable(lastBoMessage,applyAction)){
                    DSConnection.getDefault().getRemoteBackOffice().save(lastBoMessage,0,"");
                }
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading BoMessage: " + e.getCause());
            }
        }
    }

    /**
     * @param message
     * @return
     */
    private PartenonRMessage parseMessage(String message){
        PartenonRMessage partenonMessage = null;
        String delimiter = ";";
        if(!Util.isEmpty(message) && message.contains(delimiter)){
            final String[] split = message.split(delimiter);
            if(split.length>=4){
                partenonMessage = new PartenonRMessage(split[1],split[2]);
                if("ACK".equalsIgnoreCase(partenonMessage.getStatus())){
                    partenonMessage.setPartenonID(split[4]);
                }else{
                    partenonMessage.setError(split[3]);
                }
            }
        }
        return partenonMessage;
    }

    /**
     * @param trade
     */
    private void saveTrade(Trade trade){
        try {
            DSConnection.getDefault().getRemoteTrade().save(trade);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error saving trade: " + trade.getLongId() + " " +e.getCause());
        }
    }

    protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action,DSConnection.getDefault(), null);
    }

    private String getPartenonAlias(Trade trade){
        try {
            MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(trade.getLongId());

            Long adviceDocumentId = Arrays.stream(messages.getMessages())
                .filter(msg -> "PARTENONMSG".equalsIgnoreCase(msg.getMessageType()))
                .filter(msg -> !"CANCELED".equalsIgnoreCase(msg.getStatus().toString()))
                .map(BOMessage::getLongId).findFirst().orElse(0L);


            Vector<Document> documents = DSConnection.getDefault().getRemoteBO().getAdviceDocuments("advice_document.advice_id=" + adviceDocumentId,null,null);
            if(!Util.isEmpty(documents)){
                if(null!=documents.get(0).getDocument()){
                    String trim = documents.get(0).getDocument().toString().trim();
                    String[] split = trim.contains(";") ? trim.split(";") : null;
                    if(null!=split && split.length>=5){
                        String alias = split[5];
                        return alias;
                    }
                }
            }

        } catch (final CalypsoServiceException exc) {
            Log.error("Partenon", "Error loading Partenon MSG: " + exc.getMessage());
            Log.error(SwiftUtilPublic.class, exc); //sonar
        }
        return "";
    }

    private boolean checkMStatus(String staus){
        switch (staus){
            case "ACK":
                return true;
            default:
                return false;
        }
    }

    private class PartenonRMessage {
        Long id;
        String status;
        String error;
        String partenonID;

        public PartenonRMessage(String id, String status) {
            try{
                if (id.contains("_")){
                    this.id = Long.parseLong(id.substring(id.lastIndexOf("_") + 1 , id.length()));
                } else{
                    this.id = Long.parseLong(id);
                }
            }catch (Exception e){
                Log.error(this,"Error parsing tradeID: " + id + " " + e.getCause());
            }
            this.status = status;
        }

        public void setError(String error) {
            this.error = error;
        }

        public void setPartenonID(String partenonID) {
            this.partenonID = partenonID;
        }

        public Long getId() {
            return id;
        }
        public String getStatus() {
            return status;
        }
        public String getError() {
            return error;
        }
        public String getPartenonID() {
            return partenonID;
        }
    }

}
