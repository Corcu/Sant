package calypsox.tk.camel.processor.confirmation;

import calypsox.camel.wf.CalypsoWorkflowHandler;
import calypsox.tk.confirmation.model.jaxb.ConfirmationToCalypsoBean;
import calypsox.tk.confirmation.model.jaxb.Request;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.service.DSConnection;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalypsoConfirmationResponseProcessor implements Processor {

    public static final String MATCH_STATUS_KWD="MatchingStatus";
    public static final String CONF_DATETIME_KWD="ConfirmationDateTime";

    private static final Action TRADE_ACTION=Action.UPDATE;

    /**
     * XmlSchema's not well-formed so a full replacement is needed
     */
    private static final String NAMESPACE=" xmlns=\"http://www.tibco.com/schemas/Scrittura/STPFXII.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.tibco.com/schemas/Scrittura/STPFXII.xsd\"";


    @Override
    public void process(Exchange exchange) throws Exception {
        ConfirmationToCalypsoBean msgBean = unMarshallIncomingMsg(exchange.getIn().getBody(String.class));
        if (msgBean != null) {
            processResponseBean(msgBean);
        }
    }

    /**
     * @param msgBean
     */
    private void processResponseBean(ConfirmationToCalypsoBean msgBean) {
        BOMessage boMessage=this.findMessageFromEventId(msgBean);
        this.updateBOMessage(msgBean,boMessage);
        this.findAndUpdateTrade(msgBean,boMessage);
    }

    /**
     * @param msgBean
     * @return
     */
    private void findAndUpdateTrade(ConfirmationToCalypsoBean msgBean, BOMessage boMessage) {
        String errorMsg = "Couldn't set trade's MATCHING_STATUS keyword";
        try {
            Trade trade = findTradeFromBOMessage(boMessage);
            if (trade != null && TradeWorkflow.isTradeActionApplicable(trade, TRADE_ACTION, DSConnection.getDefault(), null)) {
                updateConfirmedTrade(trade,msgBean);
            }else{
                Log.warn(CalypsoConfirmationResponseProcessor.class.getSimpleName(),errorMsg);
            }
        } catch (CalypsoServiceException exc) {
            Log.error(CalypsoConfirmationResponseProcessor.class.getSimpleName(), errorMsg, exc.getCause());
        }
    }

    private void updateBOMessage(ConfirmationToCalypsoBean msgBean, BOMessage boMessage){
        if (boMessage != null) {
            try {
                updateMsgInfo(boMessage, msgBean);
                DSConnection.getDefault().getRemoteBO().save(boMessage,0,this.getClass().getSimpleName());
            } catch (CalypsoServiceException exc) {
                String errorMsg = "Couldn't set message's MATCHING_STATUS keyword";
                Log.error(CalypsoConfirmationResponseProcessor.class.getSimpleName(), errorMsg, exc.getCause());
            }
        }
    }

    private void updateMsgInfo(BOMessage boMessage, ConfirmationToCalypsoBean msgBean){
        addMsgAttribute(boMessage, MATCH_STATUS_KWD, msgBean.getRequest().getState());
        addMsgAttribute(boMessage,CONF_DATETIME_KWD,msgBean.getRequest().getDateTime());
    }

    private BOMessage findMessageFromEventId(ConfirmationToCalypsoBean msgBean) {
        BOMessage boMessage=null;
        long msgId=Optional.ofNullable(msgBean).map(ConfirmationToCalypsoBean::getRequest)
                .map(Request::getIdEvent).map(Long::valueOf).orElse(0L);
        if(msgId>0) {
            try {
            boMessage = DSConnection.getDefault().getRemoteBO().getMessage(msgId);
            } catch (CalypsoServiceException e) {
                e.printStackTrace();
            }
        }
        return boMessage;
    }

    private static void addKeyword(Trade trade, String keywordName, String keywordValue) {
        if (trade != null) {
            trade.addKeyword(keywordName, keywordValue);
            trade.setAction(TRADE_ACTION);
        }
    }

    private static void addMsgAttribute(BOMessage boMessage, String attrName, String attrValue) {
        if (boMessage != null) {
            boMessage.setAttribute(attrName, attrValue);
            boMessage.setAction(Action.ACK);
        }
    }
    private void updateConfirmedTrade(Trade trade, ConfirmationToCalypsoBean msgBean){
        Hashtable<String,String> newKwds=new Hashtable<>();
        newKwds.put(MATCH_STATUS_KWD, msgBean.getRequest().getState());
        newKwds.put(CONF_DATETIME_KWD,msgBean.getRequest().getDateTime());
        Action tradeAction=TRADE_ACTION;
       if(trade.getProduct() instanceof PerformanceSwap){
            tradeAction=Action.valueOf("CONF_AMEND");
        }
        CalypsoWorkflowHandler.addKeywordsAndSaveTrade(newKwds,trade,tradeAction);
    }

    private Trade findTradeFromBOMessage(BOMessage boMessage) throws CalypsoServiceException{
        long tradeId = Optional.ofNullable(boMessage).map(BOMessage::getTradeLongId)
                        .orElse(0L);
        return DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
    }
    /**
     * @param incomingMsg
     * @return
     */
    private ConfirmationToCalypsoBean unMarshallIncomingMsg(String incomingMsg) {
        JAXBContext jaxbContext;
        ConfirmationToCalypsoBean msgBean = null;
        try {
            jaxbContext = JAXBContext.newInstance(ConfirmationToCalypsoBean.class);
            Unmarshaller jaxbMarshaller = jaxbContext.createUnmarshaller();
            msgBean = (ConfirmationToCalypsoBean) jaxbMarshaller.unmarshal(IOUtils.toInputStream(deleteNamespaceFromSchema(incomingMsg), Charset.forName(StandardCharsets.UTF_8.name())));
        } catch (JAXBException exc) {
            Log.error(this, "Couldn't unmarshal CalypsoConfirmation incoming msg", exc.getCause());
        }
        return msgBean;
    }

    private String deleteNamespaceFromSchema(String xmlBody){
        return Optional.ofNullable(xmlBody).map(xml -> xml.replace(NAMESPACE,"")).orElse("");
    }
}
