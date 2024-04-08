package calypsox.ctm.rx;

import calypsox.camel.CamelContextSingleton;
import calypsox.ctm.model.IONTradeAck;
import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.publish.jaxb.*;
import com.calypso.tk.service.DSConnection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author aalonsop
 * Sends acks to ION system.
 * Two ACK input sources exists:
 * 1 - GATEWAYMSG Workflow ->
 *      When an ION GATEWAYMSG reaches COMPLETED status, event is received and the ACK is sent. Needed cause MSG reprocessing is done with WF's rules
 * 2 - UploaderEngine (CalypsoAcknowledment) ->
 *      When engine's process ends, an NACK is sent, but only when errors exists (completed ACK is triggered from msg's wf as seen before)
 */
public class RxIONAckAdapter {

    public static boolean sendIONCompletedAck(PSEventMessage eventMessage, String engineName) {
        log(" -> [ION ACK] Starting to process IONTradeAck from PSEventMessageEvent:" + eventMessage.getLongId());
        Optional.of(getIONAckObjectFromBOMessage(eventMessage.getBoMessage()))
                .filter(IONTradeAck::isValidAck)
                .map(RxIONAckAdapter::marshallIONAck)
                .ifPresent(ionAckXml -> CamelContextSingleton.INSTANCE.getCamelContext()
                        .createProducerTemplate()
                        .sendBody(CTMUploaderConstants.ION_ACK_ROUTE_NAME, ionAckXml));
        RxDatauploaderAdapter.markEventAsProcessed(engineName, eventMessage.getLongId());
        return true;
    }

    /**
     * @param ack
     */
    public static void sendIONErrorAck(CalypsoAcknowledgement ack) {
        log(" -> [ION NACK] Starting to process engine's GATEWAYMSG upload result");

        Optional.of(ack)
                .map(RxIONAckAdapter::transformCalypsoAckIntoIONAck)
                .filter(IONTradeAck::isValidAck)
                .map(RxIONAckAdapter::marshallIONAck)
                .ifPresent(ionAckXml -> CamelContextSingleton.INSTANCE.getCamelContext()
                        .createProducerTemplate()
                        .sendBody(CTMUploaderConstants.ION_ACK_ROUTE_NAME, ionAckXml));
    }


    private static IONTradeAck transformCalypsoAckIntoIONAck(CalypsoAcknowledgement calypsoAck) {
        return Optional.ofNullable(calypsoAck.getCalypsoTrades())
                .map(CalypsoTrades::getCalypsoTrade)
                .map(Collection::stream)
                .flatMap(Stream::findFirst)
                .map(RxIONAckAdapter::performObjectMapping)
                .orElse(new IONTradeAck());
    }

    private static IONTradeAck performObjectMapping(CalypsoTrade calypsoTradeAck) {
        IONTradeAck ionTradeAck = new IONTradeAck();
        if(isNotTradeCreated(calypsoTradeAck)){
            ionTradeAck.setId(getIonRefKwdFromCalypsoTrade(calypsoTradeAck));
            ionTradeAck.setTargetId("");
            ionTradeAck.setErrorStr("Block trade not found for given reference");
        }
        return ionTradeAck;
    }

    private static String marshallIONAck(IONTradeAck ionAck) {
        StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(IONTradeAck.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            stringWriter = new StringWriter();
            jaxbMarshaller.marshal(ionAck, stringWriter);
        } catch (JAXBException exc) {
            Log.error(RxIONAckAdapter.class, "Couldn't marshall IONTradeAck.class into xml String", exc.getCause());
        }
        return stringWriter.toString();
    }

    private static String getIonRefKwdFromCalypsoTrade(CalypsoTrade calypsoTradeAck) {
        return Optional.of(calypsoTradeAck)
                .map(CalypsoTrade::getTradeCustomKeywords)
                .map(TradeCustomKeywords::getKeyword)
                .map(List::stream)
                .map(stream -> stream.filter(kwd -> kwd.getName().equals(CTMUploaderConstants.TRADE_KEYWORD_ORIGINAL_EXTERNAL_REF)))
                .flatMap(Stream::findAny)
                .map(RxIONAckAdapter::mapIONKeyword)
                .orElse("");
    }

    public static String mapIONKeyword(Keyword keyword) {
        return Optional.ofNullable(keyword)
                .map(Keyword::getValue)
                .map(value -> value.replace("ION-", ""))
                .orElse("");
    }

    private static boolean isNotTradeCreated(CalypsoTrade calypsoTradeAck) {
        return calypsoTradeAck.getCalypsoTradeId() <= 0L;
    }

    private static IONTradeAck getIONAckObjectFromBOMessage(BOMessage msg) {
        IONTradeAck ack = new IONTradeAck();
        try {
            if (msg != null && msg.getTradeLongId() > 0L) {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(msg.getTradeLongId());
                if (trade != null) {
                    String ionKwd = Optional.ofNullable(trade.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_ORIGINAL_EXTERNAL_REF))
                            .map(str -> str.replace("ION-", "")).orElse("");
                    ack.setId(ionKwd);
                    ack.setTargetId(String.valueOf(trade.getLongId()));
                    ack.setErrorStr("");
                }
            }
        } catch (CalypsoServiceException exc) {
            Log.error(RxIONAckAdapter.class.getSimpleName(), exc.getCause());
        }
        return ack;
    }

    private static void log(String logMsg){
        Log.info("UPLOADER", Thread.currentThread().getName() + logMsg);
    }

}
