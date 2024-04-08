package calypsox.tk.util;

import calypsox.tk.bo.JMSQueueMessage;
import com.calypso.infra.util.Util;
import com.calypso.processing.error.CompositeErrorMessage;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MissingDependencyError;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Bond routing ack/nack message Utility class handling the exception message formating
 */
public class BondJMSQueueAnswer extends JMSQueueAnswer {
    /**
     * OK message
     */
    public static final String OK = "OK";
    /**
     * Not OK generic message
     */
    public static final String KO = "KO";

    /**
     * code of the response
     */
    private String code = null;
    /**
     * full description of the response
     */
    private String description = null;

    private String ETTEventType = null;

    // Variables to generate XML String.
    /**
     * document
     */
    private Document document;
    /**
     * transactionConfElmn
     */
    @SuppressWarnings("unused")
    private Element calypsoDocumentResponse;
    private Element calypsoResponse;

    /**
     * Default constructor
     */
    public BondJMSQueueAnswer() {
    }

    /**
     * Used Constructor
     *
     * @param message external message that ask for this response
     */
    public BondJMSQueueAnswer(final ExternalMessage message) {
        this.code = OK;

        if (message instanceof JMSQueueMessage) {
            setReference(((JMSQueueMessage) message).getReference());
            setCorrelationId(((JMSQueueMessage) message).getCorrelationId());
        }
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return generateXML();
    }

    /**
     * Same as toString()
     *
     * @return a string representation of the object.
     */
    @Override
    public String getText() {
        return toString();
    }

    /**
     * Set the code
     *
     * @param code new code
     */
    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Get the code
     * <p>
     * return the code
     */
    @Override
    public String getCode() {
        return this.code;
    }

    /**
     * set the description
     *
     * @param description new description
     */
    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Get the message description
     *
     * @return the message description
     */
    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * Set the exception description and code
     *
     * @param exception new exception
     */
    @Override
    public void setException(final Exception exception) {
        this.description = exception.getMessage();
        if (!Util.isEmpty(this.description) && (exception.getCause() != null)) {

            if (exception.getCause() instanceof CompositeErrorMessage) {
                StringBuffer desc = new StringBuffer("");
                appendErrMessage((CompositeErrorMessage) exception.getCause(), desc);
                this.description += ": " + desc.toString();
                // CompositeErrorMessage cem = (CompositeErrorMessage) exception.getCause();
                // ErrorMessage[] ems = cem.getSubMessages();
                // for (ErrorMessage em : ems) {
                // if (em instanceof CompositeErrorMessage) {
                // ErrorMessage[] subms = ((CompositeErrorMessage) em).getSubMessages();
                // for (ErrorMessage sem : ems) {
                // this.description += ": " + sem.getMessage();
                // }
                // } else {
                // this.description += ": " + em.getMessage();
                // }
                // }

            } else {
                if (exception.getCause().getMessage().contains("Zero length BigInteger")) {
                    this.description += ": Parsing error, Numeric field present in the file but with no value";
                } else {
                    this.description += ": " + exception.getCause().getMessage();
                }

            }
        }
        this.code = getExceptionCode();
    }

    void appendErrMessage(ErrorMessage e, StringBuffer description) {
        if (e instanceof CompositeErrorMessage) {
            CompositeErrorMessage cem = (CompositeErrorMessage) e;
            ErrorMessage[] ems = cem.getSubMessages();
            for (ErrorMessage em : ems) {
                if (em instanceof CompositeErrorMessage) {
                    appendErrMessage(em, description);
                } else if (em instanceof MissingDependencyError) {
                    description.append(((MissingDependencyError) em).getFieldName() + " not present in the system");
                } else {
                    description.append(em.getMessage());
                }
            }

        } else {
            description.append(e.getMessage());

        }
    }

    @Override
    public String getETTEventType() {
        return this.ETTEventType;
    }

    @Override
    public void setETTEventType(final String eTTEventType) {
        this.ETTEventType = eTTEventType;
    }

    /**
     * Get a specific code for an exception
     *
     * @return the specific code
     */
    @Override
    public String getExceptionCode() {
        if (this.description == null) {
            return KO;
        }
        if (this.description.contains("XML document structures must start and end within the same entity")) {
            return "BADFORMED_XML";
        }
        if (this.description.contains("is not valid on Status")) {
            return "INVALID_ACTION";
        }
        if (this.description.contains("Could not translate as a Book")) {
            return "NO_BOOK";
        }
        if (this.description.contains("Could not translate as a LegalEntity")) {
            return "NO_COUNTERPARTY";
        }
        if (this.description.contains("Could not translate as a FXReset ")) {
            return "NO_FX_RESET";
        }
        if (this.description.contains("No Currency pair for name ")) {
            return "NO_FX_CCY_PAIR";
        }
        if (this.description.contains("Legal Entity role:")) {
            return "BAD_COUNTERPARTY_ROLE";
        }
        if (this.description.contains("Could not translate as a CurrencyPair")) {
            return "BAD_CURRENCY_PAIR";
        }
        if (this.description.contains("java.lang.NumberFormatException")) {
            return "BADFORMED_NUMBER";
        }
        if (this.description.contains("java.lang.IllegalArgumentException")) {
            return "BADFORMED_DATE";
        }
        if (this.description.contains("java.util.NoSuchElementException")) {
            return "UNKNOWN_CURRENCY_PAIR";
        }
        return KO;
    }

    /**
     * Generates the XML String with necessary elements.
     *
     * @return XML String generated.
     */
    private String generateXML() {
        final StringWriter strWriter = new StringWriter();
        final org.dom4j.io.OutputFormat format = new org.dom4j.io.OutputFormat();
        format.setEncoding("ISO-8859-1"); // Sets the encoding to format object.
        final XMLWriter output = new XMLWriter(strWriter, format);
        output.setEscapeText(true);

        this.document = DocumentHelper.createDocument();
        // this.calypsoDocumentResponse = this.document.addElement("calypsoDocumentResponse");
        // this.calypsoResponse = this.calypsoDocumentResponse.addElement("calypsoResponse");
        this.calypsoResponse = this.document.addElement("transactionConfirmation");
        generateBondAckNack();
        try {
            output.write(this.document);
            output.setIndentLevel(2);
            output.close();
        } catch (final IOException ioe) {
            Log.error(this, "IOException ocurred: " + ioe);
        }

        return strWriter.getBuffer().toString();
    }

    /**
     * Generates all necessary elements that depend of transactionConfirmation node.
     */
    private void generateBondAckNack() {
        Element correlationElmn = this.calypsoResponse.addElement("userCorrelation");
        final Element codeElmn = this.calypsoResponse.addElement("code");
        final Element descElmn = this.calypsoResponse.addElement("description");

        // Check values of all elements.
        if (null != getCorrelationId()) {
            correlationElmn.setText(getCorrelationId());
        }

        if (null != this.code) {
            codeElmn.setText(this.code);
        }

        if (null != this.description) {
            descElmn.setText(this.description);
        }

        // if (null != this.ETTEventType) {
        // eTTEventTypeElmn.setText(this.ETTEventType);
        // }

    }

}
