package calypsox.tk.util;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * The Class SwiftAcordMessageHandler.
 */
public class SwiftAcordMessageHandler implements MessageHandler,
        MessageProducer {

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.MessageHandler#getParsedMessage(java.lang.String)
     */
    @Override
    public BOMessage getParsedMessage(final String serializedMessage) {
        Log.debug(this, "SwiftAcordMessageHandler::entry");
        Log.debug(this, "SwiftAcordMessageHandler::message:\n"
                + serializedMessage);
        BOMessage result = null;
        try {
            final XMLReader reader = XMLReaderFactory.createXMLReader();
            final XMLMessageHandler handler = new AccordSWIFTXMLHandler();
            reader.setContentHandler(handler);
            final InputSource inputSource = new InputSource(new StringReader(
                    serializedMessage));
            reader.parse(inputSource);
            result = handler.getMessage();
        } catch (final SAXException e) {
            Log.error(this,
                    "SwiftAcordMessageHandler::getParsedMessage::SAXException="
                            + e);
        } catch (final IOException e) {
            Log.error(this,
                    "SwiftAcordMessageHandler::getParsedMessage::IOException="
                            + e);
        }
        return result;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.MessageProducer#serializeMessage(com.calypso.tk.bo.BOMessage
     * , java.lang.String, java.lang.String)
     */
    @Override
    public String serializeMessage(final BOMessage message,
                                   final String acknowledgement, final String originalMessage) {
        final TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        final StringWriter writer = new StringWriter();
        final String xslFilePath = "xsl/accordSWIFTResponse.xsl";
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer(new StreamSource(
                    this.getClass().getClassLoader()
                            .getResourceAsStream(xslFilePath)));
            final String xslParam = "acknowledgement";
            transformer.setParameter(xslParam, acknowledgement);
            transformer.setParameter("tradeId", message.getTradeLongId());
            final InputSource inputSource = new InputSource(new StringReader(
                    originalMessage));
            transformer.transform(new SAXSource(inputSource), new StreamResult(
                    writer));
        } catch (final TransformerConfigurationException e) {
            Log.debug(
                    this,
                    "SwiftAcordMessageHandler::getParsedMessage::TransformerConfigurationException="
                            + e);
        } catch (final TransformerException e) {
            Log.debug(this,
                    "SwiftAcordMessageHandler::getParsedMessage::TransformerException="
                            + e);
        }
        return writer.toString();

    }
}
